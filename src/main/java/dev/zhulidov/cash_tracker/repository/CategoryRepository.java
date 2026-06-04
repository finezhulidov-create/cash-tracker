package dev.zhulidov.cash_tracker.repository;

import dev.zhulidov.cash_tracker.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByUser_Id(Long id);
}