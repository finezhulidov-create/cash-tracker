package dev.zhulidov.cash_tracker.transactions.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ImportKeyTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 28, 14, 30);

    @Test
    void of_dropsSecondsAndNanos() {
        ImportKey key = ImportKey.of(AT.withSecond(59).withNano(123), new BigDecimal("350.00"));

        assertEquals(AT, key.dateTime());
    }

    @Test
    void of_normalizesAmountScale() {
        // BigDecimal.equals учитывает масштаб: без нормализации 350 и 350.00 были бы разными ключами
        assertEquals(new BigDecimal("350.00"), ImportKey.of(AT, new BigDecimal("350")).amount());
        assertEquals(new BigDecimal("350.50"), ImportKey.of(AT, new BigDecimal("350.5")).amount());
    }

    @Test
    void sameOperationFromStatementAndDatabase_givesEqualKeys() {
        ImportKey fromStatement = ImportKey.of(AT, new BigDecimal("350"));
        ImportKey fromDatabase = ImportKey.of(AT.withSecond(5), new BigDecimal("350.00"));

        assertEquals(fromStatement, fromDatabase);
        assertEquals(fromStatement.hashCode(), fromDatabase.hashCode());
    }

    @Test
    void differentMinuteOrAmount_givesDifferentKeys() {
        ImportKey key = ImportKey.of(AT, new BigDecimal("350.00"));

        assertNotEquals(key, ImportKey.of(AT.plusMinutes(1), new BigDecimal("350.00")));
        assertNotEquals(key, ImportKey.of(AT, new BigDecimal("350.01")));
    }
}
