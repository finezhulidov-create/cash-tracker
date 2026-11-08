package dev.zhulidov.cash_tracker.transactions.dto;

import java.math.BigDecimal;

public record ParsedAmount(
        BigDecimal value,
        boolean income
) {

    public BigDecimal absolute(){
        return value.abs();
    }
}
