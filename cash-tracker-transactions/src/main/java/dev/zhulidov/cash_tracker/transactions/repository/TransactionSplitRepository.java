package dev.zhulidov.cash_tracker.transactions.repository;

import dev.zhulidov.cash_tracker.transactions.model.TransactionSplit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionSplitRepository extends JpaRepository<TransactionSplit, Long> {
    void deleteAllByTransaction_Id(Long id);
    Page<TransactionSplit> findAllByCategory_Id(Long id, Pageable pageable);
    List<TransactionSplit> findAllByCategory_Id(Long id);
}
