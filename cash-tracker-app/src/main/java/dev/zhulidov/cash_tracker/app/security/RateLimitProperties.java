package dev.zhulidov.cash_tracker.app.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Ограничение частоты запросов к /auth/** (защита от перебора паролей и спама регистраций).
 *
 * @param enabled       выключатель
 * @param requests      сколько запросов с одного IP допускается за окно
 * @param windowSeconds длина окна в секундах
 * @param keyPrefix     префикс ключей в Redis (в тестах делается уникальным, чтобы счётчики не пересекались)
 */
@ConfigurationProperties(prefix = "app.rate-limit.auth")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("30") int requests,
        @DefaultValue("60") int windowSeconds,
        @DefaultValue("ratelimit:auth:") String keyPrefix
) {
}
