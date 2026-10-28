package dev.zhulidov.cash_tracker.transactions.repository;

import dev.zhulidov.cash_tracker.transactions.dto.CategorySpendingDto;
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

    Page<TransactionSplit> findAllByCategory_Id(Long id, Pageable pageable);
    List<TransactionSplit> findAllByCategory_Id(Long id);

    /** Есть ли у категории хотя бы одна часть траты (тогда её нельзя удалять). */
    boolean existsByCategory_Id(Long categoryId);

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

    /** Траты пользователя по категориям за [from, to), от больших к меньшим. */
    @Query("SELECT new dev.zhulidov.cash_tracker.transactions.dto.CategorySpendingDto(c.id, c.categoryName, SUM(ts.amount)) " +
            "FROM TransactionSplit ts JOIN ts.category c " +
            "WHERE ts.transaction.userId = :userId " +
            "AND ts.transaction.dateTime >= :from AND ts.transaction.dateTime < :to " +
            "GROUP BY c.id, c.categoryName " +
            "ORDER BY SUM(ts.amount) DESC")
    List<CategorySpendingDto> sumByCategory(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
