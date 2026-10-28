package dev.zhulidov.cash_tracker.app;

import org.junit.jupiter.api.Test;

// Smoke-тест: поднимает весь контекст приложения на настоящем PostgreSQL и прогоняет
// на нём все Liquibase-миграции. Ловит рассинхрон "entity есть, а таблицы/колонки в changelog нет"
// (ddl-auto=validate) — именно на этом контекст падал, когда у User появился balance без миграции.
class CashTrackerApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }

}
