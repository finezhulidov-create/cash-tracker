package dev.zhulidov.cash_tracker.transactions.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDto(
        @NotNull
        Long id,
        @NotNull
        Long userId,
        @NotNull
        BigDecimal amount,
        @NotNull
        String description,
        @NotNull
        LocalDateTime dateTime

        ) {
}
