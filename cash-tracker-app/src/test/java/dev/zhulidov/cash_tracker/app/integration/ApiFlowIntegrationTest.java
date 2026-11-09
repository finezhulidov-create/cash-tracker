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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозные сценарии через настоящую цепочку безопасности (JWT), Liquibase, JPA и Redis.
 * База общая для всех тестов и не откатывается, поэтому у каждого теста свои пользователи с уникальным email.
 * Даты трат задаются явно (октябрь 2026), поэтому результаты не зависят от текущей даты.
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

    private static long longAt(String body, String path) {
        return ((Number) JsonPath.read(body, path)).longValue();
    }

    /** Регистрирует пользователя и возвращает токен из ответа регистрации. */
    private String register(String email) throws Exception {
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "username", "Tester", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
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
        return new long[]{longAt(body, "$.id"), longAt(body, "$.userId")};
    }

    private static Map<String, Object> part(long categoryId, String amount) {
        Map<String, Object> split = new LinkedHashMap<>();
        split.put("categoryId", categoryId);
        split.put("amount", new java.math.BigDecimal(amount));
        return split;
    }

    @SafeVarargs
    private Map<String, Object> transactionBody(String amount, String dateTime, Map<String, Object>... parts) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("description", "Supermarket");
        request.put("amount", new java.math.BigDecimal(amount));
        if (dateTime != null) {
            request.put("dateTime", dateTime);
        }
        request.put("splits", List.of(parts));
        return request;
    }

    @SafeVarargs
    private long createTransaction(String token, String amount, String dateTime, Map<String, Object>... parts) throws Exception {
        String body = mockMvc.perform(post("/v1/transactions")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(transactionBody(amount, dateTime, parts))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return longAt(body, "$.id");
    }

    private long createBudget(String token, Object amount, String period, Long categoryId) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("amount", amount);
        request.put("period", period);
        if (categoryId != null) {
            request.put("categoryId", categoryId);
        }
        String body = mockMvc.perform(post("/v1/budgets").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return longAt(body, "$.id");
    }

    // ==================== регистрация и вход ====================

    @Test
    void register_returnsUsableTokenImmediatelyWithoutSeparateLogin() throws Exception {
        String email = uniqueEmail();
        String token = register(email);

        mockMvc.perform(get("/v1/users/me").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Tester"))
                .andExpect(jsonPath("$.balance").value(0.0));
    }

    @Test
    void register_withAlreadyUsedEmail_returns409EvenIfCaseDiffers() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email.toUpperCase(), "username", "Other", "password", PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void register_storesEmailInLowerCaseSoLoginWorksWithAnyCase() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email.toUpperCase(), "username", "Tester", "password", PASSWORD))))
                .andExpect(status().isOk());

        String token = login(email);
        mockMvc.perform(get("/v1/users/me").with(bearer(token)))
                .andExpect(jsonPath("$.email").value(email.toLowerCase()));
    }

    @Test
    void register_withTooShortPassword_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", uniqueEmail(), "username", "Tester", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void login_withWrongPassword_returns401WithInvalidCredentialsCode() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "definitely-wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_withUnknownEmail_isIndistinguishableFromWrongPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", uniqueEmail(), "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpoint_withoutToken_returns401WithJsonError() throws Exception {
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void protectedEndpoint_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/users/me").with(bearer("not-a-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void deleteUser_removesAccountSoOldTokenAndLoginNoLongerWork() throws Exception {
        String email = uniqueEmail();
        register(email);
        String token = login(email);

        mockMvc.perform(delete("/v1/users/delete").with(bearer(token)))
                .andExpect(status().isOk());

        // регрессия: токен удалённого пользователя раньше давал 500
        mockMvc.perform(get("/v1/users/me").with(bearer(token)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUser_removesAllHisDataIncludingCategoriesWithTransactions() throws Exception {
        String token = registerAndLogin();
        long[] food = createCategory(token, "Food");
        createTransaction(token, "100.00", "2026-10-05T10:00:00", part(food[0], "100.00"));
        createBudget(token, 500, "2026-10-01", food[0]);

        // каскад по пользователю должен пройти, несмотря на защиту категорий с тратами
        mockMvc.perform(delete("/v1/users/delete").with(bearer(token)))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertTrue(categoryRepository.findAllByUserId(food[1]).isEmpty());
    }

    // ==================== баланс ====================

    @Test
    void updateBalance_isPersistedAndVisibleInProfile() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(put("/v1/users/me/balance").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"balance\": 1500.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500.50));

        mockMvc.perform(get("/v1/users/me").with(bearer(token)))
                .andExpect(jsonPath("$.balance").value(1500.50));
    }

    @Test
    void updateBalance_withNegativeAmount_returns400WithFieldError() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(put("/v1/users/me/balance").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"balance\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.balance").exists());
    }

    @Test
    void balance_isManualAndDoesNotChangeWhenTransactionsAreCreated() throws Exception {
        String token = registerAndLogin();
        long[] food = createCategory(token, "Food");
        mockMvc.perform(put("/v1/users/me/balance").with(bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"balance\": 1000}"));

        createTransaction(token, "400.00", "2026-10-05T10:00:00", part(food[0], "400.00"));

        mockMvc.perform(get("/v1/users/me").with(bearer(token)))
                .andExpect(jsonPath("$.balance").value(1000.0));
    }

    // ==================== формат ошибок ====================

    @Test
    void unknownPath_returns404InErrorFormat() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/does-not-exist").with(bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void unsupportedMethod_returns405InErrorFormat() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(patch("/v1/users/me").with(bearer(token)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void malformedJson_returns400InErrorFormat() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/v1/categories").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    @Test
    void validationErrors_listEveryInvalidFieldAtOnce() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/v1/transactions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.splits").exists());
    }

    // ==================== категории ====================

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
    void deleteCategory_withTransactions_returns409UntilTransactionsAreRemoved() throws Exception {
        String token = registerAndLogin();
        long categoryId = createCategory(token, "Food")[0];
        long transactionId = createTransaction(token, "100.00", "2026-10-05T10:00:00", part(categoryId, "100.00"));

        mockMvc.perform(delete("/v1/categories/{id}", categoryId).with(bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_IN_USE"));
        // категория и трата на месте
        mockMvc.perform(get("/v1/categories/{id}", categoryId).with(bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v1/transactions/{id}", transactionId).with(bearer(token)))
                .andExpect(jsonPath("$.splits.length()").value(1));

        mockMvc.perform(delete("/v1/transactions/{id}", transactionId).with(bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/v1/categories/{id}", categoryId).with(bearer(token)))
                .andExpect(status().isOk());
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

    @Test
    void categorySplits_listsPartsWithTransactionIdAndCategoryAndRefreshesAfterNewTransaction() throws Exception {
        String token = registerAndLogin();
        long categoryId = createCategory(token, "Food")[0];
        long first = createTransaction(token, "100.00", "2026-10-05T10:00:00", part(categoryId, "100.00"));

        mockMvc.perform(get("/v1/categories/{id}/splits", categoryId).with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].transactionId").value(first))
                .andExpect(jsonPath("$.content[0].amount").value(100.0))
                .andExpect(jsonPath("$.content[0].category.categoryName").value("Food"));

        // вторая трата сбрасывает кэш сплитов этой категории, и список обновляется
        createTransaction(token, "50.00", "2026-10-06T10:00:00", part(categoryId, "50.00"));
        mockMvc.perform(get("/v1/categories/{id}/splits", categoryId).with(bearer(token)))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    // ==================== транзакции ====================

    @Test
    void createTransaction_returnsSplitsWithIdsAndCategories() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Food")[0];
        long home = createCategory(token, "Home")[0];

        mockMvc.perform(post("/v1/transactions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(transactionBody("1000.00", "2026-10-05T14:30:00",
                                part(food, "700.00"), part(home, "300.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount").value(1000.0))
                .andExpect(jsonPath("$.dateTime").value("2026-10-05T14:30:00"))
                .andExpect(jsonPath("$.splits.length()").value(2))
                .andExpect(jsonPath("$.splits[0].id").isNumber())
                .andExpect(jsonPath("$.splits[0].category.categoryName").value("Food"))
                .andExpect(jsonPath("$.splits[1].category.categoryName").value("Home"));
    }

    @Test
    void createTransaction_withoutDate_usesCurrentServerTimeInUtc() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Food")[0];
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1);

        String body = mockMvc.perform(post("/v1/transactions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(transactionBody("10.00", null, part(food, "10.00")))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        LocalDateTime created = LocalDateTime.parse(JsonPath.read(body, "$.dateTime"));
        assertTrue(created.isAfter(before) && created.isBefore(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1)),
                "dateTime was " + created);
    }

    @Test
    void createTransaction_whenSplitsDoNotSumToAmount_returns400AndSavesNothing() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Food")[0];

        mockMvc.perform(post("/v1/transactions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(transactionBody("1000.00", "2026-10-05T10:00:00", part(food, "999.00")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("NOT_VALID_TRANSACTION_SPLIT_SUM"));

        mockMvc.perform(get("/v1/transactions").with(bearer(token)))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void transactions_getUpdateAndDelete() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Food")[0];
        long home = createCategory(token, "Home")[0];
        long id = createTransaction(token, "1000.00", "2026-10-05T10:00:00", part(food, "1000.00"));

        mockMvc.perform(get("/v1/transactions/{id}", id).with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.splits.length()").value(1));

        // полная замена: новая сумма, дата и разбивка по другим категориям
        Map<String, Object> update = transactionBody("600.00", "2026-10-07T09:00:00",
                part(home, "400.00"), part(food, "200.00"));
        update.put("description", "Updated");
        mockMvc.perform(put("/v1/transactions/{id}", id).with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"))
                .andExpect(jsonPath("$.amount").value(600.0))
                .andExpect(jsonPath("$.dateTime").value("2026-10-07T09:00:00"))
                .andExpect(jsonPath("$.splits.length()").value(2))
                .andExpect(jsonPath("$.splits[0].id").isNumber());

        mockMvc.perform(get("/v1/transactions/{id}", id).with(bearer(token)))
                .andExpect(jsonPath("$.splits.length()").value(2));

        mockMvc.perform(delete("/v1/transactions/{id}", id).with(bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v1/transactions/{id}", id).with(bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void transactionsList_filtersSortsAndPaginates() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Food")[0];
        long home = createCategory(token, "Home")[0];
        createTransaction(token, "1000.00", "2026-10-05T10:00:00", part(food, "700.00"), part(home, "300.00"));
        createTransaction(token, "500.00", "2026-10-20T10:00:00", part(food, "500.00"));
        createTransaction(token, "200.00", "2026-11-03T10:00:00", part(home, "200.00"));
        createTransaction(token, "50.00", "2027-01-10T10:00:00", part(food, "50.00"));

        // по умолчанию: от новых к старым
        mockMvc.perform(get("/v1/transactions").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].dateTime").value("2027-01-10T10:00:00"))
                .andExpect(jsonPath("$.content[3].dateTime").value("2026-10-05T10:00:00"));

        // пагинация
        mockMvc.perform(get("/v1/transactions").param("size", "2").param("page", "1").with(bearer(token)))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].dateTime").value("2026-10-20T10:00:00"))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        // сортировка по сумме
        mockMvc.perform(get("/v1/transactions").param("sort", "amount,asc").with(bearer(token)))
                .andExpect(jsonPath("$.content[0].amount").value(50.0));

        // по категории: транзакция с двумя частями в разных категориях попадает в обе, но по одному разу
        mockMvc.perform(get("/v1/transactions").param("categoryId", String.valueOf(home)).with(bearer(token)))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mockMvc.perform(get("/v1/transactions").param("categoryId", String.valueOf(food)).with(bearer(token)))
                .andExpect(jsonPath("$.page.totalElements").value(3));

        // по сумме
        mockMvc.perform(get("/v1/transactions").param("minAmount", "400").param("maxAmount", "900").with(bearer(token)))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(500.0));

        // по дате: обе границы и открытый диапазон
        mockMvc.perform(get("/v1/transactions")
                        .param("from", "2026-10-01T00:00:00").param("to", "2026-10-31T23:59:59").with(bearer(token)))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mockMvc.perform(get("/v1/transactions").param("from", "2026-11-01T00:00:00").with(bearer(token)))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mockMvc.perform(get("/v1/transactions").param("to", "2026-10-05T10:00:00").with(bearer(token)))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        // сумма за период
        mockMvc.perform(get("/v1/transactions/total")
                        .param("from", "2026-10-01T00:00:00").param("to", "2026-10-31T23:59:59").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1500.0));
    }

    @Test
    void transactionsList_searchesByDescriptionIgnoringCaseAndOnlyAmongOwnTransactions() throws Exception {
        String token = registerAndLogin();
        String stranger = registerAndLogin();
        long food = createCategory(token, "Food")[0];
        long foreign = createCategory(stranger, "Food")[0];
        createTransactionWithDescription(token, "Супермаркет Лента", "300.00", food);
        createTransactionWithDescription(token, "Кофе", "150.00", food);
        createTransactionWithDescription(stranger, "Супермаркет чужой", "10.00", foreign);

        mockMvc.perform(get("/v1/transactions").param("q", "СУПЕРМАРКЕТ").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Супермаркет Лента"));

        // поиск сочетается с остальными фильтрами
        mockMvc.perform(get("/v1/transactions").param("q", "е").param("minAmount", "200").with(bearer(token)))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Супермаркет Лента"));

        mockMvc.perform(get("/v1/transactions").param("q", "ничего такого").with(bearer(token)))
                .andExpect(jsonPath("$.page.totalElements").value(0));

        mockMvc.perform(get("/v1/transactions").param("q", "x".repeat(256)).with(bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    private void createTransactionWithDescription(String token, String description, String amount, long categoryId)
            throws Exception {
        var body = transactionBody(amount, "2026-10-05T10:00:00", part(categoryId, amount));
        body.put("description", description);
        mockMvc.perform(post("/v1/transactions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isOk());
    }

    @Test
    void transactionsList_withUnknownSortProperty_returns400InsteadOf500() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/transactions").param("sort", "nonsense,asc").with(bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SORT"));
    }

    @Test
    void transactionsList_withInvertedDateRange_returns400() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/transactions")
                        .param("from", "2026-10-31T00:00:00").param("to", "2026-10-01T00:00:00").with(bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void transactions_ofAnotherUserAreInvisibleAndImmutable() throws Exception {
        String owner = registerAndLogin();
        String stranger = registerAndLogin();
        long ownerCategory = createCategory(owner, "Food")[0];
        long strangerCategory = createCategory(stranger, "Mine")[0];
        long id = createTransaction(owner, "100.00", "2026-10-05T10:00:00", part(ownerCategory, "100.00"));

        mockMvc.perform(get("/v1/transactions/{id}", id).with(bearer(stranger))).andExpect(status().isNotFound());
        mockMvc.perform(delete("/v1/transactions/{id}", id).with(bearer(stranger))).andExpect(status().isNotFound());
        mockMvc.perform(put("/v1/transactions/{id}", id).with(bearer(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(transactionBody("100.00", "2026-10-05T10:00:00", part(strangerCategory, "100.00")))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/transactions").with(bearer(stranger)))
                .andExpect(jsonPath("$.page.totalElements").value(0));

        // и разбить свою трату по чужой категории тоже нельзя
        mockMvc.perform(post("/v1/transactions").with(bearer(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(transactionBody("100.00", "2026-10-05T10:00:00", part(ownerCategory, "100.00")))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v1/transactions/{id}", id).with(bearer(owner)))
                .andExpect(status().isOk());
    }

    // ==================== бюджеты ====================

    @Test
    void transactionWithSplits_isCountedInBudgetStatusOfEachCategory() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Food")[0];
        long home = createCategory(token, "Home")[0];

        // дата в запросе — 17-е число, храниться должно 1-е
        String budgetBody = mockMvc.perform(post("/v1/budgets").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 5000, "period", "2026-10-17", "categoryId", food))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("2026-10-01"))
                .andReturn().getResponse().getContentAsString();
        long budgetId = longAt(budgetBody, "$.id");

        createTransaction(token, "1000.00", "2026-10-05T10:00:00", part(food, "700.00"), part(home, "300.00"));

        mockMvc.perform(get("/v1/budgets/{id}/status", budgetId).with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned").value(5000.0))
                .andExpect(jsonPath("$.spent").value(700.0))
                .andExpect(jsonPath("$.available").value(4300.0));
    }

    @Test
    void budgetStatus_countsOnlyTheBudgetMonth() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Food")[0];
        long budgetId = createBudget(token, 1000, "2026-10-01", food);
        createTransaction(token, "100.00", "2026-10-01T00:00:00", part(food, "100.00"));   // начало месяца: входит
        createTransaction(token, "200.00", "2026-10-31T23:59:59", part(food, "200.00"));   // конец месяца: входит
        createTransaction(token, "999.00", "2026-11-01T00:00:00", part(food, "999.00"));   // следующий месяц: нет
        createTransaction(token, "888.00", "2026-09-30T23:59:59", part(food, "888.00"));   // прошлый месяц: нет

        mockMvc.perform(get("/v1/budgets/{id}/status", budgetId).with(bearer(token)))
                .andExpect(jsonPath("$.spent").value(300.0))
                .andExpect(jsonPath("$.available").value(700.0));
    }

    @Test
    void overallBudget_countsAllCategoriesOfTheMonth() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Food")[0];
        long home = createCategory(token, "Home")[0];
        long overallId = createBudget(token, 3000, "2026-10-12", null);
        createTransaction(token, "1000.00", "2026-10-05T10:00:00", part(food, "700.00"), part(home, "300.00"));
        createTransaction(token, "500.00", "2026-10-20T10:00:00", part(home, "500.00"));
        createTransaction(token, "777.00", "2026-11-02T10:00:00", part(food, "777.00"));

        mockMvc.perform(get("/v1/budgets/{id}", overallId).with(bearer(token)))
                .andExpect(jsonPath("$.category").doesNotExist())
                .andExpect(jsonPath("$.period").value("2026-10-01"));
        mockMvc.perform(get("/v1/budgets/{id}/status", overallId).with(bearer(token)))
                .andExpect(jsonPath("$.spent").value(1500.0))
                .andExpect(jsonPath("$.available").value(1500.0));

        // второй общий бюджет на тот же месяц — конфликт
        mockMvc.perform(post("/v1/budgets").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 10, "period", "2026-10-30"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_ALREADY_EXISTS"));
        // а на другой месяц — можно
        createBudget(token, 10, "2026-11-01", null);
    }

    @Test
    void budgets_listUpdateDuplicateAndDelete() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Food")[0];

        long budgetId = createBudget(token, 1000, "2026-10-01", food);

        mockMvc.perform(get("/v1/budgets").param("yearMonth", "2026-10").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(budgetId));
        mockMvc.perform(get("/v1/budgets").param("yearMonth", "2026-11").with(bearer(token)))
                .andExpect(jsonPath("$.length()").value(0));

        // второй бюджет на ту же категорию и месяц — конфликт, даже если указан другой день месяца
        mockMvc.perform(post("/v1/budgets").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 10, "period", "2026-10-20", "categoryId", food))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_ALREADY_EXISTS"));

        // перенос на следующий месяц: сохраняется первое число, а не 20-е
        mockMvc.perform(put("/v1/budgets/{id}", budgetId).with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("limitAmount", 6000, "period", "2026-11-20"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limitAmount").value(6000.0))
                .andExpect(jsonPath("$.period").value("2026-11-01"));

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
        long food = createCategory(owner, "Food")[0];
        long budgetId = createBudget(owner, 1000, "2026-10-01", food);

        mockMvc.perform(get("/v1/budgets/{id}", budgetId).with(bearer(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/budgets/{id}/status", budgetId).with(bearer(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/v1/budgets/{id}", budgetId).with(bearer(stranger)))
                .andExpect(status().isNotFound());
        // и создать бюджет на чужую категорию тоже нельзя
        mockMvc.perform(post("/v1/budgets").with(bearer(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("amount", 1, "period", "2026-10-01", "categoryId", food))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/budgets").param("yearMonth", "2026-10").with(bearer(stranger)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== аналитика ====================

    private void createAnalyticsScenario(String token, long food, long home) throws Exception {
        createTransaction(token, "1000.00", "2026-10-05T10:00:00", part(food, "700.00"), part(home, "300.00"));
        createTransaction(token, "500.00", "2026-10-20T10:00:00", part(food, "500.00"));
        createTransaction(token, "200.00", "2026-11-03T10:00:00", part(home, "200.00"));
        createTransaction(token, "50.00", "2027-01-10T10:00:00", part(food, "50.00"));
    }

    @Test
    void analytics_byCategory_sumsPartsPerCategoryLargestFirstWithInclusiveDates() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Food")[0];
        long home = createCategory(token, "Home")[0];
        createAnalyticsScenario(token, food, home);

        mockMvc.perform(get("/v1/analytics/by-category")
                        .param("from", "2026-10-01").param("to", "2026-10-31").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].categoryName").value("Food"))
                .andExpect(jsonPath("$[0].spent").value(1200.0))
                .andExpect(jsonPath("$[1].categoryName").value("Home"))
                .andExpect(jsonPath("$[1].spent").value(300.0));

        // один день: конечная дата входит целиком
        mockMvc.perform(get("/v1/analytics/by-category")
                        .param("from", "2026-10-05").param("to", "2026-10-05").with(bearer(token)))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].spent").value(700.0));

        mockMvc.perform(get("/v1/analytics/by-category")
                        .param("from", "2025-01-01").param("to", "2025-12-31").with(bearer(token)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void analytics_monthly_returnsTwelveMonthsOfTheRequestedYearOnly() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Food")[0];
        long home = createCategory(token, "Home")[0];
        createAnalyticsScenario(token, food, home);

        mockMvc.perform(get("/v1/analytics/monthly").param("year", "2026").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].month").value("2026-01"))
                .andExpect(jsonPath("$[0].spent").value(0.0))
                .andExpect(jsonPath("$[9].month").value("2026-10"))
                .andExpect(jsonPath("$[9].spent").value(1500.0))
                .andExpect(jsonPath("$[10].spent").value(200.0))
                .andExpect(jsonPath("$[11].spent").value(0.0));

        mockMvc.perform(get("/v1/analytics/monthly").param("year", "2027").with(bearer(token)))
                .andExpect(jsonPath("$[0].spent").value(50.0));
    }

    @Test
    void analytics_summary_combinesOverallAndCategoryBudgetsWithTotals() throws Exception {
        String token = registerAndLogin();
        long food = createCategory(token, "Food")[0];
        long home = createCategory(token, "Home")[0];
        createAnalyticsScenario(token, food, home);
        createBudget(token, 3000, "2026-10-01", null);
        createBudget(token, 250, "2026-10-01", home);
        createBudget(token, 2000, "2026-10-01", food);

        mockMvc.perform(get("/v1/analytics/summary").param("yearMonth", "2026-10").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMonth").value("2026-10"))
                .andExpect(jsonPath("$.totalSpent").value(1500.0))
                .andExpect(jsonPath("$.overall.assigned").value(3000.0))
                .andExpect(jsonPath("$.overall.available").value(1500.0))
                .andExpect(jsonPath("$.categories.length()").value(2))
                .andExpect(jsonPath("$.categories[0].category.categoryName").value("Food"))
                .andExpect(jsonPath("$.categories[0].spent").value(1200.0))
                .andExpect(jsonPath("$.categories[0].available").value(800.0))
                // Home перерасходована: лимит 250, потрачено 300
                .andExpect(jsonPath("$.categories[1].category.categoryName").value("Home"))
                .andExpect(jsonPath("$.categories[1].available").value(-50.0));
    }

    @Test
    void analytics_neverMixesDataOfDifferentUsers() throws Exception {
        String owner = registerAndLogin();
        String stranger = registerAndLogin();
        long food = createCategory(owner, "Food")[0];
        long home = createCategory(owner, "Home")[0];
        createAnalyticsScenario(owner, food, home);

        mockMvc.perform(get("/v1/analytics/by-category")
                        .param("from", "2026-01-01").param("to", "2027-12-31").with(bearer(stranger)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/v1/analytics/summary").param("yearMonth", "2026-10").with(bearer(stranger)))
                .andExpect(jsonPath("$.totalSpent").value(0.0))
                .andExpect(jsonPath("$.categories.length()").value(0));
    }

    @Test
    void analytics_withInvalidParameters_return400() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/v1/analytics/by-category")
                        .param("from", "2026-10-31").param("to", "2026-10-01").with(bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
        mockMvc.perform(get("/v1/analytics/monthly").param("year", "1900").with(bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/analytics/summary").param("yearMonth", "october").with(bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"));
    }
}
