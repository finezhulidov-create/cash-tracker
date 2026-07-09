package dev.zhulidov.cash_tracker.transactions.dto;


import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record TransactionCreateRequest(
        List<TransactionSplitDto> splits,
        @NotNull
        String description,
        @NotNull
        BigDecimal amount
) {
}
