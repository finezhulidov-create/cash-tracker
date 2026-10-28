package dev.zhulidov.cash_tracker.common.exception;

/**
 * Конфликт с текущим состоянием ресурса (HTTP 409). errorCode — машиночитаемый код для фронта.
 */
public class ResourceConflictException extends RuntimeException {

    private final String errorCode;

    public ResourceConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
