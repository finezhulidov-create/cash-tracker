package dev.zhulidov.cash_tracker.transactions.repository;

import dev.zhulidov.cash_tracker.transactions.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByCategory_Id(Long id);
    List<Expense> findAllByCategory_UserIdAndDateTimeBetween(Long userId, LocalDateTime from, LocalDateTime to);
    List<Expense> findAllByCategoryUserId(Long userId);
}