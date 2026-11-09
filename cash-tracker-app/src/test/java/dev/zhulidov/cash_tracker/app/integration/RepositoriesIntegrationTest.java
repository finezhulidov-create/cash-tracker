package dev.zhulidov.cash_tracker.app.integration;

import dev.zhulidov.cash_tracker.app.AbstractIntegrationTest;
import dev.zhulidov.cash_tracker.app.model.User;
import dev.zhulidov.cash_tracker.app.repository.UserRepository;
import dev.zhulidov.cash_tracker.transactions.model.Budget;
import dev.zhulidov.cash_tracker.transactions.model.Category;
import dev.zhulidov.cash_tracker.transactions.model.Transaction;
import dev.zhulidov.cash_tracker.transactions.model.TransactionSplit;
import dev.zhulidov.cash_tracker.transactions.repository.BudgetRepository;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionSplitRepository;
import dev.zhulidov.cash_tracker.transactions.util.TransactionSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Запросы на настоящем PostgreSQL с миграциями Liquibase. Каждый тест откатывается.
 */
@Transactional
class RepositoriesIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate OCTOBER = LocalDate.of(2026, 10, 1);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransactionSplitRepository splitRepository;
    @Autowired
    private BudgetRepository budgetRepository;
    @PersistenceContext
    private EntityManager em;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private record Split(Category category, String amount) {
    }

    // ==================== helpers ====================

    private User user() {
        return userRepository.saveAndFlush(User.builder()
                .email("user-" + UUID.randomUUID() + "@test.com").password("hash").build());
    }

    private Category category(User owner, String name) {
        return categoryRepository.saveAndFlush(Category.builder().categoryName(name).userId(owner.getId()).build());
    }

    private Transaction transaction(User owner, LocalDateTime when, Split... splits) {
        return transaction(owner, when, "tx", splits);
    }

    private Transaction transaction(User owner, LocalDateTime when, String description, Split... splits) {
        BigDecimal total = Arrays.stream(splits).map(s -> new BigDecimal(s.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var tx = Transaction.builder().userId(owner.getId()).amount(total).description(description).dateTime(when).build();
        tx.setSplits(new ArrayList<>(Arrays.stream(splits).map(s -> TransactionSplit.builder()
                .transaction(tx).category(s.category()).amount(new BigDecimal(s.amount())).build()).toList()));
        return transactionRepository.saveAndFlush(tx);
    }

    private Budget budget(User owner, Category category, LocalDate period, String limit) {
        return budgetRepository.saveAndFlush(Budget.builder().userId(owner.getId()).category(category)
                .period(period).limitAmount(new BigDecimal(limit)).build());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    // ==================== users ====================

    @Test
    void user_isCreatedWithZeroBalanceAndFoundByEmail() {
        var saved = user();
        em.clear();

        // поиск не зависит от регистра: так пользователь войдёт, даже если введёт email заглавными буквами
        var loaded = userRepository.findByEmailIgnoreCase(saved.getEmail().toUpperCase()).orElseThrow();

        assertEquals(saved.getId(), loaded.getId());
        assertAmount("0", loaded.getBalance());
        assertTrue(userRepository.existsByEmailIgnoreCase(saved.getEmail().toUpperCase()));
        assertFalse(userRepository.existsByEmailIgnoreCase("nobody-" + UUID.randomUUID() + "@test.com"));
    }

    @Test
    void user_withDuplicateEmail_isRejectedByUniqueConstraint() {
        var first = user();

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(
                User.builder().email(first.getEmail()).password("other").build()));
    }

    // ==================== categories ====================

    @Test
    void categories_areFoundOnlyForTheirOwner() {
        var owner = user();
        var other = user();
        category(owner, "Food");
        category(owner, "Home");
        category(other, "Secret");

        assertEquals(2, categoryRepository.findAllByUserId(owner.getId()).size());
        Page<Category> page = categoryRepository.findAllByUserId(owner.getId(), PageRequest.of(0, 1));
        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalElements());
    }

    // ==================== sum of splits (budget "spent") ====================

    @Test
    void sumOfSplits_countsOnlyMatchingCategoryUserAndMonthWithHalfOpenInterval() {
        var owner = user();
        var stranger = user();
        var food = category(owner, "Food");
        var home = category(owner, "Home");
        var foreign = category(stranger, "Foreign");

        transaction(owner, LocalDateTime.of(2026, 10, 1, 0, 0, 0), new Split(food, "100.00"));   // начало месяца: входит
        transaction(owner, LocalDateTime.of(2026, 10, 31, 23, 59, 59),
                new Split(food, "50.00"), new Split(home, "25.00"));                                  // конец месяца: входит
        transaction(owner, LocalDateTime.of(2026, 11, 1, 0, 0, 0), new Split(food, "1000.00")); // следующий месяц: не входит
        transaction(owner, LocalDateTime.of(2026, 9, 30, 23, 59, 59), new Split(food, "999.00")); // прошлый месяц: не входит
        transaction(stranger, LocalDateTime.of(2026, 10, 15, 12, 0), new Split(foreign, "70.00"));

        var from = LocalDateTime.of(2026, 10, 1, 0, 0);
        var to = LocalDateTime.of(2026, 11, 1, 0, 0);

        assertAmount("150.00", splitRepository.sumAmountByCategoryAndUserAndDateRange(food.getId(), owner.getId(), from, to));
        assertAmount("25.00", splitRepository.sumAmountByCategoryAndUserAndDateRange(home.getId(), owner.getId(), from, to));
        // чужая категория не учитывается для другого пользователя, а при отсутствии строк COALESCE даёт 0
        assertAmount("0", splitRepository.sumAmountByCategoryAndUserAndDateRange(foreign.getId(), owner.getId(), from, to));
    }

    @Test
    void sumOfTransactions_countsOnlyUserTransactionsWithinRange() {
        var owner = user();
        var stranger = user();
        var food = category(owner, "Food");
        var foreign = category(stranger, "Foreign");
        transaction(owner, LocalDateTime.of(2026, 10, 5, 10, 0), new Split(food, "40.00"));
        transaction(owner, LocalDateTime.of(2026, 10, 6, 10, 0), new Split(food, "60.00"));
        transaction(owner, LocalDateTime.of(2026, 12, 6, 10, 0), new Split(food, "500.00"));
        transaction(stranger, LocalDateTime.of(2026, 10, 5, 10, 0), new Split(foreign, "7.00"));

        assertAmount("100.00", transactionRepository.sumAmountByUserIdAndDateTimeBetween(
                owner.getId(), LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 31, 23, 59)));
        assertAmount("0", transactionRepository.sumAmountByUserIdAndDateTimeBetween(
                owner.getId(), LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 31, 0, 0)));
    }

    // ==================== TransactionSpecifications ====================

    private void prepareTransactionsForFilters(User owner, User stranger, Category a, Category b, Category c) {
        transaction(owner, LocalDateTime.of(2026, 10, 5, 10, 0), new Split(a, "30.00"), new Split(a, "20.00")); // 50, два сплита в A
        transaction(owner, LocalDateTime.of(2026, 10, 10, 10, 0), new Split(b, "200.00"));
        transaction(owner, LocalDateTime.of(2026, 11, 5, 10, 0), new Split(a, "500.00"));
        transaction(stranger, LocalDateTime.of(2026, 10, 5, 10, 0), new Split(c, "70.00"));
    }

    private long count(Specification<Transaction> spec) {
        return transactionRepository.findAll(spec, PageRequest.of(0, 50)).getTotalElements();
    }

    @Test
    void specifications_filterByUserCategoryAmountAndDate() {
        var owner = user();
        var stranger = user();
        var a = category(owner, "A");
        var b = category(owner, "B");
        var c = category(stranger, "C");
        prepareTransactionsForFilters(owner, stranger, a, b, c);
        var byOwner = TransactionSpecifications.hasUserId(owner.getId());

        assertEquals(3, count(byOwner));

        // категория: транзакция с двумя сплитами в A возвращается один раз (distinct)
        Page<Transaction> inA = transactionRepository.findAll(
                byOwner.and(TransactionSpecifications.hasCategoryId(a.getId())), PageRequest.of(0, 50));
        assertEquals(2, inA.getTotalElements());
        assertEquals(2, inA.getContent().size());

        // сумма: только нижняя, только верхняя и обе границы
        assertEquals(2, count(byOwner.and(TransactionSpecifications.amountBetween(new BigDecimal("100"), null))));
        assertEquals(1, count(byOwner.and(TransactionSpecifications.amountBetween(null, new BigDecimal("100")))));
        assertEquals(2, count(byOwner.and(TransactionSpecifications.amountBetween(new BigDecimal("40"), new BigDecimal("250")))));

        // дата
        var october = TransactionSpecifications.dateBetween(
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 31, 23, 59));
        assertEquals(2, count(byOwner.and(october)));

        // комбинация фильтров и «пустые» фильтры (null) не мешают
        assertEquals(1, count(byOwner.and(TransactionSpecifications.hasCategoryId(a.getId())).and(october)));
        assertEquals(3, count(byOwner.and(TransactionSpecifications.hasCategoryId(null))
                .and(TransactionSpecifications.amountBetween(null, null))
                .and(TransactionSpecifications.dateBetween(null, null))));
    }

    @Test
    void specifications_filterByDescriptionIgnoringCaseAndTreatingLikeWildcardsAsText() {
        var owner = user();
        var stranger = user();
        var a = category(owner, "A");
        var c = category(stranger, "C");
        var when = LocalDateTime.of(2026, 10, 5, 10, 0);
        transaction(owner, when, "Супермаркет Лента", new Split(a, "10.00"));
        transaction(owner, when, "МАГАЗИН у дома", new Split(a, "20.00"));
        transaction(owner, when, "Скидка 50% на всё", new Split(a, "30.00"));
        transaction(owner, when, "file_name", new Split(a, "40.00"));
        transaction(owner, when, "fileXname", new Split(a, "50.00"));
        transaction(stranger, when, "Супермаркет чужой", new Split(c, "60.00"));
        var byOwner = TransactionSpecifications.hasUserId(owner.getId());

        // регистр не важен, в том числе для кириллицы; подстрока ищется в любом месте
        assertEquals(1, count(byOwner.and(TransactionSpecifications.descriptionContains("супермаркет"))));
        assertEquals(1, count(byOwner.and(TransactionSpecifications.descriptionContains("МАГАЗИН"))));
        assertEquals(1, count(byOwner.and(TransactionSpecifications.descriptionContains("  дома "))));

        // % и _ ищутся как обычные символы, а не как подстановки LIKE
        assertEquals(1, count(byOwner.and(TransactionSpecifications.descriptionContains("50%"))));
        assertEquals(1, count(byOwner.and(TransactionSpecifications.descriptionContains("e_n"))));
        assertEquals(0, count(byOwner.and(TransactionSpecifications.descriptionContains("%%%%"))));

        // пустой запрос фильтр не включает, чужие траты не видны
        assertEquals(5, count(byOwner.and(TransactionSpecifications.descriptionContains(" "))));
        assertEquals(0, count(byOwner.and(TransactionSpecifications.descriptionContains("чужой"))));
    }

    @Test
    void specifications_neverReturnTransactionsOfAnotherUser() {
        var owner = user();
        var stranger = user();
        var a = category(owner, "A");
        var b = category(owner, "B");
        var c = category(stranger, "C");
        prepareTransactionsForFilters(owner, stranger, a, b, c);

        // чужая категория, но запрос от владельца: ничего не находится
        assertEquals(0, count(TransactionSpecifications.hasUserId(owner.getId())
                .and(TransactionSpecifications.hasCategoryId(c.getId()))));
        assertEquals(1, count(TransactionSpecifications.hasUserId(stranger.getId())));
    }

    // ==================== budgets ====================

    @Test
    void budgets_areFoundByUserCategoryAndMonth() {
        var owner = user();
        var stranger = user();
        var food = category(owner, "Food");
        var home = category(owner, "Home");
        var foreign = category(stranger, "Foreign");
        var foodBudget = budget(owner, food, OCTOBER, "1000");
        budget(owner, home, OCTOBER, "2000");
        budget(owner, food, OCTOBER.plusMonths(1), "3000");
        budget(stranger, foreign, OCTOBER, "4000");

        assertEquals(foodBudget.getId(),
                budgetRepository.findByUserIdAndCategory_IdAndPeriod(owner.getId(), food.getId(), OCTOBER).orElseThrow().getId());
        assertTrue(budgetRepository.findByUserIdAndCategory_IdAndPeriod(owner.getId(), home.getId(), OCTOBER.plusMonths(1)).isEmpty());
        assertTrue(budgetRepository.existsByUserIdAndCategory_IdAndPeriod(owner.getId(), food.getId(), OCTOBER));
        assertFalse(budgetRepository.existsByUserIdAndCategory_IdAndPeriod(owner.getId(), home.getId(), OCTOBER.plusMonths(1)));
        assertEquals(2, budgetRepository.findAllByUserIdAndPeriod(owner.getId(), OCTOBER).size());
    }

    @Test
    void budgetsOfMonth_comeWithCategoryAlreadyLoaded() {
        var owner = user();
        budget(owner, category(owner, "Food"), OCTOBER, "1000");
        em.flush();
        em.clear();

        var budgets = budgetRepository.findAllByUserIdAndPeriod(owner.getId(), OCTOBER);

        assertEquals(1, budgets.size());
        // @EntityGraph: категория подтянута сразу, маппинг в DTO не вызовет LazyInitializationException
        assertTrue(Hibernate.isInitialized(budgets.get(0).getCategory()));
    }

    @Test
    void budget_forSameUserCategoryAndMonth_isRejectedByUniqueConstraint() {
        var owner = user();
        var food = category(owner, "Food");
        budget(owner, food, OCTOBER, "1000");

        assertThrows(DataIntegrityViolationException.class, () -> budget(owner, food, OCTOBER, "2000"));
    }

    // ==================== общий бюджет (без категории) ====================

    @Test
    void overallBudget_isFoundSeparatelyFromCategoryBudgets() {
        var owner = user();
        var other = user();
        var food = category(owner, "Food");
        budget(owner, food, OCTOBER, "1000");
        assertFalse(budgetRepository.existsByUserIdAndCategoryIsNullAndPeriod(owner.getId(), OCTOBER));

        budget(owner, null, OCTOBER, "30000");

        assertTrue(budgetRepository.existsByUserIdAndCategoryIsNullAndPeriod(owner.getId(), OCTOBER));
        assertFalse(budgetRepository.existsByUserIdAndCategoryIsNullAndPeriod(owner.getId(), OCTOBER.plusMonths(1)));
        assertFalse(budgetRepository.existsByUserIdAndCategoryIsNullAndPeriod(other.getId(), OCTOBER));
        // оба бюджета месяца возвращаются, у общего категория не подгружается (её нет)
        em.flush();
        em.clear();
        var budgets = budgetRepository.findAllByUserIdAndPeriod(owner.getId(), OCTOBER);
        assertEquals(2, budgets.size());
        assertEquals(1, budgets.stream().filter(b -> b.getCategory() == null).count());
    }

    @Test
    void overallBudget_isUniquePerUserAndMonthEvenThoughCategoryIsNull() {
        var owner = user();
        budget(owner, null, OCTOBER, "30000");

        // обычный UNIQUE с NULL не сработал бы (NULL != NULL), поэтому нужен частичный индекс
        assertThrows(DataIntegrityViolationException.class, () -> budget(owner, null, OCTOBER, "999"));
    }

    @Test
    void overallBudget_canExistForAnotherMonthAnotherUserAndAlongsideCategoryBudget() {
        var owner = user();
        var other = user();
        var food = category(owner, "Food");

        budget(owner, null, OCTOBER, "30000");

        assertDoesNotThrow(() -> {
            budget(owner, null, OCTOBER.plusMonths(1), "30000");
            budget(other, null, OCTOBER, "30000");
            budget(owner, food, OCTOBER, "1000");
        });
    }

    // ==================== новые запросы ====================

    @Test
    void existsByCategory_isTrueOnlyWhenCategoryHasAtLeastOneSplit() {
        var owner = user();
        var used = category(owner, "Used");
        var unused = category(owner, "Unused");
        transaction(owner, LocalDateTime.of(2026, 10, 5, 10, 0), new Split(used, "10.00"));

        assertTrue(splitRepository.existsByCategory_Id(used.getId()));
        assertFalse(splitRepository.existsByCategory_Id(unused.getId()));
    }

    @Test
    void sumByCategory_groupsLargestFirstAndIgnoresOtherUsersAndOtherPeriods() {
        var owner = user();
        var stranger = user();
        var food = category(owner, "Food");
        var home = category(owner, "Home");
        var foreign = category(stranger, "Foreign");
        transaction(owner, LocalDateTime.of(2026, 10, 5, 10, 0), new Split(food, "700.00"), new Split(home, "300.00"));
        transaction(owner, LocalDateTime.of(2026, 10, 20, 10, 0), new Split(food, "500.00"));
        transaction(owner, LocalDateTime.of(2026, 11, 1, 0, 0), new Split(home, "9999.00"));   // следующий месяц
        transaction(stranger, LocalDateTime.of(2026, 10, 5, 10, 0), new Split(foreign, "70.00"));

        var rows = splitRepository.sumByCategory(owner.getId(),
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0));

        assertEquals(2, rows.size());
        assertEquals("Food", rows.get(0).categoryName());
        assertAmount("1200.00", rows.get(0).spent());
        assertEquals("Home", rows.get(1).categoryName());
        assertAmount("300.00", rows.get(1).spent());
        assertTrue(splitRepository.sumByCategory(owner.getId(),
                LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 2, 1, 0, 0)).isEmpty());
    }

    @Test
    void sumOfTransactionsOverHalfOpenRange_excludesTheUpperBoundary() {
        var owner = user();
        var food = category(owner, "Food");
        transaction(owner, LocalDateTime.of(2026, 10, 1, 0, 0), new Split(food, "100.00"));       // нижняя граница входит
        transaction(owner, LocalDateTime.of(2026, 10, 31, 23, 59, 59), new Split(food, "50.00")); // последняя секунда входит
        transaction(owner, LocalDateTime.of(2026, 11, 1, 0, 0), new Split(food, "1000.00"));      // верхняя граница не входит

        assertAmount("150.00", transactionRepository.sumAmountByUserIdAndDateTimeRange(owner.getId(),
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0)));
    }

    @Test
    void monthlyTotals_groupsByMonthOfTheRangeOnly() {
        var owner = user();
        var stranger = user();
        var food = category(owner, "Food");
        var foreign = category(stranger, "Foreign");
        transaction(owner, LocalDateTime.of(2026, 3, 5, 10, 0), new Split(food, "100.50"));
        transaction(owner, LocalDateTime.of(2026, 3, 25, 10, 0), new Split(food, "20.00"));
        transaction(owner, LocalDateTime.of(2026, 10, 1, 0, 0), new Split(food, "2500.00"));
        transaction(owner, LocalDateTime.of(2027, 1, 1, 0, 0), new Split(food, "777.00"));      // другой год
        transaction(stranger, LocalDateTime.of(2026, 3, 5, 10, 0), new Split(foreign, "5.00"));

        var totals = transactionRepository.monthlyTotals(owner.getId(),
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0));

        var byMonth = totals.stream().collect(java.util.stream.Collectors.toMap(
                TransactionRepository.MonthlyTotal::getMonthNumber, TransactionRepository.MonthlyTotal::getTotal));
        assertEquals(2, byMonth.size());
        assertAmount("120.50", byMonth.get(3));
        assertAmount("2500.00", byMonth.get(10));
    }

    @Test
    void specifications_supportOpenEndedDateRanges() {
        var owner = user();
        var food = category(owner, "Food");
        transaction(owner, LocalDateTime.of(2026, 9, 1, 10, 0), new Split(food, "10.00"));
        transaction(owner, LocalDateTime.of(2026, 10, 1, 10, 0), new Split(food, "20.00"));
        transaction(owner, LocalDateTime.of(2026, 11, 1, 10, 0), new Split(food, "30.00"));
        var byOwner = TransactionSpecifications.hasUserId(owner.getId());

        assertEquals(2, count(byOwner.and(TransactionSpecifications.dateBetween(LocalDateTime.of(2026, 10, 1, 10, 0), null))));
        assertEquals(2, count(byOwner.and(TransactionSpecifications.dateBetween(null, LocalDateTime.of(2026, 10, 1, 10, 0)))));
    }

    // ==================== схема БД после миграций ====================

    @Test
    void migrations_createIdentityColumnsForIdsOfTransactionSplitAndBudget() {
        for (String table : List.of("transaction", "transaction_split", "budget", "users", "categories")) {
            String isIdentity = jdbcTemplate.queryForObject(
                    "SELECT is_identity FROM information_schema.columns "
                            + "WHERE table_schema = current_schema() AND table_name = ? AND column_name = 'id'",
                    String.class, table);
            assertEquals("YES", isIdentity, "id of " + table + " must be generated by the database");
        }
    }

    @Test
    void migrations_createIndexesForTheHotQueries() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = current_schema()", String.class);

        assertTrue(indexes.containsAll(List.of(
                "idx_transaction_user_date", "idx_split_transaction", "idx_split_category",
                "idx_categories_user", "idx_budget_category", "uq_budget_overall_user_period")),
                "indexes were: " + indexes);
    }

    @Test
    void migrations_makeCategoryForeignKeyOfSplitsProtectingButDeferred() {
        var row = jdbcTemplate.queryForMap(
                "SELECT confdeltype, condeferrable, condeferred FROM pg_constraint WHERE conname = 'fk_split_category'");

        // 'a' = NO ACTION (раньше было 'c' = CASCADE), проверка откладывается до конца транзакции
        assertEquals("a", String.valueOf(row.get("confdeltype")));
        assertEquals(true, row.get("condeferrable"));
        assertEquals(true, row.get("condeferred"));
    }

    @Test
    void migrations_userBalanceIsNotNullWithZeroDefault() {
        var row = jdbcTemplate.queryForMap(
                "SELECT is_nullable, column_default FROM information_schema.columns "
                        + "WHERE table_schema = current_schema() AND table_name = 'users' AND column_name = 'balance'");

        assertEquals("NO", row.get("is_nullable"));
        assertTrue(String.valueOf(row.get("column_default")).startsWith("0"), "default was " + row.get("column_default"));
    }

    // ==================== защита категорий на уровне БД ====================

    /**
     * Без тестовой транзакции: проверка отложенного внешнего ключа срабатывает только при коммите.
     * Данные создаются и убираются через настоящие коммиты; пользователь удаляется в конце одним DELETE,
     * что заодно проверяет, что каскад по пользователю проходит через защиту категорий.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deletingCategoryWithSplitsFailsAtCommitAndDeletingTheUserStillWorks() {
        var tx = new TransactionTemplate(transactionManager);
        long[] ids = tx.execute(status -> {
            var owner = user();
            var food = category(owner, "Food");
            transaction(owner, LocalDateTime.of(2026, 10, 5, 10, 0), new Split(food, "10.00"));
            return new long[]{owner.getId(), food.getId()};
        });
        long userId = ids[0];
        long categoryId = ids[1];

        try {
            // удаление категории в обход приложения: БД не даёт молча потерять траты
            var failure = assertThrows(Exception.class, () -> tx.executeWithoutResult(
                    status -> jdbcTemplate.update("DELETE FROM categories WHERE id = ?", categoryId)));
            assertTrue(NestedExceptionUtils.getMostSpecificCause(failure).getMessage().contains("fk_split_category"),
                    "cause was: " + NestedExceptionUtils.getMostSpecificCause(failure).getMessage());
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM categories WHERE id = ?", Integer.class, categoryId));
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM transaction_split WHERE category_id = ?", Integer.class, categoryId));
        } finally {
            // каскад по пользователю: категории, транзакции и их части удаляются вместе, порядок каскадов не важен
            tx.executeWithoutResult(status -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId));
        }

        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories WHERE id = ?", Integer.class, categoryId));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transaction_split WHERE category_id = ?", Integer.class, categoryId));
    }
}
