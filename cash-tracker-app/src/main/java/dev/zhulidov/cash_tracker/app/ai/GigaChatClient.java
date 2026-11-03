package dev.zhulidov.cash_tracker.app.ai;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Тонкий клиент GigaChat: получает и кэширует токен доступа и выполняет запрос chat/completions.
 * Все сбои превращаются в {@link AiException} с понятным кодом; ключ, токен и тексты запросов в лог не пишутся.
 *
 * <p>Токен живёт 30 минут. Он обновляется заранее, а если сервер всё равно ответил 401 (токен отозван
 * или часы разошлись), запрос повторяется один раз с новым токеном.
 */
@Slf4j
public class GigaChatClient {

    private static final String USER_AGENT = "CashTracker/1.0";
    /** Токен обновляется за минуту до истечения. */
    private static final Duration TOKEN_SAFETY_MARGIN = Duration.ofSeconds(60);
    /** Срок жизни токена по документации — 30 минут; держим с запасом. */
    private static final Duration TOKEN_MAX_AGE = Duration.ofMinutes(29);
    private static final Duration TOKEN_FALLBACK_AGE = Duration.ofMinutes(25);
    /** Значения expires_at больше этого порога считаем миллисекундами (в документации единицы описаны противоречиво). */
    private static final long MILLIS_THRESHOLD = 100_000_000_000L;
    private static final long DEFAULT_RETRY_AFTER_SECONDS = 30;

    private final AiProperties properties;
    private final Clock clock;
    private final RestClient oauthClient;
    private final RestClient apiClient;

    private String accessToken;
    private Instant accessTokenValidUntil = Instant.MIN;

    /**
     * @param builder заготовка RestClient с уже настроенными таймаутами и TLS (в тестах — с подменой сервера)
     */
    public GigaChatClient(AiProperties properties, RestClient.Builder builder, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.oauthClient = builder.clone().baseUrl(properties.oauthUrl()).build();
        this.apiClient = builder.clone().baseUrl(properties.apiUrl()).build();
    }

    /** Ответ модели. */
    public record ChatAnswer(String text, boolean truncated, int totalTokens) {
    }

    public ChatAnswer chat(String systemPrompt, String userMessage) {
        try {
            return requestCompletion(accessToken(), systemPrompt, userMessage);
        } catch (HttpClientErrorException.Unauthorized first) {
            log.info("GigaChat answered 401, refreshing the access token and retrying once");
            invalidateToken();
            try {
                return requestCompletion(accessToken(), systemPrompt, userMessage);
            } catch (RestClientException second) {
                throw translate(second);
            }
        } catch (RestClientException e) {
            throw translate(e);
        }
    }

    // ==================== токен ====================

    private synchronized String accessToken() {
        Instant now = clock.instant();
        if (accessToken != null && now.isBefore(accessTokenValidUntil)) {
            return accessToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("scope", properties.scope());
        JsonNode response;
        try {
            response = oauthClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("RqUID", UUID.randomUUID().toString())
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + properties.authKey())
                    .header(HttpHeaders.USER_AGENT, USER_AGENT)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw translate(e);
        }

        String token = response == null ? "" : response.path("access_token").asText("");
        if (token.isBlank()) {
            log.warn("GigaChat token response has no access_token");
            throw AiException.badResponse();
        }
        accessToken = token;
        accessTokenValidUntil = validUntil(now, response.path("expires_at"));
        return token;
    }

    private synchronized void invalidateToken() {
        accessToken = null;
        accessTokenValidUntil = Instant.MIN;
    }

    static Instant validUntil(Instant now, JsonNode expiresAt) {
        if (!expiresAt.isNumber()) {
            return now.plus(TOKEN_FALLBACK_AGE);
        }
        long value = expiresAt.asLong();
        Instant expires = value >= MILLIS_THRESHOLD ? Instant.ofEpochMilli(value) : Instant.ofEpochSecond(value);
        Instant until = expires.minus(TOKEN_SAFETY_MARGIN);
        // Часы сервера могут расходиться с нашими: не ходим за новым токеном на каждый запрос
        Instant floor = now.plus(Duration.ofMinutes(1));
        if (until.isBefore(floor)) {
            until = floor;
        }
        Instant cap = now.plus(TOKEN_MAX_AGE);
        return until.isAfter(cap) ? cap : until;
    }

    // ==================== запрос к модели ====================

    private ChatAnswer requestCompletion(String token, String systemPrompt, String userMessage) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("temperature", properties.temperature());
        body.put("max_tokens", properties.maxTokens());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)));

        JsonNode response = apiClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return parse(response);
    }

    private ChatAnswer parse(JsonNode response) {
        if (response == null) {
            log.warn("GigaChat returned an empty body");
            throw AiException.badResponse();
        }
        String text = response.at("/choices/0/message/content").asText("");
        String finishReason = response.at("/choices/0/finish_reason").asText("");
        int totalTokens = response.at("/usage/total_tokens").asInt(0);

        if (text.isBlank() || "blacklist".equals(finishReason) || "error".equals(finishReason)) {
            log.warn("GigaChat gave no usable answer: finish_reason={}, empty={}", finishReason, text.isBlank());
            throw AiException.badResponse();
        }
        boolean truncated = "length".equals(finishReason);
        if (truncated) {
            log.warn("GigaChat answer was cut at max_tokens={}", properties.maxTokens());
        }
        return new ChatAnswer(text, truncated, totalTokens);
    }

    // ==================== ошибки ====================

    private AiException translate(RestClientException e) {
        if (e instanceof RestClientResponseException response) {
            int status = response.getStatusCode().value();
            switch (status) {
                case 401, 403 -> {
                    log.error("GigaChat rejected the credentials (HTTP {}): check GIGACHAT_AUTH_KEY and the scope", status);
                    return AiException.authFailed();
                }
                case 402 -> {
                    log.error("GigaChat token quota of the model is exhausted (HTTP 402)");
                    return AiException.quotaExhausted();
                }
                case 429 -> {
                    log.warn("GigaChat is busy (HTTP 429): only one concurrent request is allowed for personal accounts");
                    return AiException.busy(retryAfter(response));
                }
                default -> {
                    log.warn("GigaChat answered HTTP {}: {}", status, abbreviate(response.getResponseBodyAsString()));
                    return status >= 500 ? AiException.unavailable() : AiException.badResponse();
                }
            }
        }
        // таймаут, обрыв, ошибка TLS (например, «PKIX path building failed» — нет сертификата Минцифры)
        log.warn("GigaChat is unreachable: {}", rootMessage(e));
        return AiException.unavailable();
    }

    private static long retryAfter(RestClientResponseException response) {
        String header = response.getResponseHeaders() == null ? null : response.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (header != null) {
            try {
                long seconds = Long.parseLong(header.trim());
                if (seconds > 0) {
                    return seconds;
                }
            } catch (NumberFormatException ignored) {
                // дата вместо секунд — используем значение по умолчанию
            }
        }
        return DEFAULT_RETRY_AFTER_SECONDS;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 200 ? oneLine : oneLine.substring(0, 200) + "…";
    }
}
