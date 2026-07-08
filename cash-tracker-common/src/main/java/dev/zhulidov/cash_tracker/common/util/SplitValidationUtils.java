package dev.zhulidov.cash_tracker.common.util;

import dev.zhulidov.cash_tracker.common.exception.TransactionSplitMismatchException;

import java.math.BigDecimal;
import java.util.List;

public class SplitValidationUtils {
    public static void assertSplit(BigDecimal transactionAmount, List<BigDecimal> splitAmounts){
        BigDecimal sum = splitAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!(transactionAmount.compareTo(sum) == 0)){
            throw new TransactionSplitMismatchException("Split amount must be compare to transaction amount");
        }
    }
}
