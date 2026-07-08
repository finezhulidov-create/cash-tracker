package dev.zhulidov.cash_tracker.transactions.repository;

import dev.zhulidov.cash_tracker.transactions.model.TransactionSplit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionSplitRepository extends JpaRepository<TransactionSplit, Long> {
}
