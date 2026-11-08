package dev.zhulidov.cash_tracker.transactions.repository;

import dev.zhulidov.cash_tracker.transactions.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Page<Category> findAllByUserId(Long id, Pageable pageable);
    List<Category> findAllByUserId(Long userId);

    Optional<Category> findByUserIdAndCategoryName(Long userId, String categoryName);

    Optional<Category> findFirstByUserIdAndCategoryName(Long userId, String name);
}