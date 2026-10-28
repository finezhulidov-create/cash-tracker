package dev.zhulidov.cash_tracker.transactions.repository;

import dev.zhulidov.cash_tracker.transactions.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    /** Сумма трат за период с включёнными обеими границами (для /transactions/total). */
    @Query("SELECT COALESCE(SUM(t.amount),0) FROM Transaction t" +
    " WHERE t.userId = :userId AND t.dateTime BETWEEN :from AND :to")
    BigDecimal sumAmountByUserIdAndDateTimeBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /** Сумма трат за полуоткрытый период [from, to): удобно для месяцев без «хвоста» последней секунды. */
    @Query("SELECT COALESCE(SUM(t.amount),0) FROM Transaction t" +
            " WHERE t.userId = :userId AND t.dateTime >= :from AND t.dateTime < :to")
    BigDecimal sumAmountByUserIdAndDateTimeRange(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /** Суммы трат по месяцам за период [from, to); месяцы без трат в результат не попадают. */
    @Query("SELECT EXTRACT(MONTH FROM t.dateTime) AS monthNumber, SUM(t.amount) AS total FROM Transaction t" +
            " WHERE t.userId = :userId AND t.dateTime >= :from AND t.dateTime < :to" +
            " GROUP BY EXTRACT(MONTH FROM t.dateTime)")
    List<MonthlyTotal> monthlyTotals(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    interface MonthlyTotal {
        Integer getMonthNumber();

        BigDecimal getTotal();
    }
}
