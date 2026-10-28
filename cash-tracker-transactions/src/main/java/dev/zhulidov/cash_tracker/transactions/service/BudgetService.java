package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.common.exception.ResourceAlreadyExistsException;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.common.util.SecurityUtils;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetCreateRequest;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetDto;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetStatusDto;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetUpdateRequest;
import dev.zhulidov.cash_tracker.transactions.model.Budget;
import dev.zhulidov.cash_tracker.transactions.model.BudgetMapper;
import dev.zhulidov.cash_tracker.transactions.model.Category;
import dev.zhulidov.cash_tracker.transactions.repository.BudgetRepository;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionSplitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;


@Service
@RequiredArgsConstructor
public class BudgetService {

    private static final String DUPLICATE_CATEGORY_MESSAGE = "Budget for this category and month already exists";
    private static final String DUPLICATE_OVERALL_MESSAGE = "Overall budget for this month already exists";

    private final BudgetRepository repository;
    private final CategoryRepository categoryRepository;
    private final TransactionSplitRepository splitRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetMapper budgetMapper;

    /** categoryId не указан: создаётся общий бюджет на месяц (учитываются все траты). */
    @Transactional
    public BudgetDto createBudget(BudgetCreateRequest request, Long userId){
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(()-> new ResourceNotFoundException("Category Not Found"));
            SecurityUtils.assertOwner(category.getUserId(),userId);
        }
        LocalDate period = request.period().withDayOfMonth(1);
        assertNoDuplicate(userId, category, period);
        var budget = Budget.builder()
                .userId(userId)
                .limitAmount(request.amount())
                .period(period)
                .category(category)
                .build();
        repository.saveAndFlush(budget);
        return budgetMapper.toDto(budget);
    }

    @Transactional
    public BudgetDto updateBudget(BudgetUpdateRequest request, Long userId, Long budgetId){
        var budget = findOwnedBudget(budgetId, userId);
        LocalDate period = request.period().withDayOfMonth(1);
        if (!period.equals(budget.getPeriod())) {
            assertNoDuplicate(userId, budget.getCategory(), period);
        }
        budgetMapper.updateEntityFromDto(request, budget);
        budget.setPeriod(period);
        var saved = repository.saveAndFlush(budget);
        return budgetMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public BudgetDto getBudget(Long budgetId, Long userId){
        return budgetMapper.toDto(findOwnedBudget(budgetId, userId));
    }

    @Transactional(readOnly = true)
    public List<BudgetDto> getBudgetsByMonth(Long userId, YearMonth yearMonth){
        return repository.findAllByUserIdAndPeriod(userId, yearMonth.atDay(1)).stream()
                .map(budgetMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetStatusDto getStatus(Long budgetId, Long userId){
        var budget = findOwnedBudget(budgetId, userId);
        LocalDateTime from = budget.getPeriod().atStartOfDay();
        LocalDateTime to = budget.getPeriod().plusMonths(1).atStartOfDay();
        BigDecimal spent = budget.getCategory() == null
                ? transactionRepository.sumAmountByUserIdAndDateTimeRange(userId, from, to)
                : splitRepository.sumAmountByCategoryAndUserAndDateRange(budget.getCategory().getId(), userId, from, to);
        BigDecimal assigned = budget.getLimitAmount();
        var dto = budgetMapper.toDto(budget);
        return new BudgetStatusDto(dto.id(), dto.period(), dto.category(),
                assigned, spent, assigned.subtract(spent));
    }

    @Transactional
    public void deleteBudget(Long budgetId, Long userId){
        repository.delete(findOwnedBudget(budgetId, userId));
    }

    private void assertNoDuplicate(Long userId, Category category, LocalDate period){
        if (category == null) {
            if (repository.existsByUserIdAndCategoryIsNullAndPeriod(userId, period)) {
                throw new ResourceAlreadyExistsException(DUPLICATE_OVERALL_MESSAGE);
            }
        } else if (repository.existsByUserIdAndCategory_IdAndPeriod(userId, category.getId(), period)) {
            throw new ResourceAlreadyExistsException(DUPLICATE_CATEGORY_MESSAGE);
        }
    }

    private Budget findOwnedBudget(Long budgetId, Long userId){
        var budget = repository.findById(budgetId)
                .orElseThrow(()-> new ResourceNotFoundException("Budget not found"));
        SecurityUtils.assertOwner(budget.getUserId(), userId);
        return budget;
    }

}
