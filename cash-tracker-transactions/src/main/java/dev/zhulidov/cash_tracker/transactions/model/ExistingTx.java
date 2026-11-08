package dev.zhulidov.cash_tracker.transactions.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ExistingTx {

     LocalDateTime getDateTime();
     BigDecimal getAmount();

}
