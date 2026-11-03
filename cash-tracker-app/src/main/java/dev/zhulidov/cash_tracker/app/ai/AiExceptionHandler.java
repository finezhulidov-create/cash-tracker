package dev.zhulidov.cash_tracker.app.ai;

import dev.zhulidov.cash_tracker.app.controller.AiController;
import dev.zhulidov.cash_tracker.common.dto.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/** Превращает {@link AiException} в ответ в общем формате ошибок API (с заголовком Retry-After, если он известен). */
@RestControllerAdvice(assignableTypes = AiController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AiExceptionHandler {

    @ExceptionHandler(AiException.class)
    public ResponseEntity<ErrorResponse> handle(AiException e) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(e.getStatus());
        if (e.getRetryAfterSeconds() != null) {
            response.header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()));
        }
        return response.body(ErrorResponse.from(e.getStatus().value(), e.getMessage(), LocalDateTime.now(), e.getErrorCode()));
    }
}
