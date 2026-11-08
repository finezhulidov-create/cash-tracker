package dev.zhulidov.cash_tracker.transactions.util;

import dev.zhulidov.cash_tracker.transactions.dto.ParsedAmount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StatementValuesTest {

    // ==================== parseAmount ====================

    @ParameterizedTest(name = "\"{0}\" → расход {1}")
    @CsvSource(delimiter = '|', value = {
            "-1 234,56 ₽     | -1234.56",
            "350,00          | -350.00",
            "350             | -350",
            "1 234.5    | -1234.5",     // неразрывный пробел между разрядами
            "2 500,00   | -2500.00",    // узкий неразрывный пробел
            "−350.00    | -350.00",     // типографский минус U+2212
            "–350.00    | -350.00",     // en dash вместо минуса
            "99,90 руб.      | -99.90",
            "99,90 RUB       | -99.90",
            "99,90 р.        | -99.90",
    })
    void parseAmount_expense(String raw, String expected) {
        Optional<ParsedAmount> parsed = StatementValues.parseAmount(raw);

        assertTrue(parsed.isPresent(), "не разобрано: " + raw);
        assertFalse(parsed.get().income());
        assertEquals(0, new BigDecimal(expected).compareTo(parsed.get().value()));
        assertEquals(0, new BigDecimal(expected).abs().compareTo(parsed.get().absolute()));
    }

    @Test
    void parseAmount_plusSignMeansIncome() {
        ParsedAmount parsed = StatementValues.parseAmount("+15 000,00 ₽").orElseThrow();

        assertTrue(parsed.income());
        assertEquals(new BigDecimal("15000.00"), parsed.value());
        assertEquals(new BigDecimal("15000.00"), parsed.absolute());
    }

    @ParameterizedTest(name = "\"{0}\" не разбирается")
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "abc",
            "12,345",            // три знака после запятой — не денежная сумма
            "1.234,56",          // точка как разделитель тысяч не поддерживается
            "0,00",              // нулевая операция бессмысленна
            "1 234,56 (комиссия 10)",
            "--100",
    })
    void parseAmount_rejectsGarbage(String raw) {
        assertTrue(StatementValues.parseAmount(raw).isEmpty());
    }

    // ==================== parseDateTime ====================

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource(delimiter = '|', value = {
            "28.10.2026 14:30         | 2026-10-28T14:30",
            "28.10.2026 14:30:05      | 2026-10-28T14:30:05",
            "28.10.26 14:30           | 2026-10-28T14:30",
            "28.10.26 9:05            | 2026-10-28T09:05",    // час одной цифрой
            "28.10.2026, 14:30        | 2026-10-28T14:30",    // запятая между датой и временем
            "  28.10.2026   14:30     | 2026-10-28T14:30",    // лишние пробелы
            "2026-10-28T14:30         | 2026-10-28T14:30",    // ISO
            "2026-10-28T14:30:05      | 2026-10-28T14:30:05",
            "28.10.2026               | 2026-10-28T00:00",    // без времени — начало дня
            "28.10.26                 | 2026-10-28T00:00",
            "2026-10-28               | 2026-10-28T00:00",
    })
    void parseDateTime_supportedFormats(String raw, String expected) {
        assertEquals(Optional.of(LocalDateTime.parse(expected)), StatementValues.parseDateTime(raw));
    }

    @Test
    void parseDateTime_keepsTimeInsteadOfStartOfDay() {
        // регресс: раньше дата с временем разбиралась через LocalDate и время терялось
        LocalDateTime parsed = StatementValues.parseDateTime("28.10.2026 14:30").orElseThrow();

        assertEquals(14, parsed.getHour());
        assertEquals(30, parsed.getMinute());
    }

    @ParameterizedTest(name = "\"{0}\" не разбирается")
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "вчера",
            "31.02.2026 10:00",   // несуществующая дата (STRICT)
            "28.13.2026",         // нет 13-го месяца
            "28.10.2026 25:00",   // нет 25-го часа
            "28.10.2026 14:61",
            "123456",             // код авторизации — не время
    })
    void parseDateTime_rejectsInvalid(String raw) {
        assertTrue(StatementValues.parseDateTime(raw).isEmpty());
    }
}
