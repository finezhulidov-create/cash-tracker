package dev.zhulidov.cash_tracker.app.ai;

import org.springframework.http.HttpStatus;

/**
 * Ошибка ИИ-советника с готовым HTTP-статусом и машиночитаемым кодом для фронтенда.
 * Подробности (адреса, тела ответов Сбера) в сообщение не попадают: они только в логе.
 */
public class AiException extends RuntimeException {

    public static final String DISABLED = "AI_DISABLED";
    public static final String UNAVAILABLE = "AI_UNAVAILABLE";
    public static final String AUTH_FAILED = "AI_AUTH_FAILED";
    public static final String QUOTA_EXHAUSTED = "AI_QUOTA_EXHAUSTED";
    public static final String BUSY = "AI_BUSY";
    public static final String RATE_LIMIT_EXCEEDED = "AI_RATE_LIMIT_EXCEEDED";
    public static final String BAD_RESPONSE = "AI_BAD_RESPONSE";

    private final HttpStatus status;
    private final String errorCode;
    private final Long retryAfterSeconds;

    public AiException(HttpStatus status, String errorCode, String message) {
        this(status, errorCode, message, null);
    }

    public AiException(HttpStatus status, String errorCode, String message, Long retryAfterSeconds) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /** Через сколько секунд можно повторить; null, если неизвестно. */
    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public static AiException disabled() {
        return new AiException(HttpStatus.SERVICE_UNAVAILABLE, DISABLED, "AI advisor is not configured on this server");
    }

    public static AiException unavailable() {
        return new AiException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE, "AI service is temporarily unavailable");
    }

    public static AiException authFailed() {
        return new AiException(HttpStatus.SERVICE_UNAVAILABLE, AUTH_FAILED, "AI service is temporarily unavailable");
    }

    public static AiException quotaExhausted() {
        return new AiException(HttpStatus.SERVICE_UNAVAILABLE, QUOTA_EXHAUSTED, "AI token quota is exhausted");
    }

    public static AiException busy(long retryAfterSeconds) {
        return new AiException(HttpStatus.TOO_MANY_REQUESTS, BUSY, "AI service is busy, try again later", retryAfterSeconds);
    }

    public static AiException rateLimited(long retryAfterSeconds) {
        return new AiException(HttpStatus.TOO_MANY_REQUESTS, RATE_LIMIT_EXCEEDED,
                "Too many AI requests, try again later", retryAfterSeconds);
    }

    public static AiException badResponse() {
        return new AiException(HttpStatus.BAD_GATEWAY, BAD_RESPONSE, "AI service returned an unusable answer");
    }
}
