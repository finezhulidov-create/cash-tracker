package dev.zhulidov.cash_tracker.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Schema(description = "Сводка месяца для дашборда")
public record MonthSummaryDto(
        @Schema(type = "string", example = "2026-10") YearMonth yearMonth,
        @Schema(description = "Все траты месяца независимо от бюджетов", example = "8200.00") BigDecimal totalSpent,
        @Schema(description = "Общий бюджет месяца (без категории); null, если не задан", nullable = true)
        BudgetStatusDto overall,
        @Schema(description = "Бюджеты по категориям с остатками, по названию категории")
        List<BudgetStatusDto> categories
) {
}
