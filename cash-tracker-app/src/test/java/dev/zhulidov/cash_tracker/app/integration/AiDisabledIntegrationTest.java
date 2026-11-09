package dev.zhulidov.cash_tracker.app.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import dev.zhulidov.cash_tracker.app.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ИИ-советник необязателен: профиль test его не включает, и приложение работает как раньше —
 * контекст поднимается без ключа GigaChat, а сам эндпоинт честно отвечает, что функция не настроена.
 */
class AiDisabledIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String token() throws Exception {
        String body = mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "off-" + UUID.randomUUID() + "@test.com",
                                "username", "Tester", "password", "secret-password-1"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    @Test
    void statusSaysTheAdvisorIsOff() throws Exception {
        mockMvc.perform(get("/v1/ai/status").header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void askingForAdviceGivesAClear503AndNothingElseBreaks() throws Exception {
        String token = token();

        mockMvc.perform(post("/v1/ai/advice").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"yearMonth\":\"2026-10\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("AI_DISABLED"));

        // остальное API работает как обычно
        mockMvc.perform(get("/v1/users/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(get("/v1/analytics/summary").param("yearMonth", "2026-10")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }
}
