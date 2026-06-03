package dev.zhulidov.cash_tracker.repository;

import dev.zhulidov.cash_tracker.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
}