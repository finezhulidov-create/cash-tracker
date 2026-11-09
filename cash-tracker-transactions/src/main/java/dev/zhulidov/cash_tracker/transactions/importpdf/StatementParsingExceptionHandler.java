package dev.zhulidov.cash_tracker.transactions.importpdf;

import dev.zhulidov.cash_tracker.common.dto.ErrorResponse;
import dev.zhulidov.cash_tracker.transactions.controller.AiParsingController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/** Превращает {@link StatementParsingException} в ответ в общем формате ошибок API. */
@RestControllerAdvice(assignableTypes = AiParsingController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StatementParsingExceptionHandler {

    @ExceptionHandler(StatementParsingException.class)
    public ResponseEntity<ErrorResponse> handle(StatementParsingException e) {
        return ResponseEntity.status(e.getStatus())
                .body(ErrorResponse.from(e.getStatus().value(), e.getMessage(), LocalDateTime.now(), e.getErrorCode()));
    }
}
