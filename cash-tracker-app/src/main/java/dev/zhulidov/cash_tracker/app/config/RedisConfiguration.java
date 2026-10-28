package dev.zhulidov.cash_tracker.app.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.zhulidov.cash_tracker.transactions.dto.CategoryDto;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionSplitDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

@Configuration
public class RedisConfiguration {

    static final Duration TTL = Duration.ofMinutes(15);

    /**
     * Для каждого кэша значение сериализуется строго типизированно (List&lt;DTO&gt;), без полиморфной
     * типизации. Универсальный GenericJackson2JsonRedisSerializer не годится: он не записывает тип для
     * final-классов JDK (например, неизменяемого списка из Stream.toList()), а при чтении требует его,
     * из-за чего второе чтение из кэша падало с SerializationException.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(TTL)
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withCacheConfiguration("categories", listOf(defaults, mapper, CategoryDto.class))
                .withCacheConfiguration("splits", listOf(defaults, mapper, TransactionSplitDto.class))
                .build();
    }

    private static RedisCacheConfiguration listOf(RedisCacheConfiguration base, ObjectMapper mapper, Class<?> element) {
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, element);
        return base.serializeValuesWith(SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(mapper, listType)));
    }
}
