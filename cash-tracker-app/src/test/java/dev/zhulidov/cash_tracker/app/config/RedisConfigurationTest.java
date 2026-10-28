package dev.zhulidov.cash_tracker.app.config;

import dev.zhulidov.cash_tracker.transactions.dto.CategoryDto;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionSplitDto;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object roundTrip(String cacheName, Object value) {
        RedisSerializationContext.SerializationPair pair = cache(cacheName).getCacheConfiguration().getValueSerializationPair();
        return pair.read(pair.write(value));
    }

    @Test
    void categoriesCache_roundTripsImmutableListOfRecords() {
        // регрессия: Stream.toList() даёт final-класс JDK; универсальный сериализатор писал его без типа
        // и падал при чтении с "expected VALUE_STRING ... contains type id"
        List<CategoryDto> cached = Stream.of(new CategoryDto(1L, "Food", 7L), new CategoryDto(2L, "Home", 7L)).toList();

        assertEquals(cached, roundTrip("categories", cached));
    }

    @Test
    void splitsCache_roundTripsImmutableListOfSplitsWithNestedCategory() {
        List<TransactionSplitDto> cached = Stream.of(
                new TransactionSplitDto(1L, 100L, new BigDecimal("700.00"), new CategoryDto(10L, "Food", 7L)),
                new TransactionSplitDto(2L, 101L, new BigDecimal("0.50"), new CategoryDto(10L, "Food", 7L))).toList();

        assertEquals(cached, roundTrip("splits", cached));
    }

    @Test
    void categoriesCache_roundTripsEmptyList() {
        assertEquals(List.of(), roundTrip("categories", List.<CategoryDto>of()));
    }

    @Test
    void cacheManager_createsCachesWithRequestedNames() {
        assertEquals("categories", cache("categories").getName());
        assertEquals("splits", cache("splits").getName());
    }
}
