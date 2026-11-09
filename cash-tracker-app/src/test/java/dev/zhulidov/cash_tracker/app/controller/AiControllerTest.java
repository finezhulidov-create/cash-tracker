package dev.zhulidov.cash_tracker.app.controller;

import dev.zhulidov.cash_tracker.app.ai.AiException;
import dev.zhulidov.cash_tracker.app.ai.MonthAdvisorService;
import dev.zhulidov.cash_tracker.app.dto.AiAdviceResponse;
import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.common.exception.InvalidRequestException;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonthAdvisorService advisor;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final Long OWNER_ID = 1L;
    private static final String BODY = "{\"yearMonth\":\"2026-10\"}";

    private UsernamePasswordAuthenticationToken authToken() {
        var principal = new UserPrincipal(OWNER_ID, "owner@test.com", "hash");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private org.springframework.test.web.servlet.ResultActions advice(String body) throws Exception {
        return mockMvc.perform(post("/v1/ai/advice").contentType(MediaType.APPLICATION_JSON).content(body)
                .with(authentication(authToken())).with(csrf()));
    }

    // ==================== GET /v1/ai/status ====================

    @Test
    void status_tellsWhetherTheAdvisorIsOn() throws Exception {
        when(advisor.isEnabled()).thenReturn(true, false);

        mockMvc.perform(get("/v1/ai/status").with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
        mockMvc.perform(get("/v1/ai/status").with(authentication(authToken())))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void status_requiresALogin() throws Exception {
        mockMvc.perform(get("/v1/ai/status")).andExpect(status().isUnauthorized());
    }

    // ==================== POST /v1/ai/advice ====================

    @Test
    void advice_returnsTheTextAndAsksForTheMonthOfTheCurrentUser() throws Exception {
        when(advisor.advise(OWNER_ID, YearMonth.of(2026, 10)))
                .thenReturn(new AiAdviceResponse("### Итог\nВсё хорошо", Instant.parse("2026-10-31T10:05:00Z"), false));

        advice(BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("### Итог\nВсё хорошо"))
                .andExpect(jsonPath("$.generatedAt").value("2026-10-31T10:05:00Z"))
                .andExpect(jsonPath("$.cached").value(false));

        verify(advisor).advise(OWNER_ID, YearMonth.of(2026, 10));
    }

    @Test
    void advice_tellsWhenTheAnswerIsFromTheCache() throws Exception {
        when(advisor.advise(OWNER_ID, YearMonth.of(2026, 10)))
                .thenReturn(new AiAdviceResponse("Текст", Instant.parse("2026-10-31T10:05:00Z"), true));

        advice(BODY).andExpect(jsonPath("$.cached").value(true));
    }

    @Test
    void advice_takesTheUserFromTheTokenNotFromTheBody() throws Exception {
        when(advisor.advise(anyLong(), any())).thenReturn(new AiAdviceResponse("t", Instant.now(), false));

        advice("{\"yearMonth\":\"2026-10\",\"userId\":999}").andExpect(status().isOk());

        verify(advisor).advise(eq(OWNER_ID), any());
        verify(advisor, never()).advise(eq(999L), any());
    }

    @Test
    void advice_requiresALogin() throws Exception {
        mockMvc.perform(post("/v1/ai/advice").contentType(MediaType.APPLICATION_JSON).content(BODY).with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(advisor);
    }

    @Test
    void advice_withoutAMonthIs400() throws Exception {
        advice("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(advisor);
    }

    @Test
    void advice_withAnImpossibleMonthIs400() throws Exception {
        advice("{\"yearMonth\":\"2026-13\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));

        verifyNoInteractions(advisor);
    }

    @Test
    void advice_withoutABodyIs400() throws Exception {
        mockMvc.perform(post("/v1/ai/advice").with(authentication(authToken())).with(csrf()))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(advisor);
    }

    @Test
    void advice_forAFutureMonthIs400WithTheUsualErrorBody() throws Exception {
        when(advisor.advise(anyLong(), any())).thenThrow(new InvalidRequestException("'yearMonth' must not be in the future"));

        advice(BODY)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400));
    }

    // ---------- ошибки ИИ-сервиса: общий формат ErrorResponse ----------

    @Test
    void advice_whenSwitchedOffIs503() throws Exception {
        when(advisor.advise(anyLong(), any())).thenThrow(AiException.disabled());

        advice(BODY)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.errorCode").value("AI_DISABLED"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void advice_whenTheModelIsBusyIs429WithRetryAfter() throws Exception {
        when(advisor.advise(anyLong(), any())).thenThrow(AiException.busy(30));

        advice(BODY)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.errorCode").value("AI_BUSY"));
    }

    @Test
    void advice_whenTheUserLimitIsSpentIs429WithRetryAfter() throws Exception {
        when(advisor.advise(anyLong(), any())).thenThrow(AiException.rateLimited(1800));

        advice(BODY)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "1800"))
                .andExpect(jsonPath("$.errorCode").value("AI_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void advice_whenTheServiceFailsIs503WithoutRetryAfter() throws Exception {
        when(advisor.advise(anyLong(), any())).thenThrow(AiException.unavailable());

        advice(BODY)
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().doesNotExist("Retry-After"))
                .andExpect(jsonPath("$.errorCode").value("AI_UNAVAILABLE"));
    }

    @Test
    void advice_whenTheAnswerIsUnusableIs502() throws Exception {
        when(advisor.advise(anyLong(), any())).thenThrow(AiException.badResponse());

        advice(BODY)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("AI_BAD_RESPONSE"));
    }

    @Test
    void advice_whenTheQuotaIsExhaustedIs503() throws Exception {
        when(advisor.advise(anyLong(), any())).thenThrow(AiException.quotaExhausted());

        advice(BODY)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("AI_QUOTA_EXHAUSTED"));
    }

    @Test
    void advice_anUnexpectedFailureStaysAnOrdinary500WithoutDetails() throws Exception {
        when(advisor.advise(anyLong(), any())).thenThrow(new IllegalStateException("secret internals"));

        advice(BODY)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }
}
