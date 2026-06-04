package dev.zhulidov.cash_tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExpenseDto(String expense, BigDecimal amount, LocalDateTime dateTime) {
}
