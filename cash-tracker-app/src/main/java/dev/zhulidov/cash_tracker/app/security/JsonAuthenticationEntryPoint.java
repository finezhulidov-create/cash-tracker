package dev.zhulidov.cash_tracker.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Запрос без токена или с невалидным токеном: 401 в формате ErrorResponse.
 * Без этого Spring Security по умолчанию отвечал 403 с пустым телом, и фронт не мог отличить
 * «нужно войти» от «нет прав».
 */
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        JsonErrorWriter.write(objectMapper, response, HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHORIZED", "Authentication required");
    }
}
