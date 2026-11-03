package dev.zhulidov.cash_tracker.app.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GigaChatClientTest {

    private static final String OAUTH = "https://oauth.test/api/v2/oauth";
    private static final String CHAT = "https://api.test/v1/chat/completions";
    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private final AiTestData.MutableClock clock = new AiTestData.MutableClock(AiTestData.NOW);
    private MockRestServiceServer server;
    private GigaChatClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GigaChatClient(AiTestData.props().build(), builder, clock);
    }

    // ---------- заготовки ответов ----------

    private ResponseActions expectToken(String token) {
        return server.expect(once(), requestTo(OAUTH)).andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic test-key"));
    }

    /** Токен, выданный в момент NOW: живёт 30 минут. */
    private void tokenIssued(String token) {
        tokenIssuedAt(token, AiTestData.NOW);
    }

    /** Все ожидания приходится задавать заранее: MockRestServiceServer не принимает новые после первого запроса. */
    private void tokenIssuedAt(String token, Instant issuedAt) {
        long expiresAtMillis = issuedAt.plus(Duration.ofMinutes(30)).toEpochMilli();
        expectToken(token).andRespond(withSuccess(
                "{\"access_token\":\"" + token + "\",\"expires_at\":" + expiresAtMillis + "}", MediaType.APPLICATION_JSON));
    }

    private ResponseActions expectChat(String token) {
        return server.expect(once(), requestTo(CHAT)).andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private static String completion(String text, String finishReason) {
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":" + quote(text) + "},\"index\":0,"
                + "\"finish_reason\":\"" + finishReason + "\"}],\"usage\":{\"total_tokens\":321}}";
    }

    private static String quote(String text) {
        try {
            return new ObjectMapper().writeValueAsString(text);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private AiException chatFailure() {
        return assertThrows(AiException.class, () -> client.chat("system", "user"));
    }

    // ---------- нормальная работа ----------

    @Test
    void getsATokenThenAsksTheModelWithSystemAndUserMessages() {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("scope", "GIGACHAT_API_PERS");
        server.expect(once(), requestTo(OAUTH)).andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic test-key"))
                .andExpect(header("RqUID", matchesPattern(UUID_PATTERN)))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(content().formData(form))
                .andRespond(withSuccess("{\"access_token\":\"tok-1\",\"expires_at\":"
                        + AiTestData.NOW.plus(Duration.ofMinutes(30)).toEpochMilli() + "}", MediaType.APPLICATION_JSON));
        expectChat("tok-1")
                .andExpect(header(HttpHeaders.USER_AGENT, "CashTracker/1.0"))
                .andExpect(jsonPath("$.model").value("GigaChat-2-Pro"))
                .andExpect(jsonPath("$.temperature").value(0.3))
                .andExpect(jsonPath("$.max_tokens").value(1500))
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("system prompt"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("user data"))
                .andRespond(withSuccess(completion("Привет", "stop"), MediaType.APPLICATION_JSON));

        GigaChatClient.ChatAnswer answer = client.chat("system prompt", "user data");

        assertEquals("Привет", answer.text());
        assertEquals(321, answer.totalTokens());
        assertFalse(answer.truncated());
        server.verify();
    }

    @Test
    void reusesTheTokenWhileItIsValid() {
        tokenIssued("tok-1");
        expectChat("tok-1").andRespond(withSuccess(completion("one", "stop"), MediaType.APPLICATION_JSON));
        expectChat("tok-1").andRespond(withSuccess(completion("two", "stop"), MediaType.APPLICATION_JSON));

        assertEquals("one", client.chat("s", "u").text());
        clock.advance(Duration.ofMinutes(20));
        assertEquals("two", client.chat("s", "u").text());

        server.verify();
    }

    @Test
    void asksForANewTokenWhenTheOldOneIsAboutToExpire() {
        tokenIssued("tok-1");
        expectChat("tok-1").andRespond(withSuccess(completion("one", "stop"), MediaType.APPLICATION_JSON));
        tokenIssuedAt("tok-2", AiTestData.NOW.plus(Duration.ofMinutes(29)));
        expectChat("tok-2").andRespond(withSuccess(completion("two", "stop"), MediaType.APPLICATION_JSON));

        client.chat("s", "u");
        clock.advance(Duration.ofMinutes(29));
        client.chat("s", "u");

        server.verify();
    }

    @Test
    void anAnswerCutByTheTokenLimitIsFlagged() {
        tokenIssued("tok-1");
        expectChat("tok-1").andRespond(withSuccess(completion("обрыв", "length"), MediaType.APPLICATION_JSON));

        assertTrue(client.chat("s", "u").truncated());
    }

    // ---------- срок жизни токена ----------

    @ParameterizedTest
    @CsvSource({
            // expires_at, ожидаемая граница действия токена относительно now (в секундах)
            "SECONDS_30MIN, 1740",   // через 30 минут минус минута запаса, но не больше 29 минут
            "SECONDS_10MIN, 540",    // через 10 минут минус минута
            "MILLIS_10MIN,  540",    // то же значение в миллисекундах
            "SECONDS_PAST,   60",    // уже в прошлом (часы расходятся): не дольше минуты, но и не на каждый запрос
            "ABSENT,       1500"     // поля нет: 25 минут
    })
    void tokenLifetimeUnderstandsSecondsMillisecondsAndMissingValues(String kind, long expectedSeconds) throws Exception {
        Instant now = AiTestData.NOW;
        String json = switch (kind) {
            case "SECONDS_30MIN" -> "{\"expires_at\":" + now.plus(Duration.ofMinutes(30)).getEpochSecond() + "}";
            case "SECONDS_10MIN" -> "{\"expires_at\":" + now.plus(Duration.ofMinutes(10)).getEpochSecond() + "}";
            case "MILLIS_10MIN" -> "{\"expires_at\":" + now.plus(Duration.ofMinutes(10)).toEpochMilli() + "}";
            case "SECONDS_PAST" -> "{\"expires_at\":" + now.minus(Duration.ofMinutes(5)).getEpochSecond() + "}";
            default -> "{}";
        };

        Instant until = GigaChatClient.validUntil(now, new ObjectMapper().readTree(json).path("expires_at"));

        assertEquals(expectedSeconds, Duration.between(now, until).getSeconds());
    }

    // ---------- 401: токен отозван ----------

    @Test
    void refreshesTheTokenAndRetriesOnceOn401() {
        tokenIssued("tok-1");
        expectChat("tok-1").andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"status\":401}").contentType(MediaType.APPLICATION_JSON));
        tokenIssued("tok-2");
        expectChat("tok-2").andRespond(withSuccess(completion("после обновления", "stop"), MediaType.APPLICATION_JSON));

        assertEquals("после обновления", client.chat("s", "u").text());
        server.verify();
    }

    @Test
    void givesUpWhenTheFreshTokenIsRejectedToo() {
        tokenIssued("tok-1");
        expectChat("tok-1").andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        tokenIssued("tok-2");
        expectChat("tok-2").andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        AiException error = chatFailure();

        assertEquals(AiException.AUTH_FAILED, error.getErrorCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
        server.verify();
    }

    @Test
    void wrongKeyOnTheTokenEndpointIsAnAuthFailure() {
        expectToken("x").andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"code\":6}").contentType(MediaType.APPLICATION_JSON));

        assertEquals(AiException.AUTH_FAILED, chatFailure().getErrorCode());
    }

    @Test
    void aTokenResponseWithoutATokenIsABadResponse() {
        expectToken("x").andRespond(withSuccess("{\"expires_at\":1}", MediaType.APPLICATION_JSON));

        assertEquals(AiException.BAD_RESPONSE, chatFailure().getErrorCode());
    }

    // ---------- коды ошибок модели ----------

    @ParameterizedTest
    @CsvSource({
            "402, 503, AI_QUOTA_EXHAUSTED",
            "403, 503, AI_AUTH_FAILED",
            "413, 502, AI_BAD_RESPONSE",
            "422, 502, AI_BAD_RESPONSE",
            "400, 502, AI_BAD_RESPONSE",
            "500, 503, AI_UNAVAILABLE",
            "503, 503, AI_UNAVAILABLE"
    })
    void mapsModelErrorsToClearCodes(int upstream, int ours, String code) {
        tokenIssued("tok-1");
        expectChat("tok-1").andRespond(withStatus(HttpStatus.valueOf(upstream)).body("{\"message\":\"details\"}")
                .contentType(MediaType.APPLICATION_JSON));

        AiException error = chatFailure();

        assertEquals(code, error.getErrorCode());
        assertEquals(ours, error.getStatus().value());
    }

    @Test
    void tooManyRequestsBecomesBusyWithTheRetryAfterOfTheService() {
        tokenIssued("tok-1");
        expectChat("tok-1").andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "12"));

        AiException error = chatFailure();

        assertEquals(AiException.BUSY, error.getErrorCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getStatus());
        assertEquals(12L, error.getRetryAfterSeconds());
    }

    @Test
    void tooManyRequestsWithoutAHeaderUsesADefaultWait() {
        tokenIssued("tok-1");
        expectChat("tok-1").andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertEquals(30L, chatFailure().getRetryAfterSeconds());
    }

    @Test
    void aNetworkFailureOrTimeoutIsUnavailable() {
        tokenIssued("tok-1");
        expectChat("tok-1").andRespond(request -> {
            throw new IOException("Read timed out");
        });

        AiException error = chatFailure();

        assertEquals(AiException.UNAVAILABLE, error.getErrorCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
    }

    @Test
    void aTlsFailureOnTheTokenEndpointIsUnavailableToo() {
        expectToken("x").andRespond(request -> {
            throw new IOException("PKIX path building failed");
        });

        assertEquals(AiException.UNAVAILABLE, chatFailure().getErrorCode());
    }

    // ---------- непригодные ответы ----------

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"choices\":[]}",
            "{}",
            "{\"choices\":[{\"message\":{\"content\":\"\"},\"finish_reason\":\"stop\"}]}",
            "{\"choices\":[{\"message\":{\"content\":\"   \"},\"finish_reason\":\"stop\"}]}",
            "{\"choices\":[{\"message\":{\"content\":\"text\"},\"finish_reason\":\"blacklist\"}]}",
            "{\"choices\":[{\"message\":{\"content\":\"text\"},\"finish_reason\":\"error\"}]}"
    })
    void anEmptyOrRefusedAnswerIsABadResponseNotASilent200(String body) {
        tokenIssued("tok-1");
        expectChat("tok-1").andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertEquals(AiException.BAD_RESPONSE, chatFailure().getErrorCode());
    }

    // ---------- безопасность ----------

    @Test
    void errorsNeverExposeTheKeyTheTokenOrUpstreamDetails() {
        tokenIssued("tok-secret");
        expectChat("tok-secret").andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"message\":\"internal host db-7.sber.local exploded\"}").contentType(MediaType.APPLICATION_JSON));

        AiException error = chatFailure();

        assertFalse(error.getMessage().contains("test-key"));
        assertFalse(error.getMessage().contains("tok-secret"));
        assertFalse(error.getMessage().contains("sber.local"));
    }
}
