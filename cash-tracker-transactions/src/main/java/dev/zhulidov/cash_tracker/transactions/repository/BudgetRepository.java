package dev.zhulidov.cash_tracker.transactions.repository;

import dev.zhulidov.cash_tracker.transactions.model.Budget;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget,Long> {
    Optional<Budget> findByUserIdAndCategory_IdAndPeriod(Long userId, Long categoryId, LocalDate period);
    boolean existsByUserIdAndCategory_IdAndPeriod(Long userId, Long categoryId, LocalDate period);

    // category подтягиваем сразу: иначе при маппинге в DTO будет N+1 / LazyInitializationException
    @EntityGraph(attributePaths = "category")
    List<Budget> findAllByUserIdAndPeriod(Long userId, LocalDate period);
}
