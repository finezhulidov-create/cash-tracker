package dev.zhulidov.cash_tracker.app;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Основа интеграционных тестов: полный контекст приложения (настоящая цепочка безопасности,
 * Liquibase, JPA, Redis-кэш) на реальных Postgres и Redis в Docker.
 * Все наследники делят один контекст и одни контейнеры, поэтому стартуют быстро.
 * Требуют запущенного Docker; пропустить: mvn test -DexcludedGroups=integration
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TestContainers.postgres().getJdbcUrl());
        registry.add("spring.datasource.username", () -> TestContainers.postgres().getUsername());
        registry.add("spring.datasource.password", () -> TestContainers.postgres().getPassword());
        registry.add("spring.data.redis.host", () -> TestContainers.redis().getHost());
        registry.add("spring.data.redis.port", () -> TestContainers.redis().getMappedPort(6379));
    }
}
