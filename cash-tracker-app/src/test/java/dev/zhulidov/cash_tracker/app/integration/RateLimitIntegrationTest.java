package dev.zhulidov.cash_tracker.app.integration;

import dev.zhulidov.cash_tracker.app.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Рейт-лимит на настоящем Redis с боевым значением 30 запросов. В остальных интеграционных тестах лимит
 * практически отключён (application-test.properties), а здесь включён, поэтому у класса свой контекст.
 * Окно длинное и ключи уникальны, чтобы счётчик не пересекался с другими тестами и не сбрасывался посреди теста.
 */
class RateLimitIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void rateLimitProperties(DynamicPropertyRegistry registry) {
        registry.add("app.rate-limit.auth.requests", () -> 30);
        registry.add("app.rate-limit.auth.window-seconds", () -> 3600);
        registry.add("app.rate-limit.auth.key-prefix", () -> "rl-it-" + UUID.randomUUID() + ":");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void thirtyAuthRequestsAreAllowedAndTheThirtyFirstIsRejectedWith429() throws Exception {
        for (int i = 1; i <= 30; i++) {
            // пустое тело даёт 400 от валидации, то есть запрос дошёл до контроллера и лимит не сработал
            mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }

        String retryAfter = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andReturn().getResponse().getHeader("Retry-After");

        long seconds = Long.parseLong(retryAfter);
        assertTrue(seconds > 0 && seconds <= 3600, "Retry-After was " + seconds);
    }

    @Test
    void limitIsSharedByAllAuthEndpointsAndDoesNotAffectTheRestOfTheApi() throws Exception {
        // исчерпываем лимит (если предыдущий тест уже сделал это, дополнительные запросы просто отклоняются)
        for (int i = 0; i < 32; i++) {
            mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"));
        }

        // регистрация делит тот же счётчик, что и логин
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests());

        // остальное API и служебные эндпоинты не ограничиваются
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v1/users/me")).andExpect(status().isUnauthorized());
    }
}
