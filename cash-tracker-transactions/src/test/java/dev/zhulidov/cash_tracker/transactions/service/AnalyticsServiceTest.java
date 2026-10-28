package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.common.exception.InvalidRequestException;
import dev.zhulidov.cash_tracker.transactions.dto.CategorySpendingDto;
import dev.zhulidov.cash_tracker.transactions.model.Budget;
import dev.zhulidov.cash_tracker.transactions.model.BudgetMapper;
import dev.zhulidov.cash_tracker.transactions.model.BudgetMapperImpl;
import dev.zhulidov.cash_tracker.transactions.model.Category;
import dev.zhulidov.cash_tracker.transactions.repository.BudgetRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionSplitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TransactionSplitRepository splitRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @Spy
    private BudgetMapper budgetMapper = new BudgetMapperImpl();

    @InjectMocks
    private AnalyticsService service;

    private static final Long USER_ID = 1L;
    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);
    private static final LocalDateTime OCT_START = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime NOV_START = LocalDateTime.of(2026, 11, 1, 0, 0);

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private Budget budget(long id, Category category, String limit) {
        return Budget.builder().id(id).userId(USER_ID).category(category)
                .limitAmount(new BigDecimal(limit)).period(OCTOBER.atDay(1)).build();
    }

    private Category category(long id, String name) {
        return Category.builder().id(id).categoryName(name).userId(USER_ID).build();
    }

    private TransactionRepository.MonthlyTotal total(int month, String amount) {
        return new TransactionRepository.MonthlyTotal() {
            @Override
            public Integer getMonthNumber() {
                return month;
            }

            @Override
            public BigDecimal getTotal() {
                return new BigDecimal(amount);
            }
        };
    }

    // ==================== spendingByCategory ====================

    @Test
    void spendingByCategory_includesBothBoundaryDaysByQueryingUntilTheStartOfTheNextDay() {
        var rows = List.of(new CategorySpendingDto(10L, "Food", new BigDecimal("700.00")));
        when(splitRepository.sumByCategory(USER_ID, OCT_START, NOV_START)).thenReturn(rows);

        var result = service.spendingByCategory(USER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertEquals(rows, result);
    }

    @Test
    void spendingByCategory_forSingleDay_coversExactlyThatDay() {
        when(splitRepository.sumByCategory(USER_ID,
                LocalDateTime.of(2026, 10, 15, 0, 0), LocalDateTime.of(2026, 10, 16, 0, 0))).thenReturn(List.of());

        assertTrue(service.spendingByCategory(USER_ID, LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15)).isEmpty());
    }

    @Test
    void spendingByCategory_whenFromIsAfterTo_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class,
                () -> service.spendingByCategory(USER_ID, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1)));

        verifyNoInteractions(splitRepository);
    }

    // ==================== monthlySpending ====================

    @Test
    void monthlySpending_returnsTwelveMonthsAndFillsMonthsWithoutSpendingWithZero() {
        when(transactionRepository.monthlyTotals(USER_ID,
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0)))
                .thenReturn(List.of(total(3, "100.50"), total(10, "2500.00")));

        var result = service.monthlySpending(USER_ID, 2026);

        assertEquals(12, result.size());
        assertEquals(YearMonth.of(2026, 1), result.get(0).month());
        assertEquals(YearMonth.of(2026, 12), result.get(11).month());
        assertAmount("0", result.get(0).spent());
        assertAmount("100.50", result.get(2).spent());
        assertAmount("2500.00", result.get(9).spent());
        assertAmount("0", result.get(11).spent());
    }

    @Test
    void monthlySpending_whenNoTransactionsAtAll_returnsTwelveZeros() {
        when(transactionRepository.monthlyTotals(eq(USER_ID), any(), any())).thenReturn(List.of());

        var result = service.monthlySpending(USER_ID, 2026);

        assertEquals(12, result.size());
        assertTrue(result.stream().allMatch(month -> month.spent().signum() == 0));
    }

    // ==================== monthSummary ====================

    @Test
    void monthSummary_combinesTotalSpentOverallBudgetAndCategoryBudgets() {
        var food = category(10L, "Food");
        var home = category(20L, "Home");
        when(transactionRepository.sumAmountByUserIdAndDateTimeRange(USER_ID, OCT_START, NOV_START))
                .thenReturn(new BigDecimal("12000.00"));
        when(splitRepository.sumByCategory(USER_ID, OCT_START, NOV_START)).thenReturn(List.of(
                new CategorySpendingDto(10L, "Food", new BigDecimal("700.00"))));
        // Home идёт первым в выдаче БД: сводка должна отсортировать категории по названию
        when(budgetRepository.findAllByUserIdAndPeriod(USER_ID, OCTOBER.atDay(1))).thenReturn(List.of(
                budget(2, home, "3000.00"), budget(3, null, "30000.00"), budget(1, food, "5000.00")));

        var summary = service.monthSummary(USER_ID, OCTOBER);

        assertEquals(OCTOBER, summary.yearMonth());
        assertAmount("12000.00", summary.totalSpent());
        assertNotNull(summary.overall());
        assertNull(summary.overall().category());
        assertAmount("30000.00", summary.overall().assigned());
        assertAmount("12000.00", summary.overall().spent());
        assertAmount("18000.00", summary.overall().available());

        assertEquals(2, summary.categories().size());
        assertEquals("Food", summary.categories().get(0).category().categoryName());
        assertAmount("700.00", summary.categories().get(0).spent());
        assertAmount("4300.00", summary.categories().get(0).available());
        assertEquals("Home", summary.categories().get(1).category().categoryName());
        assertAmount("0", summary.categories().get(1).spent());
        assertAmount("3000.00", summary.categories().get(1).available());
    }

    @Test
    void monthSummary_whenCategoryIsOverspent_availableIsNegative() {
        var food = category(10L, "Food");
        when(transactionRepository.sumAmountByUserIdAndDateTimeRange(USER_ID, OCT_START, NOV_START))
                .thenReturn(new BigDecimal("900.00"));
        when(splitRepository.sumByCategory(USER_ID, OCT_START, NOV_START)).thenReturn(List.of(
                new CategorySpendingDto(10L, "Food", new BigDecimal("900.00"))));
        when(budgetRepository.findAllByUserIdAndPeriod(USER_ID, OCTOBER.atDay(1)))
                .thenReturn(List.of(budget(1, food, "500.00")));

        var summary = service.monthSummary(USER_ID, OCTOBER);

        assertAmount("-400.00", summary.categories().get(0).available());
    }

    @Test
    void monthSummary_whenNoOverallBudget_overallIsNull() {
        var food = category(10L, "Food");
        when(transactionRepository.sumAmountByUserIdAndDateTimeRange(USER_ID, OCT_START, NOV_START))
                .thenReturn(BigDecimal.ZERO);
        when(splitRepository.sumByCategory(USER_ID, OCT_START, NOV_START)).thenReturn(List.of());
        when(budgetRepository.findAllByUserIdAndPeriod(USER_ID, OCTOBER.atDay(1)))
                .thenReturn(List.of(budget(1, food, "5000.00")));

        var summary = service.monthSummary(USER_ID, OCTOBER);

        assertNull(summary.overall());
        assertEquals(1, summary.categories().size());
    }

    @Test
    void monthSummary_whenNothingExists_returnsZeroTotalAndEmptyLists() {
        when(transactionRepository.sumAmountByUserIdAndDateTimeRange(USER_ID, OCT_START, NOV_START))
                .thenReturn(BigDecimal.ZERO);
        when(splitRepository.sumByCategory(USER_ID, OCT_START, NOV_START)).thenReturn(List.of());
        when(budgetRepository.findAllByUserIdAndPeriod(USER_ID, OCTOBER.atDay(1))).thenReturn(List.of());

        var summary = service.monthSummary(USER_ID, OCTOBER);

        assertAmount("0", summary.totalSpent());
        assertNull(summary.overall());
        assertTrue(summary.categories().isEmpty());
    }

    @Test
    void monthSummary_readsSpendingByCategoryWithSingleQuery() {
        when(transactionRepository.sumAmountByUserIdAndDateTimeRange(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(splitRepository.sumByCategory(any(), any(), any())).thenReturn(List.of());
        when(budgetRepository.findAllByUserIdAndPeriod(any(), any())).thenReturn(List.of(
                budget(1, category(10L, "Food"), "1"), budget(2, category(20L, "Home"), "1")));

        service.monthSummary(USER_ID, OCTOBER);

        verify(splitRepository, times(1)).sumByCategory(USER_ID, OCT_START, NOV_START);
        verify(splitRepository, never()).sumAmountByCategoryAndUserAndDateRange(any(), any(), any(), any());
    }
}
