package dev.zhulidov.cash_tracker.transactions.repository;

import dev.zhulidov.cash_tracker.transactions.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findAllByUserId(Long userId, Pageable pageable);
    @Query("SELECT COALESCE(SUM(t.amount),0) FROM Transaction t" +
    " WHERE t.userId = :userId AND t.dateTime BETWEEN :from AND :to")
    BigDecimal sumAmountByUserIdAndDateTimeBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
    Page<Transaction> findAllByUserIdAndDateTimeBetween(Long userId, LocalDateTime dateTimeStart, LocalDateTime dateTimeEnd, Pageable pageable);
}
