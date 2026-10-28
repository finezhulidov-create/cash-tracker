package dev.zhulidov.cash_tracker.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Проверяет параметры кэша без запуска Redis: TTL по ТЗ (FR-7.5: 10–15 минут) и запрет кэшировать null.
 * Реальную работу кэша (попадания и инвалидацию) проверяет ApiFlowIntegrationTest.
 */
class RedisConfigurationTest {

    private RedisCache cache(String name) {
        RedisCacheManager manager = new RedisConfiguration().cacheManager(mock(RedisConnectionFactory.class));
        manager.afterPropertiesSet();
        return (RedisCache) manager.getCache(name);
    }

    @Test
    void cacheManager_setsFifteenMinuteTtlForEveryCache() {
        assertEquals(Duration.ofMinutes(15), cache("categories").getCacheConfiguration().getTtl());
        assertEquals(Duration.ofMinutes(15), cache("splits").getCacheConfiguration().getTtl());
    }

    @Test
    void cacheManager_doesNotCacheNullValues() {
        assertFalse(cache("categories").getCacheConfiguration().getAllowCacheNullValues());
    }

    @Test
    void cacheManager_createsCachesWithRequestedNames() {
        assertEquals("categories", cache("categories").getName());
        assertEquals("splits", cache("splits").getName());
    }
}
