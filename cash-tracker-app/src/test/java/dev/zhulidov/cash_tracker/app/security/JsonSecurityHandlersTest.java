package dev.zhulidov.cash_tracker.app.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Обработчики безопасности пишут ErrorResponse напрямую в ответ, потому что до них не доходит
 * GlobalExceptionHandler (они работают в цепочке фильтров, до контроллеров).
 */
class JsonSecurityHandlersTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void entryPoint_answers401WithBearerChallengeAndErrorBody() throws Exception {
        var response = new MockHttpServletResponse();

        new JsonAuthenticationEntryPoint(objectMapper).commence(
                new MockHttpServletRequest("GET", "/v1/users/me"), response,
                new InsufficientAuthenticationException("no token"));

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, body.get("status").asInt());
        assertEquals("UNAUTHORIZED", body.get("errorCode").asText());
        assertEquals("Authentication required", body.get("message").asText());
        assertFalse(body.get("timestamp").asText().isBlank());
    }

    @Test
    void entryPoint_doesNotLeakTheReasonOfAuthenticationFailure() throws Exception {
        var response = new MockHttpServletResponse();

        new JsonAuthenticationEntryPoint(objectMapper).commence(
                new MockHttpServletRequest(), response, new InsufficientAuthenticationException("secret internal reason"));

        assertFalse(response.getContentAsString().contains("secret internal reason"));
    }

    @Test
    void accessDeniedHandler_answers403WithErrorBody() throws Exception {
        var response = new MockHttpServletResponse();

        new JsonAccessDeniedHandler(objectMapper).handle(
                new MockHttpServletRequest("DELETE", "/v1/users/1"), response, new AccessDeniedException("denied"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(403, body.get("status").asInt());
        assertEquals("FORBIDDEN", body.get("errorCode").asText());
        assertEquals("Access denied", body.get("message").asText());
    }

    @Test
    void accessDeniedHandler_writesUtf8() throws Exception {
        var response = new MockHttpServletResponse();

        new JsonAccessDeniedHandler(objectMapper).handle(
                new MockHttpServletRequest(), response, new AccessDeniedException("denied"));

        assertEquals("UTF-8", response.getCharacterEncoding());
    }
}
