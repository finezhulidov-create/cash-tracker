package dev.zhulidov.cash_tracker.transactions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ExpenseCreateRequest(
        @NotBlank
        String expense,
        @NotNull
        @Positive
        BigDecimal amount
       ) {
}
