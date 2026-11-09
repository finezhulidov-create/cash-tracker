package dev.zhulidov.cash_tracker.app.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis-хранилище ИИ-советника: готовые ответы (чтобы не платить за один и тот же анализ дважды) и счётчик
 * запросов к модели на пользователя. Кэш — это оптимизация, поэтому его сбои не ломают запрос.
 * Лимит защищает от расхода платных токенов, поэтому при недоступном Redis запрос отклоняется (fail-closed),
 * в отличие от лимита на /auth, который в таком случае пропускает запросы.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiUsageStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    /** Готовый ответ модели. */
    public record CachedAdvice(String text, Instant generatedAt) {
    }

    public Optional<CachedAdvice> find(String key) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, CachedAdvice.class));
        } catch (RuntimeException | JsonProcessingException e) {
            log.warn("AI advice cache is unavailable, treating as a miss: {}", e.toString());
            return Optional.empty();
        }
    }

    public void save(String key, CachedAdvice advice) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(advice), properties.cacheTtl());
        } catch (RuntimeException | JsonProcessingException e) {
            log.warn("AI advice could not be cached: {}", e.toString());
        }
    }

    /** Списывает один запрос к модели; если лимит исчерпан — {@link AiException} 429 с временем ожидания. */
    public void consume(long userId) {
        String key = quotaKey(userId);
        try {
            Long count = redis.opsForValue().increment(key);
            Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            if (ttl == null || ttl < 0) {
                // первый запрос в окне или ключ остался без срока после сбоя между INCR и EXPIRE
                redis.expire(key, properties.rateLimitWindow());
                ttl = properties.rateLimitWindow().toSeconds();
            }
            if (count != null && count > properties.rateLimitRequests()) {
                throw AiException.rateLimited(Math.max(ttl, 1));
            }
        } catch (AiException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("AI rate limiter is unavailable, request is rejected: {}", e.toString());
            throw AiException.unavailable();
        }
    }

    /** Возвращает запрос, если модель из-за сбоя не ответила: пользователь не виноват, что ничего не получил. */
    public void refund(long userId) {
        try {
            redis.opsForValue().decrement(quotaKey(userId));
        } catch (RuntimeException e) {
            log.warn("AI request quota could not be refunded: {}", e.toString());
        }
    }

    private String quotaKey(long userId) {
        return properties.keyPrefix() + "quota:" + userId;
    }
}
