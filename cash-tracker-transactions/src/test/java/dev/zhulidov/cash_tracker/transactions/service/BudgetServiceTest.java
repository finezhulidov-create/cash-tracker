package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.common.exception.InaccessibleResourceException;
import dev.zhulidov.cash_tracker.common.exception.ResourceAlreadyExistsException;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetCreateRequest;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetUpdateRequest;
import dev.zhulidov.cash_tracker.transactions.model.Budget;
import dev.zhulidov.cash_tracker.transactions.model.BudgetMapper;
import dev.zhulidov.cash_tracker.transactions.model.BudgetMapperImpl;
import dev.zhulidov.cash_tracker.transactions.model.Category;
import dev.zhulidov.cash_tracker.transactions.repository.BudgetRepository;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionSplitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository repository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionSplitRepository splitRepository;
    // настоящий маппер: нужно проверить, что он не затирает нормализованный период
    @Spy
    private BudgetMapper budgetMapper = new BudgetMapperImpl();

    @InjectMocks
    private BudgetService budgetService;

    private static final Long OWNER_ID = 1L;
    private static final Long STRANGER_ID = 2L;
    private static final Long BUDGET_ID = 5L;
    private static final Long CATEGORY_ID = 10L;
    private static final LocalDate OCTOBER = LocalDate.of(2026, 10, 1);
    private static final LocalDate NOVEMBER = LocalDate.of(2026, 11, 1);

    private Category category(Long ownerId) {
        return Category.builder().id(CATEGORY_ID).categoryName("Food").userId(ownerId).build();
    }

    private Budget budget(Long ownerId, String limit, LocalDate period) {
        return Budget.builder().id(BUDGET_ID).userId(ownerId)
                .limitAmount(new BigDecimal(limit)).period(period).category(category(OWNER_ID)).build();
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    // ==================== createBudget ====================

    @Test
    void createBudget_whenValid_savesWithPeriodNormalizedToFirstDayOfMonth() {
        var request = new BudgetCreateRequest(new BigDecimal("5000.00"), LocalDate.of(2026, 10, 17), CATEGORY_ID);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category(OWNER_ID)));
        when(repository.existsByUserIdAndCategory_IdAndPeriod(OWNER_ID, CATEGORY_ID, OCTOBER)).thenReturn(false);

        var result = budgetService.createBudget(request, OWNER_ID);

        var captor = ArgumentCaptor.forClass(Budget.class);
        verify(repository).saveAndFlush(captor.capture());
        var saved = captor.getValue();
        assertEquals(OWNER_ID, saved.getUserId());
        assertEquals(OCTOBER, saved.getPeriod());
        assertAmount("5000.00", saved.getLimitAmount());
        assertEquals(CATEGORY_ID, saved.getCategory().getId());
        assertEquals(OCTOBER, result.period());
        assertEquals(CATEGORY_ID, result.category().id());
        assertAmount("5000.00", result.limitAmount());
    }

    @Test
    void createBudget_whenCategoryNotFound_throwsResourceNotFoundException() {
        var request = new BudgetCreateRequest(new BigDecimal("100"), OCTOBER, CATEGORY_ID);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> budgetService.createBudget(request, OWNER_ID));

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createBudget_whenCategoryBelongsToAnotherUser_throwsInaccessibleResourceException() {
        var request = new BudgetCreateRequest(new BigDecimal("100"), OCTOBER, CATEGORY_ID);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category(STRANGER_ID)));

        assertThrows(InaccessibleResourceException.class, () -> budgetService.createBudget(request, OWNER_ID));

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createBudget_whenBudgetForCategoryAndMonthExists_throwsResourceAlreadyExistsException() {
        // период в запросе — 25 октября: проверка дубликата должна идти по нормализованному 1 октября
        var request = new BudgetCreateRequest(new BigDecimal("100"), LocalDate.of(2026, 10, 25), CATEGORY_ID);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category(OWNER_ID)));
        when(repository.existsByUserIdAndCategory_IdAndPeriod(OWNER_ID, CATEGORY_ID, OCTOBER)).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> budgetService.createBudget(request, OWNER_ID));

        verify(repository, never()).saveAndFlush(any());
    }

    // ==================== updateBudget ====================

    @Test
    void updateBudget_whenOnlyLimitChanges_updatesLimitAndKeepsPeriod() {
        var existing = budget(OWNER_ID, "1000.00", OCTOBER);
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        // 15 октября нормализуется в 1 октября — месяц тот же, проверка на дубликат не нужна
        var result = budgetService.updateBudget(
                new BudgetUpdateRequest(new BigDecimal("2000.00"), LocalDate.of(2026, 10, 15)), OWNER_ID, BUDGET_ID);

        assertAmount("2000.00", existing.getLimitAmount());
        assertEquals(OCTOBER, existing.getPeriod());
        assertAmount("2000.00", result.limitAmount());
        verify(repository, never()).existsByUserIdAndCategory_IdAndPeriod(any(), any(), any());
    }

    @Test
    void updateBudget_whenMovedToFreeMonth_storesPeriodNormalizedToFirstDay() {
        var existing = budget(OWNER_ID, "1000.00", OCTOBER);
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.of(existing));
        when(repository.existsByUserIdAndCategory_IdAndPeriod(OWNER_ID, CATEGORY_ID, NOVEMBER)).thenReturn(false);
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        var result = budgetService.updateBudget(
                new BudgetUpdateRequest(new BigDecimal("1000.00"), LocalDate.of(2026, 11, 20)), OWNER_ID, BUDGET_ID);

        // регрессия: маппер раньше затирал нормализованный период сырой датой из запроса
        assertEquals(NOVEMBER, existing.getPeriod());
        assertEquals(NOVEMBER, result.period());
    }

    @Test
    void updateBudget_whenTargetMonthAlreadyHasBudget_throwsResourceAlreadyExistsException() {
        var existing = budget(OWNER_ID, "1000.00", OCTOBER);
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.of(existing));
        when(repository.existsByUserIdAndCategory_IdAndPeriod(OWNER_ID, CATEGORY_ID, NOVEMBER)).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> budgetService.updateBudget(
                new BudgetUpdateRequest(new BigDecimal("1000.00"), NOVEMBER), OWNER_ID, BUDGET_ID));

        verify(repository, never()).saveAndFlush(any());
        assertEquals(OCTOBER, existing.getPeriod());
    }

    @Test
    void updateBudget_whenNotFound_throwsResourceNotFoundException() {
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> budgetService.updateBudget(
                new BudgetUpdateRequest(new BigDecimal("1"), OCTOBER), OWNER_ID, BUDGET_ID));
    }

    @Test
    void updateBudget_whenBelongsToAnotherUser_throwsInaccessibleResourceException() {
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.of(budget(STRANGER_ID, "1000.00", OCTOBER)));

        assertThrows(InaccessibleResourceException.class, () -> budgetService.updateBudget(
                new BudgetUpdateRequest(new BigDecimal("1"), OCTOBER), OWNER_ID, BUDGET_ID));

        verify(repository, never()).saveAndFlush(any());
    }

    // ==================== getBudget / getBudgetsByMonth ====================

    @Test
    void getBudget_whenOwned_returnsDtoWithId() {
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.of(budget(OWNER_ID, "1000.00", OCTOBER)));

        var result = budgetService.getBudget(BUDGET_ID, OWNER_ID);

        assertEquals(BUDGET_ID, result.id());
        assertEquals(OCTOBER, result.period());
        assertAmount("1000.00", result.limitAmount());
    }

    @Test
    void getBudget_whenNotFound_throwsResourceNotFoundException() {
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> budgetService.getBudget(BUDGET_ID, OWNER_ID));
    }

    @Test
    void getBudget_whenBelongsToAnotherUser_throwsInaccessibleResourceException() {
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.of(budget(STRANGER_ID, "1000.00", OCTOBER)));

        assertThrows(InaccessibleResourceException.class, () -> budgetService.getBudget(BUDGET_ID, OWNER_ID));
    }

    @Test
    void getBudgetsByMonth_queriesFirstDayOfMonthAndMapsAllBudgets() {
        when(repository.findAllByUserIdAndPeriod(OWNER_ID, OCTOBER)).thenReturn(List.of(
                budget(OWNER_ID, "1000.00", OCTOBER), budget(OWNER_ID, "2000.00", OCTOBER)));

        var result = budgetService.getBudgetsByMonth(OWNER_ID, YearMonth.of(2026, 10));

        assertEquals(2, result.size());
        assertAmount("2000.00", result.get(1).limitAmount());
    }

    @Test
    void getBudgetsByMonth_whenNoBudgets_returnsEmptyList() {
        when(repository.findAllByUserIdAndPeriod(OWNER_ID, OCTOBER)).thenReturn(List.of());

        assertTrue(budgetService.getBudgetsByMonth(OWNER_ID, YearMonth.of(2026, 10)).isEmpty());
    }

    // ==================== getStatus ====================

    @Test
    void getStatus_calculatesAvailableAsAssignedMinusSpentForTheWholeMonth() {
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.of(budget(OWNER_ID, "5000.00", OCTOBER)));
        when(splitRepository.sumAmountByCategoryAndUserAndDateRange(
                CATEGORY_ID, OWNER_ID, LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0)))
                .thenReturn(new BigDecimal("1200.50"));

        var status = budgetService.getStatus(BUDGET_ID, OWNER_ID);

        assertEquals(BUDGET_ID, status.budgetId());
        assertEquals(OCTOBER, status.period());
        assertEquals(CATEGORY_ID, status.category().id());
        assertAmount("5000.00", status.assigned());
        assertAmount("1200.50", status.spent());
        assertAmount("3799.50", status.available());
    }

    @Test
    void getStatus_whenOverspent_availableIsNegative() {
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.of(budget(OWNER_ID, "1000.00", OCTOBER)));
        when(splitRepository.sumAmountByCategoryAndUserAndDateRange(any(), any(), any(), any()))
                .thenReturn(new BigDecimal("1500.00"));

        assertAmount("-500.00", budgetService.getStatus(BUDGET_ID, OWNER_ID).available());
    }

    @Test
    void getStatus_whenNothingSpent_availableEqualsAssigned() {
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.of(budget(OWNER_ID, "1000.00", OCTOBER)));
        when(splitRepository.sumAmountByCategoryAndUserAndDateRange(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        var status = budgetService.getStatus(BUDGET_ID, OWNER_ID);

        assertAmount("0", status.spent());
        assertAmount("1000.00", status.available());
    }

    @Test
    void getStatus_whenNotFound_throwsResourceNotFoundException() {
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> budgetService.getStatus(BUDGET_ID, OWNER_ID));
    }

    @Test
    void getStatus_whenBelongsToAnotherUser_doesNotQuerySpentAmount() {
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.of(budget(STRANGER_ID, "1000.00", OCTOBER)));

        assertThrows(InaccessibleResourceException.class, () -> budgetService.getStatus(BUDGET_ID, OWNER_ID));

        verifyNoInteractions(splitRepository);
    }

    // ==================== deleteBudget ====================

    @Test
    void deleteBudget_whenOwned_deletesIt() {
        var existing = budget(OWNER_ID, "1000.00", OCTOBER);
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.of(existing));

        budgetService.deleteBudget(BUDGET_ID, OWNER_ID);

        verify(repository).delete(existing);
    }

    @Test
    void deleteBudget_whenNotFound_throwsResourceNotFoundException() {
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> budgetService.deleteBudget(BUDGET_ID, OWNER_ID));

        verify(repository, never()).delete(any());
    }

    @Test
    void deleteBudget_whenBelongsToAnotherUser_throwsInaccessibleResourceException() {
        when(repository.findById(BUDGET_ID)).thenReturn(Optional.of(budget(STRANGER_ID, "1000.00", OCTOBER)));

        assertThrows(InaccessibleResourceException.class, () -> budgetService.deleteBudget(BUDGET_ID, OWNER_ID));

        verify(repository, never()).delete(any());
    }
}
