package dev.zhulidov.cash_tracker.transactions.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetCreateRequest(
        @NotNull
        @Positive
        BigDecimal amount,
        LocalDate period,
        @NotNull
        CategoryDto category

) {
}
