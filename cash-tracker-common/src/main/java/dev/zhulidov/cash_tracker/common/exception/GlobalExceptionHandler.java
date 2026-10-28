package dev.zhulidov.cash_tracker.common.exception;

import dev.zhulidov.cash_tracker.common.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Единый формат ошибок (ErrorResponse) для всего API: доменные исключения, ошибки валидации
 * и стандартные исключения Spring MVC (неверный JSON, не тот тип параметра, 404/405/415 и т.д.).
 * Всё непредвиденное превращается в 500 без внутренних деталей — подробности только в логе.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ==================== доменные исключения ====================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", e.getMessage());
    }

    /** Чужой ресурс отдаём как 404, чтобы не раскрывать факт его существования. */
    @ExceptionHandler(InaccessibleResourceException.class)
    public ResponseEntity<ErrorResponse> handleInaccessible(InaccessibleResourceException e) {
        return error(HttpStatus.NOT_FOUND, "ACCESS_DENIED", e.getMessage());
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ResourceConflictException e) {
        return error(HttpStatus.CONFLICT, e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(TransactionSplitMismatchException.class)
    public ResponseEntity<ErrorResponse> handleSplitMismatch(TransactionSplitMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "NOT_VALID_TRANSACTION_SPLIT_SUM", e.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException e) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            errors.putIfAbsent(path.substring(path.lastIndexOf('.') + 1), violation.getMessage());
        });
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", errors);
    }

    /** Некорректное поле сортировки (?sort=unknown) — ошибка клиента, а не сервера. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handleUnknownSortProperty(PropertyReferenceException e) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_SORT", "Unknown sort property: " + e.getPropertyName());
    }

    /** Страховка от гонки: проверка «уже существует» прошла, а уникальный индекс всё равно сработал. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return error(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "The request conflicts with existing data");
    }

    // ==================== безопасность ====================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        if (e instanceof BadCredentialsException) {
            return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }
        log.warn("Authentication failed: {}", e.toString());
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
    }

    // ==================== всё остальное ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error");
    }

    // ==================== стандартные исключения Spring MVC ====================

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fieldError -> errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(globalError -> errors.putIfAbsent(globalError.getObjectName(), globalError.getDefaultMessage()));
        return body(status, headers, "VALIDATION_ERROR", "Validation failed", errors);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status,
                                                                            WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().stream().findFirst().ifPresent(
                    error -> errors.putIfAbsent(name != null ? name : "parameter", error.getDefaultMessage()));
        });
        return body(status, headers, "VALIDATION_ERROR", "Validation failed", errors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        return body(status, headers, "MALFORMED_REQUEST", "Request body is missing or malformed", null);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers, HttpStatusCode status,
                                                                          WebRequest request) {
        Map<String, String> errors = Map.of(ex.getParameterName(), "Parameter is required");
        return body(status, headers, "MISSING_PARAMETER", "Required parameter '" + ex.getParameterName() + "' is missing", errors);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : (ex.getPropertyName() != null ? ex.getPropertyName() : "parameter");
        Map<String, String> errors = Map.of(name, "Invalid value");
        return body(status, headers, "INVALID_PARAMETER", "Parameter '" + name + "' has invalid value", errors);
    }

    /** Остальные стандартные исключения (404 для неизвестного пути, 405, 415, ...) — в том же формате. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        String message = body instanceof ProblemDetail problem && problem.getDetail() != null
                ? problem.getDetail()
                : HttpStatus.valueOf(statusCode.value()).getReasonPhrase();
        return body(statusCode, headers, codeFor(statusCode), message, null);
    }

    // ==================== helpers ====================

    private static String codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "BAD_REQUEST";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 500 -> "INTERNAL_ERROR";
            default -> "HTTP_" + status.value();
        };
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.from(status.value(), message, LocalDateTime.now(), code));
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message,
                                                       Map<String, String> errors) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.from(status.value(), message, LocalDateTime.now(), code, errors));
    }

    private static ResponseEntity<Object> body(HttpStatusCode status, HttpHeaders headers, String code,
                                               String message, Map<String, String> errors) {
        ErrorResponse response = errors == null
                ? ErrorResponse.from(status.value(), message, LocalDateTime.now(), code)
                : ErrorResponse.from(status.value(), message, LocalDateTime.now(), code, errors);
        return ResponseEntity.status(status).headers(headers).body(response);
    }
}
