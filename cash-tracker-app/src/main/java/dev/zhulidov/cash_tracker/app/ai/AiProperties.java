package dev.zhulidov.cash_tracker.app.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Настройки ИИ-советника (GigaChat). По умолчанию функция выключена, и приложение работает без ключа.
 * Если она включена, но ключа нет, приложение не запустится: ошибка видна сразу, а не при первом запросе пользователя.
 *
 * @param enabled             включить ИИ-советник (GIGACHAT_ENABLED)
 * @param authKey             «ключ авторизации» из личного кабинета GigaChat, base64 от client_id:client_secret (GIGACHAT_AUTH_KEY)
 * @param scope               GIGACHAT_API_PERS для физлиц, GIGACHAT_API_B2B или GIGACHAT_API_CORP для организаций
 * @param oauthUrl            адрес получения токена
 * @param apiUrl              базовый адрес API без завершающего слеша, например {@code https://api.giga.chat/v1}
 * @param model               название модели, например GigaChat-2-Pro
 * @param temperature         «творческость» ответа, больше нуля; для анализа цифр нужна низкая
 * @param maxTokens           предел длины ответа (ограничивает и стоимость запроса)
 * @param connectTimeout      сколько ждать соединения
 * @param readTimeout         сколько ждать ответа модели
 * @param queueTimeout        сколько запрос может стоять в очереди: у физлица доступен один поток, остальные ждут
 * @param cacheTtl            сколько хранить готовый ответ для тех же данных
 * @param rateLimitRequests   сколько запросов к модели можно сделать одному пользователю за окно
 * @param rateLimitWindow     длина окна лимита
 * @param caCertPath          пути (через запятую) к PEM-файлам корневого и выпускающего сертификатов Минцифры (GIGACHAT_CA_CERT_PATH);
 *                            нужны, если Java не доверяет сертификатам серверов GigaChat
 * @param keyPrefix           префикс ключей в Redis (в тестах делается уникальным)
 */
@ConfigurationProperties(prefix = "gigachat")
public record AiProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String authKey,
        @DefaultValue("GIGACHAT_API_PERS") String scope,
        @DefaultValue("https://ngw.devices.sberbank.ru:9443/api/v2/oauth") String oauthUrl,
        @DefaultValue("https://api.giga.chat/v1") String apiUrl,
        @DefaultValue("GigaChat-2-Pro") String model,
        @DefaultValue("0.3") double temperature,
        @DefaultValue("1500") int maxTokens,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("90s") Duration readTimeout,
        @DefaultValue("30s") Duration queueTimeout,
        @DefaultValue("6h") Duration cacheTtl,
        @DefaultValue("10") int rateLimitRequests,
        @DefaultValue("1h") Duration rateLimitWindow,
        @DefaultValue("") String caCertPath,
        @DefaultValue("ai:") String keyPrefix
) {

    public AiProperties {
        authKey = normalizeKey(authKey);
        apiUrl = stripTrailingSlash(apiUrl);
        if (enabled) {
            require(!authKey.isBlank(), "gigachat.enabled=true, but gigachat.auth-key (GIGACHAT_AUTH_KEY) is empty");
            require(temperature > 0, "gigachat.temperature must be greater than 0");
            require(maxTokens > 0, "gigachat.max-tokens must be greater than 0");
            require(isPositive(connectTimeout) && isPositive(readTimeout) && isPositive(queueTimeout),
                    "gigachat timeouts must be positive");
            require(isPositive(cacheTtl) && isPositive(rateLimitWindow), "gigachat.cache-ttl and gigachat.rate-limit-window must be positive");
            require(rateLimitRequests > 0, "gigachat.rate-limit-requests must be greater than 0");
        }
    }

    /** Файлы сертификатов, которым нужно доверять дополнительно к стандартным. */
    public List<Path> caCertPaths() {
        return Arrays.stream(caCertPath.split(","))
                .map(String::trim)
                .filter(path -> !path.isEmpty())
                .map(Path::of)
                .toList();
    }

    /** Ключ можно вставить вместе с префиксом «Basic », лишние пробелы по краям не страшны. */
    private static String normalizeKey(String key) {
        String trimmed = key == null ? "" : key.trim();
        return trimmed.regionMatches(true, 0, "Basic ", 0, 6) ? trimmed.substring(6).trim() : trimmed;
    }

    private static String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static boolean isPositive(Duration duration) {
        return duration != null && !duration.isNegative() && !duration.isZero();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
