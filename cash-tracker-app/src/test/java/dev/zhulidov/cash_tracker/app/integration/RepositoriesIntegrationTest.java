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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
        BigDecimal total = Arrays.stream(splits).map(s -> new BigDecimal(s.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var tx = Transaction.builder().userId(owner.getId()).amount(total).description("tx").dateTime(when).build();
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

        var loaded = userRepository.findByEmail(saved.getEmail()).orElseThrow();

        assertEquals(saved.getId(), loaded.getId());
        assertAmount("0", loaded.getBalance());
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
}
