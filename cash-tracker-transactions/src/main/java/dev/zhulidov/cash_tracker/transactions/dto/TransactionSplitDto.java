package dev.zhulidov.cash_tracker.transactions.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransactionSplitDto(
        @NotNull
        BigDecimal amount,
        @NotNull
        CategoryDto categoryDto,
        @NotNull
        TransactionDto transactionDto) {
}
