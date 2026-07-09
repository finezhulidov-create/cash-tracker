package dev.zhulidov.cash_tracker.transactions.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionSearchCriteria {
    private Long categoryId;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private LocalDateTime from;
    private LocalDateTime to;
}
