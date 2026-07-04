package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.transactions.dto.ExpenseCreateRequest;
import dev.zhulidov.cash_tracker.transactions.dto.ExpenseDto;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.transactions.model.Expense;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.transactions.repository.ExpenseRepository;
import dev.zhulidov.cash_tracker.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {
  private final   ExpenseRepository repository;
  private final   CategoryRepository categoryRepository;

    public ExpenseDto createExpense(ExpenseCreateRequest request, Long userId,Long categoryId ){
        var category = categoryRepository.findById(categoryId)
                .orElseThrow(()-> new ResourceNotFoundException("Category Not Found"));
        var expense = Expense.builder()
                .expense(request.expense())
                .amount(request.amount())
                .category(category)
                .dateTime(LocalDateTime.now())
                .build();
        SecurityUtils.assertOwner(expense.getCategory().getUserId(),userId);
        var saveExp = repository.save(expense);
        return new ExpenseDto(saveExp.getExpense(),saveExp.getAmount(),saveExp.getDateTime());

    }

    public void deleteExpenseById(Long id, Long userId){
       var expense = repository.findById(id)
               .orElseThrow(()-> new ResourceNotFoundException("Expense not found"));
        SecurityUtils.assertOwner(expense.getCategory().getUserId(),userId);
         repository.deleteById(id);


    }

    public ExpenseDto updateExpense(String updateExpense, Long id, Long userId){
        var expense = repository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Expense not found"));
        SecurityUtils.assertOwner(expense.getCategory().getUserId(),userId);
            expense.setExpense(updateExpense);
            repository.save(expense);
            return new ExpenseDto(expense.getExpense(), expense.getAmount(), expense.getDateTime());

    }

    public List<ExpenseDto> getExpensesByCategoryId(Long categoryId, Long userId){
                SecurityUtils.assertOwner(categoryRepository.findById(categoryId)
                        .orElseThrow(()-> new ResourceNotFoundException("Category not found")).getUserId(),userId);
                List<Expense> expenses = repository.findAllByCategory_Id(categoryId);
        return expenses.stream().map(exp -> new ExpenseDto(exp.getExpense()
                ,exp.getAmount()
                ,exp.getDateTime()))
                .toList();
    }

    public BigDecimal getTotalAmountByUserId(Long userId){
        var expenses = repository.findAllByCategoryUserId(userId);
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<ExpenseDto> getExpensesByDateRange(Long userId, LocalDateTime from, LocalDateTime to){
        List<Expense> expenses = repository.findAllByCategory_UserIdAndDateTimeBetween(userId,from,to);
        return expenses.stream()
                .map(exp -> new ExpenseDto(exp.getExpense(), exp.getAmount(), exp.getDateTime())).toList();
    }

    public List<ExpenseDto> getExpensesByUserId(Long id){
        return repository.findAllByCategoryUserId(id).stream()
                .map(e-> new ExpenseDto(e.getExpense(),e.getAmount(),e.getDateTime()))
                .toList();
    }


}
