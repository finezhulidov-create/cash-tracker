package dev.zhulidov.cash_tracker.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zhulidov.cash_tracker.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Пишет ошибку в формате ErrorResponse прямо в ответ. Нужен для мест, где не работает
 * GlobalExceptionHandler, то есть до контроллеров: фильтры и обработчики Spring Security.
 */
final class JsonErrorWriter {

    private JsonErrorWriter() {
    }

    static void write(ObjectMapper objectMapper, HttpServletResponse response,
                      int status, String errorCode, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                ErrorResponse.from(status, message, LocalDateTime.now(), errorCode));
    }
}
