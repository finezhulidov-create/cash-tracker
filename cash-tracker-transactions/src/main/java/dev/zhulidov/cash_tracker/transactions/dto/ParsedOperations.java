package dev.zhulidov.cash_tracker.transactions.dto;

import java.util.List;

public record ParsedOperations(
     List<OneOperation> operations
) {
    public record OneOperation(
            String datetime,
            String description,
            String amount
    ){}
}
