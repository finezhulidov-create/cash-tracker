package dev.zhulidov.cash_tracker.transactions.importpdf;

import org.springframework.http.HttpStatus;

/**
 * Сбой модели при разборе выписки, с готовым HTTP-статусом и кодом для фронтенда.
 * Подробности (адрес сервера модели, текст исключения) в сообщение не попадают: они только в логе.
 */
public class StatementParsingException extends RuntimeException {

    public static final String MODEL_UNAVAILABLE = "IMPORT_MODEL_UNAVAILABLE";
    public static final String BAD_RESPONSE = "IMPORT_BAD_RESPONSE";

    private final HttpStatus status;
    private final String errorCode;

    private StatementParsingException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    /** Сервер модели не отвечает, не принял запрос или модель не загружена. */
    public static StatementParsingException modelUnavailable(Throwable cause) {
        return new StatementParsingException(HttpStatus.SERVICE_UNAVAILABLE, MODEL_UNAVAILABLE,
                "Statement parsing model is unavailable", cause);
    }

    /** Модель ответила, но не тем JSON, который ожидался. */
    public static StatementParsingException badResponse(Throwable cause) {
        return new StatementParsingException(HttpStatus.BAD_GATEWAY, BAD_RESPONSE,
                "Statement parsing model returned an unusable answer", cause);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isModelUnavailable() {
        return MODEL_UNAVAILABLE.equals(errorCode);
    }
}
