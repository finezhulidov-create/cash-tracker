package dev.zhulidov.cash_tracker.transactions.repository;

import dev.zhulidov.cash_tracker.transactions.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Page<Category> findAllByUserId(Long id, Pageable pageable);

    List<Category> findAllByTransactionSplits_Category_Id(Long id);

}