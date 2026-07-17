package dev.zhulidov.cash_tracker.transactions.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetDto(
        @NotNull
        BigDecimal limitAmount,
        LocalDate period,
        @NotNull
        CategoryDto category) {
}
