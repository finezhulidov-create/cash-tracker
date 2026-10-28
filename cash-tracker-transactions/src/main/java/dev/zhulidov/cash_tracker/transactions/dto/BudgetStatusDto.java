package dev.zhulidov.cash_tracker.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Статус бюджета за месяц: assigned — назначенный лимит, spent — сколько потрачено за месяц
 * (по категории бюджета или, для общего бюджета, по всем тратам), available — остаток
 * (отрицательный при перерасходе). Остаток считается при запросе, а не хранится.
 */
@Schema(description = "Статус бюджета: лимит, потрачено, остаток")
public record BudgetStatusDto(
        @Schema(example = "5") Long budgetId,
        @Schema(example = "2026-10-01") LocalDate period,
        @Schema(description = "Категория бюджета; null для общего бюджета", nullable = true) CategoryDto category,
        @Schema(example = "5000.00") BigDecimal assigned,
        @Schema(example = "1200.50") BigDecimal spent,
        @Schema(description = "Остаток; отрицательный при перерасходе", example = "3799.50") BigDecimal available) {
}
