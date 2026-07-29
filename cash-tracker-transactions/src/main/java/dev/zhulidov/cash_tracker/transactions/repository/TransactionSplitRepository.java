package dev.zhulidov.cash_tracker.transactions.repository;

import dev.zhulidov.cash_tracker.transactions.model.TransactionSplit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionSplitRepository extends JpaRepository<TransactionSplit, Long> {
    void deleteAllByTransaction_Id(Long id);
    Page<TransactionSplit> findAllByCategory_Id(Long id, Pageable pageable);
    List<TransactionSplit> findAllByCategory_Id(Long id);
    @Query("SELECT COALESCE(SUM(ts.amount), 0) FROM TransactionSplit ts " +
            "WHERE ts.category.id = :categoryId " +
            "AND ts.transaction.userId = :userId " +
            "AND ts.transaction.dateTime >= :from AND ts.transaction.dateTime < :to")
    BigDecimal sumAmountByCategoryAndUserAndDateRange(
            @Param("categoryId") Long categoryId,
            @Param("userId") Long userId,
            @Param("from")LocalDateTime from,
            @Param("to") LocalDateTime to
            );
}
