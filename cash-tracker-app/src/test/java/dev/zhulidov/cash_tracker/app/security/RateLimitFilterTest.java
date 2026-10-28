package dev.zhulidov.cash_tracker.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    // 120 с от начала эпохи при окне 60 с: индекс окна 2, до конца окна 60 с
    private static final Instant NOW = Instant.ofEpochSecond(120);

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private FilterChain chain;
    private MockHttpServletResponse response;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        chain = mock(FilterChain.class);
        response = new MockHttpServletResponse();
    }

    private RateLimitFilter filter(boolean enabled, int limit, Instant now) {
        return new RateLimitFilter(redis, new RateLimitProperties(enabled, limit, 60, "rl:"),
                new ObjectMapper().findAndRegisterModules(), Clock.fixed(now, ZoneOffset.UTC));
    }

    private MockHttpServletRequest request(String uri, String ip) {
        var request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    void firstRequestInWindow_passesAndSetsExpirationOfWindowLength() throws Exception {
        when(ops.increment("rl:127.0.0.1:2")).thenReturn(1L);
        var request = request("/auth/login", "127.0.0.1");

        filter(true, 3, NOW).doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(redis).expire("rl:127.0.0.1:2", Duration.ofSeconds(60));
    }

    @Test
    void requestWithinLimit_passesWithoutResettingExpiration() throws Exception {
        when(ops.increment(anyString())).thenReturn(3L);
        var request = request("/auth/login", "127.0.0.1");

        filter(true, 3, NOW).doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(redis, never()).expire(anyString(), any(Duration.class));
        assertEquals(200, response.getStatus());
    }

    @Test
    void requestOverLimit_isRejectedWith429RetryAfterAndJsonBody() throws Exception {
        when(ops.increment(anyString())).thenReturn(4L);
        var request = request("/auth/login", "127.0.0.1");

        filter(true, 3, NOW).doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("\"errorCode\":\"RATE_LIMIT_EXCEEDED\""));
        assertTrue(response.getContentAsString().contains("\"status\":429"));
    }

    @Test
    void retryAfter_isTimeLeftUntilTheEndOfCurrentWindow() throws Exception {
        when(ops.increment(anyString())).thenReturn(99L);
        var request = request("/auth/register", "127.0.0.1");

        // 150 с: вторая половина окна [120, 180), до конца осталось 30 с
        filter(true, 3, Instant.ofEpochSecond(150)).doFilter(request, response, chain);

        assertEquals("30", response.getHeader("Retry-After"));
    }

    @Test
    void counterKey_dependsOnClientIpAndWindow() throws Exception {
        when(ops.increment(anyString())).thenReturn(1L);

        filter(true, 3, NOW).doFilter(request("/auth/login", "10.0.0.5"), response, chain);
        filter(true, 3, Instant.ofEpochSecond(180)).doFilter(request("/auth/login", "10.0.0.5"), new MockHttpServletResponse(), chain);

        verify(ops).increment("rl:10.0.0.5:2");
        verify(ops).increment("rl:10.0.0.5:3");
    }

    @Test
    void requestsOutsideAuthPrefix_areNotCounted() throws Exception {
        var request = request("/v1/users/me", "127.0.0.1");

        filter(true, 1, NOW).doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redis);
    }

    @Test
    void whenDisabled_authRequestsAreNotCounted() throws Exception {
        var request = request("/auth/login", "127.0.0.1");

        filter(false, 1, NOW).doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(redis);
    }

    @Test
    void whenRedisFails_requestIsLetThrough() throws Exception {
        when(ops.increment(anyString())).thenThrow(new IllegalStateException("redis is down"));
        var request = request("/auth/login", "127.0.0.1");

        filter(true, 3, NOW).doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void whenCounterIsUnavailable_requestIsLetThrough() throws Exception {
        when(ops.increment(anyString())).thenReturn(null);
        var request = request("/auth/login", "127.0.0.1");

        filter(true, 3, NOW).doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
