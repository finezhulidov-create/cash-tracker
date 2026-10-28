package dev.zhulidov.cash_tracker.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Бюджет на месяц")
public record BudgetDto(
        @Schema(example = "5") Long id,
        @Schema(description = "Лимит трат за месяц", example = "5000.00") BigDecimal limitAmount,
        @Schema(description = "Первое число месяца", example = "2026-10-01") LocalDate period,
        @Schema(description = "Категория бюджета; null для общего бюджета на весь месяц", nullable = true)
        CategoryDto category) {
}
