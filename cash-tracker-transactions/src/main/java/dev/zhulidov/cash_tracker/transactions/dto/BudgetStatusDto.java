package dev.zhulidov.cash_tracker.transactions.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Статус бюджета за месяц: assigned — назначенный лимит, spent — сумма сплитов
 * по категории за месяц, available — остаток (может быть отрицательным при перерасходе).
 */
public record BudgetStatusDto(
        Long budgetId,
        LocalDate period,
        CategoryDto category,
        BigDecimal assigned,
        BigDecimal spent,
        BigDecimal available) {
}
