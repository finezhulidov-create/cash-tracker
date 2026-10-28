package dev.zhulidov.cash_tracker.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Изменение бюджета: лимит и месяц (категория не меняется)")
public record BudgetUpdateRequest(
        @Schema(example = "7000.00")
        @NotNull
        @Positive
        BigDecimal limitAmount,

        @Schema(description = "Любая дата месяца, бюджет привязывается к первому числу", example = "2026-10-01")
        @NotNull
        LocalDate period
) {
}
