package dev.zhulidov.cash_tracker.app.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.zhulidov.cash_tracker.app.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ИИ-советник целиком: контроллер, безопасность, настоящие PostgreSQL и Redis (кэш и лимит),
 * HTTP-клиент и поддельный сервер GigaChat на локальном порту. В Сбер ничего не уходит.
 * Данные задаются на октябрь и сентябрь 2026 явно, поэтому результат не зависит от текущей даты.
 */
class AiAdvisorIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "secret-password-1";
    private static final String ANSWER = "### 📊 Итог месяца\nПроверочный ответ модели";

    /** Подмена GigaChat: токен и chat/completions, с записью всего, что пришло. */
    private static final FakeGigaChat FAKE = new FakeGigaChat();

    @DynamicPropertySource
    static void aiProperties(DynamicPropertyRegistry registry) {
        registry.add("gigachat.enabled", () -> true);
        registry.add("gigachat.auth-key", () -> "integration-test-key");
        registry.add("gigachat.oauth-url", () -> FAKE.baseUrl() + "/oauth");
        registry.add("gigachat.api-url", () -> FAKE.baseUrl() + "/v1");
        registry.add("gigachat.rate-limit-requests", () -> 3);
        registry.add("gigachat.queue-timeout", () -> "5s");
        // ключи Redis уникальны, чтобы счётчики и кэш не пересекались с другими запусками
        registry.add("gigachat.key-prefix", () -> "ai-it-" + UUID.randomUUID() + ":");
    }

    @AfterAll
    static void stopFake() {
        FAKE.stop();
    }

    @BeforeEach
    void resetFake() {
        FAKE.reset();
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    // ==================== helpers ====================

    private String json(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    private static RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    private static long longAt(String body, String path) {
        return ((Number) JsonPath.read(body, path)).longValue();
    }

    private String register(String email) throws Exception {
        String body = mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "username", "Tester", "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private long category(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/v1/categories").with(bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("categoryName", name))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return longAt(body, "$.id");
    }

    private void expense(String token, long categoryId, String amount, String dateTime) throws Exception {
        mockMvc.perform(post("/v1/transactions").with(bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("description", "Покупка", "amount", new java.math.BigDecimal(amount),
                                "dateTime", dateTime,
                                "splits", List.of(Map.of("categoryId", categoryId, "amount", new java.math.BigDecimal(amount)))))))
                .andExpect(status().isOk());
    }

    private void budget(String token, String amount, String period, Long categoryId) throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("amount", new java.math.BigDecimal(amount));
        body.put("period", period);
        if (categoryId != null) {
            body.put("categoryId", categoryId);
        }
        mockMvc.perform(post("/v1/budgets").with(bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk());
    }

    private ResultActions advice(String token, String yearMonth) throws Exception {
        return mockMvc.perform(post("/v1/ai/advice").with(bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"yearMonth\":\"" + yearMonth + "\"}"));
    }

    /** Пользователь с тратами в октябре и сентябре 2026 и бюджетами на октябрь. */
    private record Setup(String token, String email, long food, long cafe) {
    }

    private Setup userWithOctober() throws Exception {
        String email = "ai-" + UUID.randomUUID() + "@test.com";
        String token = register(email);
        long food = category(token, "Еда");
        long cafe = category(token, "Кафе");
        expense(token, food, "3000.00", "2026-10-05T10:00:00");
        expense(token, cafe, "1200.00", "2026-10-10T20:00:00");
        expense(token, food, "2500.00", "2026-09-15T18:00:00");
        budget(token, "10000.00", "2026-10-01", null);
        budget(token, "4000.00", "2026-10-01", food);
        return new Setup(token, email, food, cafe);
    }

    // ==================== тесты ====================

    @Test
    void firstRequestAsksTheModelAndTheSecondOneComesFromTheCache() throws Exception {
        Setup user = userWithOctober();
        int tokensBefore = FAKE.oauthRequests.size();

        advice(user.token(), "2026-10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(ANSWER))
                .andExpect(jsonPath("$.cached").value(false))
                .andExpect(jsonPath("$.generatedAt").exists());
        advice(user.token(), "2026-10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(ANSWER))
                .andExpect(jsonPath("$.cached").value(true));

        assertEquals(1, FAKE.chatRequests.size(), "платный запрос ушёл ровно один раз");
        // токен клиент хранит между запросами (и между тестами этого класса), поэтому новых не больше одного
        assertTrue(FAKE.oauthRequests.size() - tokensBefore <= 1, "токен не запрашивается заново на каждый вызов");
    }

    @Test
    void theModelReceivesPrecomputedNumbersAndNoPersonalIdentifiers() throws Exception {
        Setup user = userWithOctober();

        advice(user.token(), "2026-10").andExpect(status().isOk());

        FakeGigaChat.Call call = FAKE.chatRequests.get(0);
        assertTrue(call.authorization().startsWith("Bearer it-token-"), call.authorization());
        assertEquals("GigaChat-2-Pro", JsonPath.read(call.body(), "$.model"));
        assertEquals("system", JsonPath.read(call.body(), "$.messages[0].role"));
        assertTrue(((String) JsonPath.read(call.body(), "$.messages[0].content")).contains("Cash Tracker"));
        assertEquals("user", JsonPath.read(call.body(), "$.messages[1].role"));

        String data = JsonPath.read(call.body(), "$.messages[1].content");
        String payload = data.substring(data.indexOf('{'), data.lastIndexOf('}') + 1);
        assertEquals("2026-10", JsonPath.read(payload, "$.month"));
        assertEquals(4200.00, ((Number) JsonPath.read(payload, "$.totalSpent")).doubleValue());
        assertEquals(2500.00, ((Number) JsonPath.read(payload, "$.previousMonthTotalSpent")).doubleValue());
        assertEquals(42.0, ((Number) JsonPath.read(payload, "$.overallBudget.usedPercent")).doubleValue());
        assertEquals("ok", JsonPath.read(payload, "$.overallBudget.status"));
        assertEquals("Еда", JsonPath.read(payload, "$.categories[0].name"));
        assertEquals(75.0, ((Number) JsonPath.read(payload, "$.categories[0].budget.usedPercent")).doubleValue());
        assertEquals("Кафе", JsonPath.read(payload, "$.categories[1].name"));

        String everything = call.body();
        assertFalse(everything.contains(user.email()), "email не должен уходить в модель");
        for (String forbidden : List.of("userId", "budgetId", "categoryId", "\"id\"", "integration-test-key")) {
            assertFalse(everything.contains(forbidden), forbidden + " не должен уходить в модель");
        }
    }

    @Test
    void theAuthorizationKeyGoesOnlyToTheTokenEndpoint() throws Exception {
        Setup user = userWithOctober();

        advice(user.token(), "2026-10").andExpect(status().isOk());

        // токен мог быть получен ещё в одном из прошлых тестов, поэтому смотрим все запросы к /oauth
        assertFalse(FAKE.oauthRequests.isEmpty());
        for (FakeGigaChat.Call oauth : FAKE.oauthRequests) {
            assertEquals("Basic integration-test-key", oauth.authorization());
            assertTrue(oauth.body().contains("scope=GIGACHAT_API_PERS"), oauth.body());
            assertTrue(oauth.rqUid().matches("[0-9a-f-]{36}"), oauth.rqUid());
        }
        assertEquals(1, FAKE.chatRequests.size());
        assertTrue(FAKE.chatRequests.stream().noneMatch(c -> c.authorization().startsWith("Basic")));
    }

    @Test
    void changedDataMeansANewAnalysisAndCachedRepeatsDoNotSpendTheLimit() throws Exception {
        Setup user = userWithOctober();

        advice(user.token(), "2026-10").andExpect(jsonPath("$.cached").value(false));      // 1 из 3
        expense(user.token(), user.cafe(), "300.00", "2026-10-12T12:00:00");
        advice(user.token(), "2026-10").andExpect(jsonPath("$.cached").value(false));      // 2 из 3
        expense(user.token(), user.cafe(), "400.00", "2026-10-13T12:00:00");
        advice(user.token(), "2026-10").andExpect(jsonPath("$.cached").value(false));      // 3 из 3

        // повтор без изменений данных лимит не тратит
        advice(user.token(), "2026-10").andExpect(status().isOk()).andExpect(jsonPath("$.cached").value(true));
        advice(user.token(), "2026-10").andExpect(status().isOk()).andExpect(jsonPath("$.cached").value(true));
        assertEquals(3, FAKE.chatRequests.size());

        // а вот новые данные после исчерпания лимита — уже нет
        expense(user.token(), user.cafe(), "500.00", "2026-10-14T12:00:00");
        String retryAfter = advice(user.token(), "2026-10")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("AI_RATE_LIMIT_EXCEEDED"))
                .andExpect(header().exists("Retry-After"))
                .andReturn().getResponse().getHeader("Retry-After");
        assertTrue(Long.parseLong(retryAfter) > 0);
        assertEquals(3, FAKE.chatRequests.size(), "после отказа модель не вызывалась");
    }

    @Test
    void theLimitIsPerUser() throws Exception {
        Setup first = userWithOctober();
        Setup second = userWithOctober();
        for (int i = 0; i < 3; i++) {
            advice(first.token(), "2026-10").andExpect(status().isOk());
            expense(first.token(), first.cafe(), "100.00", "2026-10-2" + i + "T12:00:00");
        }
        advice(first.token(), "2026-10").andExpect(status().isTooManyRequests());

        advice(second.token(), "2026-10").andExpect(status().isOk()).andExpect(jsonPath("$.cached").value(false));
    }

    @Test
    void aFailureOnTheModelSideDoesNotSpendTheUsersLimit() throws Exception {
        Setup user = userWithOctober();
        FAKE.failWith(500);

        // лимит 3, неудач 5: ни одна не должна превратиться в 429
        for (int i = 0; i < 5; i++) {
            advice(user.token(), "2026-10")
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.errorCode").value("AI_UNAVAILABLE"))
                    .andExpect(jsonPath("$.message").value("AI service is temporarily unavailable"));
        }

        FAKE.failWith(0);
        advice(user.token(), "2026-10").andExpect(status().isOk()).andExpect(jsonPath("$.text").value(ANSWER));
    }

    @Test
    void theModelAnswering402MeansTheTokenQuotaIsExhausted() throws Exception {
        Setup user = userWithOctober();
        FAKE.failWith(402);

        advice(user.token(), "2026-10")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("AI_QUOTA_EXHAUSTED"));
    }

    @Test
    void anExpiredTokenIsRefreshedWithoutTheUserNoticing() throws Exception {
        Setup user = userWithOctober();
        FAKE.rejectNextChatWith401();

        advice(user.token(), "2026-10").andExpect(status().isOk()).andExpect(jsonPath("$.text").value(ANSWER));

        assertEquals(2, FAKE.chatRequests.size(), "после 401 запрос к модели повторён один раз");
        assertNotEquals(FAKE.chatRequests.get(0).authorization(), FAKE.chatRequests.get(1).authorization(),
                "повтор ушёл уже с новым токеном");
    }

    @Test
    void usersNeverSeeEachOthersDataOrCachedAnswers() throws Exception {
        Setup alice = userWithOctober();
        String bobToken = register("bob-" + UUID.randomUUID() + "@test.com");
        long transport = category(bobToken, "Транспорт");
        expense(bobToken, transport, "777.00", "2026-10-03T09:00:00");

        advice(alice.token(), "2026-10").andExpect(status().isOk());
        advice(bobToken, "2026-10").andExpect(status().isOk()).andExpect(jsonPath("$.cached").value(false));

        assertEquals(2, FAKE.chatRequests.size());
        String alicePayload = FAKE.chatRequests.get(0).body();
        String bobPayload = FAKE.chatRequests.get(1).body();
        assertTrue(alicePayload.contains("Еда") && !alicePayload.contains("Транспорт"));
        assertTrue(bobPayload.contains("Транспорт") && !bobPayload.contains("Еда") && !bobPayload.contains("Кафе"));
    }

    @Test
    void aMonthWithoutAnyDataIsAnsweredWithoutPayingForIt() throws Exception {
        String token = register("empty-" + UUID.randomUUID() + "@test.com");
        int tokensBefore = FAKE.oauthRequests.size();

        advice(token, "2026-10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("В этом месяце нет ни трат, ни бюджетов — анализировать пока нечего."))
                .andExpect(jsonPath("$.cached").value(false));

        assertTrue(FAKE.chatRequests.isEmpty());
        assertEquals(tokensBefore, FAKE.oauthRequests.size(), "за пустой месяц не тратится даже получение токена");
    }

    @Test
    void aFutureMonthIsRejected() throws Exception {
        String token = register("future-" + UUID.randomUUID() + "@test.com");

        advice(token, "2999-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

        assertTrue(FAKE.chatRequests.isEmpty());
    }

    @Test
    void theEndpointNeedsALogin() throws Exception {
        mockMvc.perform(post("/v1/ai/advice").contentType(MediaType.APPLICATION_JSON).content("{\"yearMonth\":\"2026-10\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/ai/status")).andExpect(status().isUnauthorized());
        assertTrue(FAKE.chatRequests.isEmpty());
    }

    @Test
    void statusReportsTheAdvisorAsOn() throws Exception {
        String token = register("status-" + UUID.randomUUID() + "@test.com");

        mockMvc.perform(get("/v1/ai/status").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    // ==================== подмена GigaChat ====================

    /** Минимальный сервер на JDK: /oauth выдаёт токен, /v1/chat/completions отвечает заготовленным текстом. */
    static final class FakeGigaChat {

        record Call(String authorization, String rqUid, String body) {
        }

        final List<Call> oauthRequests = new CopyOnWriteArrayList<>();
        final List<Call> chatRequests = new CopyOnWriteArrayList<>();
        private final AtomicInteger failStatus = new AtomicInteger();
        private final AtomicInteger reject401 = new AtomicInteger();
        private final AtomicInteger issuedTokens = new AtomicInteger();
        private final AtomicReference<String> currentToken = new AtomicReference<>("none");
        private final HttpServer server;

        FakeGigaChat() {
            try {
                server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            server.createContext("/oauth", this::oauth);
            server.createContext("/v1/chat/completions", this::chat);
            server.start();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        void stop() {
            server.stop(0);
        }

        /** Журнал /oauth и выданный токен не сбрасываются: клиент приложения держит токен между тестами. */
        void reset() {
            chatRequests.clear();
            failStatus.set(0);
            reject401.set(0);
        }

        void failWith(int status) {
            failStatus.set(status);
        }

        void rejectNextChatWith401() {
            reject401.set(1);
        }

        private void oauth(HttpExchange exchange) throws IOException {
            oauthRequests.add(record(exchange));
            currentToken.set("it-token-" + issuedTokens.incrementAndGet());
            long expiresAt = Instant.now().plus(Duration.ofMinutes(30)).toEpochMilli();
            reply(exchange, 200, "{\"access_token\":\"" + currentToken.get() + "\",\"expires_at\":" + expiresAt + "}");
        }

        private void chat(HttpExchange exchange) throws IOException {
            Call call = record(exchange);
            chatRequests.add(call);
            if (reject401.getAndSet(0) == 1) {
                reply(exchange, 401, "{\"status\":401,\"message\":\"Unauthorized\"}");
                return;
            }
            if (failStatus.get() != 0) {
                reply(exchange, failStatus.get(), "{\"message\":\"simulated failure\"}");
                return;
            }
            if (!("Bearer " + currentToken.get()).equals(call.authorization())) {
                reply(exchange, 401, "{\"status\":401}");
                return;
            }
            String answer = new ObjectMapper().writeValueAsString(ANSWER);
            reply(exchange, 200, "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":" + answer + "},"
                    + "\"index\":0,\"finish_reason\":\"stop\"}],\"usage\":{\"total_tokens\":42}}");
        }

        private static Call record(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            return new Call(exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("RqUID"), body);
        }

        private static void reply(HttpExchange exchange, int status, String json) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }
}
