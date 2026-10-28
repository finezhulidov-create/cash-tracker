package dev.zhulidov.cash_tracker.app;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Контейнеры для интеграционных тестов. Запускаются лениво при первом обращении и общие
 * для всех тестов в рамках одного прогона (останавливаются Ryuk'ом при выходе из JVM),
 * поэтому Postgres и Redis поднимаются один раз, а не на каждый тестовый класс.
 */
public final class TestContainers {

    private static PostgreSQLContainer<?> postgres;
    private static GenericContainer<?> redis;

    private TestContainers() {
    }

    public static synchronized PostgreSQLContainer<?> postgres() {
        if (postgres == null) {
            postgres = new PostgreSQLContainer<>("postgres:16-alpine");
            postgres.start();
        }
        return postgres;
    }

    public static synchronized GenericContainer<?> redis() {
        if (redis == null) {
            redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
            redis.start();
        }
        return redis;
    }
}
