package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.transactions.dto.CategoryDto;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionSplitDto;
import dev.zhulidov.cash_tracker.transactions.model.CategoryMapper;
import dev.zhulidov.cash_tracker.transactions.model.TransactionSplitMapper;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionSplitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryCacheService {
    private final CategoryRepository categoryRepository;
    private final TransactionSplitRepository transactionSplitRepository;
    private final CategoryMapper categoryMapper;
    private final TransactionSplitMapper splitMapper;

    @Cacheable(value = "categories", key = "#userId")
    public List<CategoryDto> findAllByUser(Long userId){
        return categoryRepository.findAllByUserId(userId).stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    // Транзакция нужна, чтобы при маппинге подгрузились связи category и transaction (open-in-view выключен).
    @Transactional(readOnly = true)
    @Cacheable(value = "splits", key = "#userId + ':' + #categoryId")
    public List<TransactionSplitDto> findSplitsByCategory(Long userId, Long categoryId){
        return transactionSplitRepository.findAllByCategory_Id(categoryId).stream()
                .map(splitMapper::toDto)
                .toList();
    }
}
