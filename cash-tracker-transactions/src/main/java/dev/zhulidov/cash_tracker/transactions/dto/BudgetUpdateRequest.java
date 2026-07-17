package dev.zhulidov.cash_tracker.transactions.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetUpdateRequest(
        BigDecimal limitAmount,
        LocalDate period,
        CategoryDto categoryDto
) {
}
