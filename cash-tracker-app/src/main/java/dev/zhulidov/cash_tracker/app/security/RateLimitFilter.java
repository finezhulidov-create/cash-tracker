package dev.zhulidov.cash_tracker.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

/**
 * Ограничивает число запросов к /auth/** с одного IP: не больше {@code requests} за окно {@code windowSeconds}.
 * Счётчик (фиксированное окно) лежит в Redis, поэтому лимит общий для всех экземпляров приложения и
 * переживает перезапуск. Если Redis недоступен, запросы пропускаются (fail-open): вход не должен
 * ломаться из-за сбоя вспомогательного хранилища.
 *
 * <p>IP берётся из {@code getRemoteAddr()}. За обратным прокси там будет адрес прокси, и лимит станет
 * общим на всех: тогда нужно настроить передачу реального адреса (server.forward-headers-strategy=native).
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String PROTECTED_PREFIX = "/auth/";

    private final StringRedisTemplate redis;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || !request.getRequestURI().startsWith(PROTECTED_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long window = properties.windowSeconds();
        long nowSeconds = clock.instant().getEpochSecond();
        String key = properties.keyPrefix() + request.getRemoteAddr() + ":" + (nowSeconds / window);

        Long count;
        try {
            count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofSeconds(window));
            }
        } catch (RuntimeException e) {
            log.warn("Rate limiter is unavailable, request is let through: {}", e.toString());
            filterChain.doFilter(request, response);
            return;
        }

        if (count != null && count > properties.requests()) {
            response.setHeader("Retry-After", String.valueOf(window - (nowSeconds % window)));
            JsonErrorWriter.write(objectMapper, response, 429, "RATE_LIMIT_EXCEEDED",
                    "Too many requests, try again later");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
