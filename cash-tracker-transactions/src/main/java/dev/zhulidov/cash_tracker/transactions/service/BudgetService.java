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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository repository;
    private final CategoryRepository categoryRepository;
    private final BudgetMapper budgetMapper;

    @Transactional
    public BudgetDto createBudget(BudgetCreateRequest request, Long userId){
        var category = categoryRepository.findById(request.category().id())
                .orElseThrow(()-> new ResourceNotFoundException("Category Not Found"));
        SecurityUtils.assertOwner(category.getUserId(),userId);
        var budget = Budget.builder()
                .userId(userId)
                .limitAmount(request.amount())
                .period(request.period())
                .category(category)
                .build();
        repository.saveAndFlush(budget);
        return budgetMapper.toDto(budget);
    }
    @Transactional
    public BudgetDto updateBudget(BudgetUpdateRequest request, Long userID){
        var budget = repository.getBudgetByCategory_Id(request.categoryDto().id())
                .orElseThrow(()-> new ResourceNotFoundException("Budget not found"));
        SecurityUtils.assertOwner(budget.getUserId(), userID);
        budgetMapper.updateEntityFromDto(request,budget);
       var saved = repository.saveAndFlush(budget);
        return budgetMapper.toDto(saved);

    }


}
