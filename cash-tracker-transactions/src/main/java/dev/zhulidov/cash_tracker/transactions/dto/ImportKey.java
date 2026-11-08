package dev.zhulidov.cash_tracker.transactions.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record ImportKey(
        LocalDateTime dateTime,
        BigDecimal amount
) {
    public static ImportKey of(LocalDateTime dateTime, BigDecimal amount){
        return new ImportKey(dateTime.truncatedTo(ChronoUnit.MINUTES), amount.setScale(2, RoundingMode.HALF_UP));
    }
}
