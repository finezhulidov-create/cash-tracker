package dev.zhulidov.cash_tracker.transactions.repository;

import dev.zhulidov.cash_tracker.transactions.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
