package dev.zhulidov.cash_tracker.common.util;

import dev.zhulidov.cash_tracker.common.exception.TransactionSplitMismatchException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class SplitValidationUtilsTest {

    @Test
    void assertSplit_shouldThrowsWhenSumMismatch(){
        BigDecimal transactionAmount = new BigDecimal("2500.00");
        List<BigDecimal> splits = List.of(new BigDecimal("2000.00"), new BigDecimal("400.00"));
        assertThrows(TransactionSplitMismatchException.class,
                ()-> SplitValidationUtils.assertSplit(transactionAmount,splits));
    }

    @Test
    void assertSplit_whenSumMatchesRegardlessOfScale_doesNtThrow(){
        BigDecimal transactionAmount = new BigDecimal("2000.0");
        List<BigDecimal> splits = List.of(new BigDecimal("2000.00"));
        assertDoesNotThrow(()->SplitValidationUtils.assertSplit(transactionAmount,splits));
    }
    @Test
    void assertSplit_whenSumMatchesTransactionAmount_doesNotThrow() {
        BigDecimal transactionAmount = new BigDecimal("2500.00");
        List<BigDecimal> splitAmounts = List.of(
                new BigDecimal("2000.00"),
                new BigDecimal("500.00")
        );

        assertDoesNotThrow(() -> SplitValidationUtils.assertSplit(transactionAmount, splitAmounts));
    }
}
