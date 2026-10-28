package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.common.exception.InvalidRequestException;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetStatusDto;
import dev.zhulidov.cash_tracker.transactions.dto.CategorySpendingDto;
import dev.zhulidov.cash_tracker.transactions.dto.MonthSummaryDto;
import dev.zhulidov.cash_tracker.transactions.dto.MonthlySpendingDto;
import dev.zhulidov.cash_tracker.transactions.model.Budget;
import dev.zhulidov.cash_tracker.transactions.model.BudgetMapper;
import dev.zhulidov.cash_tracker.transactions.repository.BudgetRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionSplitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Агрегаты для дашборда. Все суммы считаются при запросе из трат, ничего не хранится отдельно. */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionSplitRepository splitRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetMapper budgetMapper;

    /** Траты по категориям за период; обе даты включены, от больших сумм к меньшим. */
    @Transactional(readOnly = true)
    public List<CategorySpendingDto> spendingByCategory(Long userId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidRequestException("'from' must not be after 'to'");
        }
        return splitRepository.sumByCategory(userId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    /** Траты по месяцам года: всегда 12 записей, месяцы без трат дают 0. */
    @Transactional(readOnly = true)
    public List<MonthlySpendingDto> monthlySpending(Long userId, int year) {
        LocalDateTime from = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime to = LocalDate.of(year + 1, 1, 1).atStartOfDay();
        Map<Integer, BigDecimal> totals = transactionRepository.monthlyTotals(userId, from, to).stream()
                .collect(Collectors.toMap(TransactionRepository.MonthlyTotal::getMonthNumber,
                        TransactionRepository.MonthlyTotal::getTotal));
        return IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new MonthlySpendingDto(YearMonth.of(year, month),
                        totals.getOrDefault(month, BigDecimal.ZERO)))
                .toList();
    }

    /**
     * Сводка месяца: все траты, общий бюджет (если задан) и бюджеты по категориям с остатками.
     * Траты по категориям считаются одним запросом, а не по запросу на каждый бюджет.
     */
    @Transactional(readOnly = true)
    public MonthSummaryDto monthSummary(Long userId, YearMonth yearMonth) {
        LocalDate period = yearMonth.atDay(1);
        LocalDateTime from = period.atStartOfDay();
        LocalDateTime to = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        BigDecimal totalSpent = transactionRepository.sumAmountByUserIdAndDateTimeRange(userId, from, to);
        Map<Long, BigDecimal> spentByCategory = splitRepository.sumByCategory(userId, from, to).stream()
                .collect(Collectors.toMap(CategorySpendingDto::categoryId, CategorySpendingDto::spent));

        BudgetStatusDto overall = null;
        List<BudgetStatusDto> categories = new ArrayList<>();
        for (Budget budget : budgetRepository.findAllByUserIdAndPeriod(userId, period)) {
            if (budget.getCategory() == null) {
                overall = status(budget, totalSpent);
            } else {
                categories.add(status(budget, spentByCategory.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO)));
            }
        }
        categories.sort(Comparator.comparing(status -> status.category().categoryName()));
        return new MonthSummaryDto(yearMonth, totalSpent, overall, categories);
    }

    private BudgetStatusDto status(Budget budget, BigDecimal spent) {
        var dto = budgetMapper.toDto(budget);
        return new BudgetStatusDto(dto.id(), dto.period(), dto.category(),
                budget.getLimitAmount(), spent, budget.getLimitAmount().subtract(spent));
    }
}
