package dev.zhulidov.cash_tracker.repository;

import dev.zhulidov.cash_tracker.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByCategory_Id(Long id);
    List<Expense> findAllByCategory_User_idAndDateTimeBetween(Long userId, LocalDateTime from, LocalDateTime to);
    List<Expense> findAllByCategory_User_Id(Long userId);
}