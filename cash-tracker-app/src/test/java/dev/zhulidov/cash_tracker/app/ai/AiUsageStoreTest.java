package dev.zhulidov.cash_tracker.app.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiUsageStoreTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> values;

    /** Как в приложении: Spring Boot пишет даты строками ISO-8601, а не числами. */
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private AiUsageStore store;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        store = new AiUsageStore(redis, mapper, AiTestData.props().rateLimitRequests(3).build());
    }

    // ---------- кэш ----------

    @Test
    void savesAnswersAsJsonWithTheConfiguredLifetime() {
        store.save("k", new AiUsageStore.CachedAdvice("Текст", AiTestData.NOW));

        verify(values).set(eq("k"), argThat((String json) -> json.contains("Текст") && json.contains("2026-10-31T10:00:00")),
                eq(Duration.ofHours(6)));
    }

    @Test
    void readsBackWhatWasSaved() throws Exception {
        String json = mapper.writeValueAsString(new AiUsageStore.CachedAdvice("Текст", AiTestData.NOW));
        when(values.get("k")).thenReturn(json);

        Optional<AiUsageStore.CachedAdvice> found = store.find("k");

        assertTrue(found.isPresent());
        assertEquals("Текст", found.get().text());
        assertEquals(AiTestData.NOW, found.get().generatedAt());
    }

    @Test
    void anUnknownKeyIsAMiss() {
        when(values.get("k")).thenReturn(null);

        assertTrue(store.find("k").isEmpty());
    }

    @Test
    void aBrokenRecordOrARedisFailureIsJustAMiss() {
        when(values.get("broken")).thenReturn("{not json");
        when(values.get("down")).thenThrow(new RedisConnectionFailureException("down"));

        assertTrue(store.find("broken").isEmpty());
        assertTrue(store.find("down").isEmpty());
    }

    @Test
    void aFailureToCacheDoesNotBreakTheRequest() {
        doThrow(new RedisConnectionFailureException("down")).when(values).set(anyString(), anyString(), any(Duration.class));

        assertDoesNotThrow(() -> store.save("k", new AiUsageStore.CachedAdvice("Текст", AiTestData.NOW)));
    }

    // ---------- лимит на пользователя ----------

    @Test
    void theFirstRequestOpensTheWindow() {
        when(values.increment("ai-test:quota:7")).thenReturn(1L);
        when(redis.getExpire("ai-test:quota:7", TimeUnit.SECONDS)).thenReturn(-1L);

        assertDoesNotThrow(() -> store.consume(7));

        verify(redis).expire("ai-test:quota:7", Duration.ofHours(1));
    }

    @Test
    void requestsInsideTheLimitPassAndDoNotResetTheWindow() {
        when(values.increment("ai-test:quota:7")).thenReturn(3L);
        when(redis.getExpire("ai-test:quota:7", TimeUnit.SECONDS)).thenReturn(1800L);

        assertDoesNotThrow(() -> store.consume(7));

        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void theRequestOverTheLimitIsRejectedWithTheTimeLeftInTheWindow() {
        when(values.increment("ai-test:quota:7")).thenReturn(4L);
        when(redis.getExpire("ai-test:quota:7", TimeUnit.SECONDS)).thenReturn(1234L);

        AiException error = assertThrows(AiException.class, () -> store.consume(7));

        assertEquals(AiException.RATE_LIMIT_EXCEEDED, error.getErrorCode());
        assertEquals(429, error.getStatus().value());
        assertEquals(1234L, error.getRetryAfterSeconds());
    }

    @Test
    void aCounterThatLostItsExpiryAfterACrashGetsOneAgain() {
        when(values.increment("ai-test:quota:7")).thenReturn(2L);
        when(redis.getExpire("ai-test:quota:7", TimeUnit.SECONDS)).thenReturn(-1L);

        store.consume(7);

        verify(redis).expire("ai-test:quota:7", Duration.ofHours(1));
    }

    @Test
    void usersHaveSeparateCounters() {
        when(values.increment(anyString())).thenReturn(1L);
        when(redis.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(100L);

        store.consume(7);
        store.consume(8);

        verify(values).increment("ai-test:quota:7");
        verify(values).increment("ai-test:quota:8");
    }

    @Test
    void whenRedisIsDownPaidRequestsAreRefusedNotLetThrough() {
        when(values.increment(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        AiException error = assertThrows(AiException.class, () -> store.consume(7));

        assertEquals(AiException.UNAVAILABLE, error.getErrorCode());
    }

    @Test
    void refundTakesOneRequestBackAndSurvivesRedisFailure() {
        store.refund(7);
        verify(values).decrement("ai-test:quota:7");

        when(values.decrement(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        assertDoesNotThrow(() -> store.refund(7));
    }
}
