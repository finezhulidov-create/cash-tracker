package dev.zhulidov.cash_tracker.transactions.repository;

import dev.zhulidov.cash_tracker.transactions.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget,Long> {
   Optional<Budget>  getBudgetByCategory_Id(Long id);
}
