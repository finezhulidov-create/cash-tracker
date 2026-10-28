package dev.zhulidov.cash_tracker.common.exception;

public class ResourceAlreadyExistsException extends ResourceConflictException {

    public static final String DEFAULT_CODE = "RESOURCE_ALREADY_EXISTS";

    public ResourceAlreadyExistsException(String message) {
        super(DEFAULT_CODE, message);
    }

    public ResourceAlreadyExistsException(String errorCode, String message) {
        super(errorCode, message);
    }
}
