package dev.zhulidov.cash_tracker.transactions.dto;

import jakarta.validation.constraints.NotBlank;

public record ExpenseUpdateRequestDto(
        @NotBlank
        String updateExpense
) {
}
