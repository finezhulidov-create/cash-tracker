package dev.zhulidov.cash_tracker.app.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import dev.zhulidov.cash_tracker.app.AbstractIntegrationTest;
import dev.zhulidov.cash_tracker.transactions.model.Category;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозные сценарии через настоящую цепочку безопасности (JWT), Liquibase, JPA и Redis.
 * База общая для всех тестов и не откатывается, поэтому у каждого теста свои пользователи с уникальным email.
 */
class ApiFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "secret-password-1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CategoryRepository categoryRepository;

    // ==================== helpers ====================

    private String json(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    private RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "username", "Tester", "password", PASSWORD))))
                .andExpect(status().isOk());
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String registerAndLogin() throws Exception {
        String email = uniqueEmail();
        register(email);
        return login(email);
    }

    /** Возвращает {id, userId} созданной категории. */
    private long[] createCategory(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/v1/categories")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("categoryName", name))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        long userId = ((Number) JsonPath.read(body, "$.userId")).longValue();
        return new long[]{id, userId};
    }

    private long createTransaction(String token, long userId, String amount, Map<Long, String> amountByCategoryId) throws Exception {
        List<Map<String, Object>> splits = new ArrayList<>();
        amountByCategoryId.forEach((categoryId, splitAmount) -> {
            Map<String, Object> split = new LinkedHashMap<>();
            split.put("amount", new BigDecimal(splitAmount));
            split.put("categoryDto", Map.of("id", categoryId, "categoryName", "c", "userId", userId));
            split.put("transactionDto", Map.of("id", 1, "userId", userId, "amount", 1,
                    "description", "d", "dateTime", "2026-01-01T00:00:00"));
            splits.add(split);
        });
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("splits", splits);
        request.put("description", "Supermarket");
        request.put("amount", new BigDecimal(amount));

        String body = mockMvc.perform(post("/v1/transactions/create")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    // ==================== auth / user ====================

    @Test
    void registerAndLogin_thenMeShowsProfileWithZeroBalance() throws Exception {
        String email = uniqueEmail();
        register(email);
        String token = login(email);

        mockMvc.perform(get("/v1/users/me").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Tester"))
                .andExpect(jsonPath("$.balance").value(0.0));
    }

    @Test
    void updateBalance_isPersistedAndVisibleInProfile() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/v1/users/balance").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("1500.50"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/users/me").with(bearer(token)))
                .andExpect(jsonPath("$.balance").value(1500.50));
    }

    @Test
    void updateBalance_withNegativeAmount_isRejected() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/v1/users/balance").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_withoutToken_isRejected() throws Exception {
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void protectedEndpoint_withGarbageToken_isRejected() throws Exception {
        mockMvc.perform(get("/v1/users/me").with(bearer("not-a-jwt")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void login_withWrongPassword_isRejected() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "definitely-wrong"))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void deleteUser_removesAccountSoLoginNoLongerWorks() throws Exception {
        String email = uniqueEmail();
        register(email);
        String token = login(email);

        mockMvc.perform(delete("/v1/users/delete").with(bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().is4xxClientError());
    }

    // ==================== categories ====================

    @Test
    void categories_createReadUpdateDelete() throws Exception {
        String token = registerAndLogin();
        long id = createCategory(token, "Food")[0];

        mockMvc.perform(get("/v1/categories/{id}", id).with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Food"));

        mockMvc.perform(put("/v1/categories/{id}", id).with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("categoryName", "Groceries"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Groceries"));

        mockMvc.perform(delete("/v1/categories/{id}", id).with(bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/categories/{id}", id).with(bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherUser_cannotReadOrChangeForeignCategory() throws Exception {
        String owner = registerAndLogin();
        String stranger = registerAndLogin();
        long id = createCategory(owner, "Private")[0];

        mockMvc.perform(get("/v1/categories/{id}", id).with(bearer(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/v1/categories/{id}", id).with(bearer(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/categories/{id}", id).with(bearer(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void categoriesList_isServedFromRedisCacheAndEvictedOnChange() throws Exception {
        String token = registerAndLogin();
        long[] first = createCategory(token, "A");
        long userId = first[1];

        mockMvc.perform(get("/v1/categories/mine").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));

        // запись в обход сервиса: кэш не знает о ней, поэтому список остаётся прежним (cache hit)
        categoryRepository.save(Category.builder().categoryName("DB-only").userId(userId).build());
        mockMvc.perform(get("/v1/categories/mine").with(bearer(token)))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        // создание через сервис инвалидирует кэш, и список перечитывается из БД
        createCategory(token, "B");
        mockMvc.perform(get("/v1/categories/mine").with(bearer(token)))
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    // ==================== transactions + budgets ====================

    @Test
    void transactionWithSplits_isCountedInBudgetStatusOfEachCategory() throws Exception {
        String token = registerAndLogin();
        long[] food = createCategory(token, "Food");
        long[] home = createCategory(token, "Home");
        long userId = food[1];
        YearMonth thisMonth = YearMonth.now();

        String budgetBody = mockMvc.perform(post("/v1/budgets").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 5000, "period", thisMonth.atDay(17).toString(), "categoryId", food[0]))))
                .andExpect(status().isOk())
                // дата в запросе — 17-е число, храниться должно 1-е
                .andExpect(jsonPath("$.period").value(thisMonth.atDay(1).toString()))
                .andReturn().getResponse().getContentAsString();
        long budgetId = ((Number) JsonPath.read(budgetBody, "$.id")).longValue();

        Map<Long, String> splits = new LinkedHashMap<>();
        splits.put(food[0], "700.00");
        splits.put(home[0], "300.00");
        createTransaction(token, userId, "1000.00", splits);

        mockMvc.perform(get("/v1/budgets/{id}/status", budgetId).with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned").value(5000.0))
                .andExpect(jsonPath("$.spent").value(700.0))
                .andExpect(jsonPath("$.available").value(4300.0));
    }

    @Test
    void budgets_listUpdateDuplicateAndDelete() throws Exception {
        String token = registerAndLogin();
        long[] food = createCategory(token, "Food");
        YearMonth thisMonth = YearMonth.now();
        YearMonth nextMonth = thisMonth.plusMonths(1);

        String created = mockMvc.perform(post("/v1/budgets").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 1000, "period", thisMonth.atDay(1).toString(), "categoryId", food[0]))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long budgetId = ((Number) JsonPath.read(created, "$.id")).longValue();

        // список за месяц
        mockMvc.perform(get("/v1/budgets").param("yearMonth", thisMonth.toString()).with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(budgetId));
        mockMvc.perform(get("/v1/budgets").param("yearMonth", nextMonth.toString()).with(bearer(token)))
                .andExpect(jsonPath("$.length()").value(0));

        // второй бюджет на ту же категорию и месяц — конфликт, даже если указан другой день месяца
        mockMvc.perform(post("/v1/budgets").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 10, "period", thisMonth.atDay(20).toString(), "categoryId", food[0]))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_ALREADY_EXISTS"));

        // перенос на следующий месяц: сохраняется первое число, а не 20-е
        mockMvc.perform(put("/v1/budgets/{id}", budgetId).with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("limitAmount", 6000, "period", nextMonth.atDay(20).toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limitAmount").value(6000.0))
                .andExpect(jsonPath("$.period").value(nextMonth.atDay(1).toString()));

        // в новом месяце трат нет, остаток равен лимиту
        mockMvc.perform(get("/v1/budgets/{id}/status", budgetId).with(bearer(token)))
                .andExpect(jsonPath("$.spent").value(0.0))
                .andExpect(jsonPath("$.available").value(6000.0));

        mockMvc.perform(delete("/v1/budgets/{id}", budgetId).with(bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v1/budgets/{id}", budgetId).with(bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void budget_ofAnotherUser_isNotAccessible() throws Exception {
        String owner = registerAndLogin();
        String stranger = registerAndLogin();
        long[] food = createCategory(owner, "Food");

        String created = mockMvc.perform(post("/v1/budgets").with(bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 1000, "period", LocalDate.now().toString(), "categoryId", food[0]))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long budgetId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(get("/v1/budgets/{id}", budgetId).with(bearer(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/budgets/{id}/status", budgetId).with(bearer(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/v1/budgets/{id}", budgetId).with(bearer(stranger)))
                .andExpect(status().isNotFound());
        // и создать бюджет на чужую категорию тоже нельзя
        mockMvc.perform(post("/v1/budgets").with(bearer(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 1, "period", LocalDate.now().toString(), "categoryId", food[0]))))
                .andExpect(status().isNotFound());
    }
}
