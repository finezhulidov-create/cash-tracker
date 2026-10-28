package dev.zhulidov.cash_tracker.common.exception;

/**
 * Запрос синтаксически корректен, но нарушает бизнес-правило (HTTP 400), например from позже to.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
