package dev.zhulidov.cash_tracker.common.exception;

public class TransactionSplitMismatchException extends RuntimeException {
    public TransactionSplitMismatchException(String s) {
        super(s);
    }
}
