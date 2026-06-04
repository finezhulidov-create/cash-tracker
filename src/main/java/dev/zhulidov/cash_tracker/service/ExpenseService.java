package dev.zhulidov.cash_tracker.service;

import dev.zhulidov.cash_tracker.dto.ExpenseCreateRequest;
import dev.zhulidov.cash_tracker.dto.ExpenseDto;
import dev.zhulidov.cash_tracker.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.model.Expense;
import dev.zhulidov.cash_tracker.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.repository.ExpenseRepository;
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

    public ExpenseDto createExpense(ExpenseCreateRequest request){
        var category = categoryRepository.findById(request.categoryId())
                .orElseThrow(()-> new ResourceNotFoundException("Category Not Found"));
        var expense = Expense.builder()
                .expense(request.expense())
                .amount(request.amount())
                .category(category)
                .dateTime(LocalDateTime.now())
                .build();
        var saveExp = repository.save(expense);
        return new ExpenseDto(saveExp.getExpense(),saveExp.getAmount(),saveExp.getDateTime());
    }

    public void deleteExpenseById(Long id){
        repository.deleteById(id);
    }

    public ExpenseDto updateExpense(String updateExpense, Long id){
        var expense = repository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Expense not found"));
        expense.setExpense(updateExpense);
        repository.save(expense);
        return new ExpenseDto(expense.getExpense(),expense.getAmount(),expense.getDateTime());

    }

    public List<ExpenseDto> getExpensesByCategoryId(Long categoryId){
                List<Expense> expensies = repository.findAllByCategory_Id(categoryId);
        return expensies.stream().map(exp -> new ExpenseDto(exp.getExpense()
                ,exp.getAmount()
                ,exp.getDateTime()))
                .toList();
    }

    public BigDecimal getTotalAmountByUserId(Long userId){
        var expenses = repository.findAllByCategory_User_Id(userId);
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<ExpenseDto> getExpensiesByDateRange(Long userId, LocalDateTime from, LocalDateTime to){
        List<Expense> expenses = repository.findAllByCategory_User_idAndDateTimeBetween(userId,from,to);
        return expenses.stream()
                .map(exp -> new ExpenseDto(exp.getExpense(), exp.getAmount(), exp.getDateTime())).toList();
    }

    public List<ExpenseDto> getExpensesByUserId(Long id){
        return repository.findAllByCategory_User_Id(id).stream()
                .map(e-> new ExpenseDto(e.getExpense(),e.getAmount(),e.getDateTime()))
                .toList();
    }
}
