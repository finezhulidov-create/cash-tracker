package dev.zhulidov.cash_tracker.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Smoke-тест: поднимает весь контекст приложения на настоящем PostgreSQL и прогоняет
// на нём все Liquibase-миграции. Ловит рассинхрон "entity есть, а таблицы в changelog нет"
// (ddl-auto=validate) — именно на этом контекст падал раньше.
//
// Redis и Kafka контейнерами не поднимаем: их клиенты подключаются лениво,
// для старта контекста живые брокеры не нужны.
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CashTrackerApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void contextLoads() {
    }

}
