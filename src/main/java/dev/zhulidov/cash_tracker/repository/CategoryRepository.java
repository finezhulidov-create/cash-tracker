package dev.zhulidov.cash_tracker.repository;

import dev.zhulidov.cash_tracker.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}