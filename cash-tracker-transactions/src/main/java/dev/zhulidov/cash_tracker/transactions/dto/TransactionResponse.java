package dev.zhulidov.cash_tracker.transactions.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(String description, BigDecimal amount, LocalDateTime dateTime) {
}
