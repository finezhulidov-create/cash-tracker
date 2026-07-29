package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.common.util.SecurityUtils;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetCreateRequest;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetDto;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetUpdateRequest;
import dev.zhulidov.cash_tracker.transactions.model.Budget;
import dev.zhulidov.cash_tracker.transactions.model.BudgetMapper;
import dev.zhulidov.cash_tracker.transactions.repository.BudgetRepository;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionSplitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository repository;
    private final CategoryRepository categoryRepository;
    private final TransactionSplitRepository splitRepository;
    private final BudgetMapper budgetMapper;

    @Transactional
    public BudgetDto createBudget(BudgetCreateRequest request, Long userId){
        var category = categoryRepository.findById(request.categoryId())
                .orElseThrow(()-> new ResourceNotFoundException("Category Not Found"));
        SecurityUtils.assertOwner(category.getUserId(),userId);
        var budget = Budget.builder()
                .userId(userId)
                .limitAmount(request.amount())
                .period(request.period().withDayOfMonth(1))
                .category(category)
                .build();
        repository.saveAndFlush(budget);
        return budgetMapper.toDto(budget);
    }
    @Transactional
    public BudgetDto updateBudget(BudgetUpdateRequest request, Long userID, Long budgetId){
        var budget = repository.findById(budgetId)
                .orElseThrow(()-> new ResourceNotFoundException("Budget not found"));
        SecurityUtils.assertOwner(budget.getUserId(), userID);

        if (!request.period().equals(budget.getPeriod())){
            LocalDate period = request.period().withDayOfMonth(1);
            budget.setPeriod(period);
        }

        budgetMapper.updateEntityFromDto(request,budget);
       var saved = repository.saveAndFlush(budget);
        return budgetMapper.toDto(saved);

    }

    public BigDecimal spentMoneyForCategory(Long budgetId, Long userId){
        var budget = repository.findById(budgetId).orElseThrow(()-> new ResourceNotFoundException("Budget not found"));
        SecurityUtils.assertOwner(budget.getUserId(), userId);
        LocalDateTime from = budget.getPeriod().atStartOfDay();
        LocalDateTime to = budget.getPeriod().plusMonths(1).atStartOfDay();
        return splitRepository.sumAmountByCategoryAndUserAndDateRange(
                budget.getCategory().getId(), userId, from, to);
    }

}
