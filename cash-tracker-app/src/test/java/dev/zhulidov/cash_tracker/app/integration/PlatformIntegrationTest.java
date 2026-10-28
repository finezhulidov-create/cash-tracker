package dev.zhulidov.cash_tracker.app.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import dev.zhulidov.cash_tracker.app.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Инфраструктура вокруг API: Actuator, документация Swagger/OpenAPI, CORS и заголовки безопасности.
 */
class PlatformIntegrationTest extends AbstractIntegrationTest {

    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken() throws Exception {
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user-" + UUID.randomUUID() + "@test.com",
                                "username", "Tester", "password", "secret-password-1"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    // ==================== Actuator ====================

    @Test
    void health_isPublicAndReportsUpWithoutExposingComponentDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                // детали (БД, Redis) наружу не показываем
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void livenessAndReadinessProbes_areAvailableForOrchestrators() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void info_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
    }

    @Test
    void otherActuatorEndpoints_areClosedAndNotExposed() throws Exception {
        // без токена: закрыто безопасностью
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());

        // с токеном: эндпоинтов просто нет (не включены в management.endpoints.web.exposure.include)
        String token = registerAndGetToken();
        mockMvc.perform(get("/actuator/env").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/heapdump").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ==================== Swagger / OpenAPI ====================

    @Test
    void openApiDocument_isPublicAndDescribesTheCurrentApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Cash Tracker API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                // актуальные пути
                .andExpect(jsonPath("$.paths['/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/auth/register'].post").exists())
                .andExpect(jsonPath("$.paths['/v1/transactions'].post").exists())
                .andExpect(jsonPath("$.paths['/v1/transactions'].get").exists())
                .andExpect(jsonPath("$.paths['/v1/transactions/{transId}'].put").exists())
                .andExpect(jsonPath("$.paths['/v1/users/me/balance'].put").exists())
                .andExpect(jsonPath("$.paths['/v1/budgets/{budgetId}/status'].get").exists())
                .andExpect(jsonPath("$.paths['/v1/analytics/summary'].get").exists())
                .andExpect(jsonPath("$.paths['/v1/analytics/by-category'].get").exists())
                .andExpect(jsonPath("$.paths['/v1/analytics/monthly'].get").exists())
                // старых путей быть не должно
                .andExpect(jsonPath("$.paths['/v1/transactions/create']").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/transactions/all']").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/transactions/filter']").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/users/balance']").doesNotExist());
    }

    @Test
    void openApiDocument_usesProjectErrorResponseSchemaAndSplitRequestContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // схема ошибки — наша (а не org.springframework.web.ErrorResponse, как было в BudgetController)
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.errorCode").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.errors").exists())
                // в части траты передаются только categoryId и amount
                .andExpect(jsonPath("$.components.schemas.SplitRequest.properties.categoryId").exists())
                .andExpect(jsonPath("$.components.schemas.SplitRequest.properties.amount").exists())
                .andExpect(jsonPath("$.components.schemas.TransactionCreateRequest.properties.transactionDto").doesNotExist());
    }

    @Test
    void openApiDocument_marksAuthEndpointsAsNotRequiringAToken() throws Exception {
        String doc = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // у публичных операций security пустой, чтобы Swagger UI не требовал токен для входа и регистрации
        assertEquals(List.of(), JsonPath.read(doc, "$.paths['/auth/login'].post.security"));
        assertEquals(List.of(), JsonPath.read(doc, "$.paths['/auth/register'].post.security"));
        // а для остальных действует глобальное требование bearerAuth
        assertNotNull(JsonPath.read(doc, "$.security[0].bearerAuth"));
    }

    @Test
    void swaggerUi_isPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    // ==================== CORS ====================

    @Test
    void cors_preflightFromFrontendOriginIsAllowedForJsonRequestsWithToken() throws Exception {
        mockMvc.perform(options("/v1/transactions")
                        .header("Origin", FRONTEND_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND_ORIGIN))
                // разрешённые заголовки возвращаются так, как их запросил браузер
                .andExpect(header().string("Access-Control-Allow-Headers", "authorization, content-type"));
    }

    @Test
    void cors_allowsPatchAndExposesRetryAfterHeader() throws Exception {
        mockMvc.perform(options("/v1/transactions")
                        .header("Origin", FRONTEND_ORIGIN)
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk());

        // реальный запрос от фронта: браузеру нужно разрешить читать Retry-After при 429
        mockMvc.perform(get("/actuator/health").header("Origin", FRONTEND_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND_ORIGIN))
                .andExpect(header().string("Access-Control-Expose-Headers", "Retry-After"));
    }

    @Test
    void cors_requestFromUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/v1/transactions")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    // ==================== заголовки безопасности ====================

    @Test
    void responses_carryNosniffAndOtherDefaultSecurityHeaders() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"));
    }

    @Test
    void errorResponses_areJsonWithUtf8Charset() throws Exception {
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
