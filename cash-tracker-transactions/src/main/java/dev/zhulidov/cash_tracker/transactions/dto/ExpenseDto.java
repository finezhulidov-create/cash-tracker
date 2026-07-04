package dev.zhulidov.cash_tracker.transactions.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExpenseDto(String expense, BigDecimal amount, LocalDateTime dateTime) {
}
