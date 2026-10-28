package dev.zhulidov.cash_tracker.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.YearMonth;

@Schema(description = "Траты за месяц")
public record MonthlySpendingDto(
        @Schema(type = "string", example = "2026-10") YearMonth month,
        @Schema(example = "12500.00") BigDecimal spent
) {
}
