package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.common.service.UserLookUpService;
import dev.zhulidov.cash_tracker.transactions.dto.CategoryCreateRequestDto;
import dev.zhulidov.cash_tracker.transactions.dto.CategoryDto;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionSplitDto;
import dev.zhulidov.cash_tracker.transactions.model.Category;
import dev.zhulidov.cash_tracker.transactions.model.CategoryMapper;
import dev.zhulidov.cash_tracker.transactions.model.TransactionSplit;
import dev.zhulidov.cash_tracker.transactions.model.TransactionSplitMapper;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionSplitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import dev.zhulidov.cash_tracker.common.util.SecurityUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
  private final   CategoryRepository repository;
  private final UserLookUpService userLookUpService;
  private final TransactionSplitRepository splitRepository;
  private final CategoryMapper categoryMapper;
  private final TransactionSplitMapper transactionSplitMapper;

    public CategoryDto createCategory(CategoryCreateRequestDto requestDto, Long userId){

        var category = Category.builder()
                .categoryName(requestDto.categoryName())
                .userId(userId)
                .build();
        var savedCategory = repository.save(category);
        return categoryMapper.toDto(savedCategory);
    }

    public CategoryDto getCategoryById(Long id, Long userId){

        var category = repository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Category not found"));
        SecurityUtils.assertOwner(category.getUserId(),userId);
        return categoryMapper.toDto(category);
    }

    public void deleteCategoryById(Long id, Long userId){
        var category = repository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Category not found"));
        SecurityUtils.assertOwner(category.getUserId(),userId);
        repository.deleteById(id);

    }

    public CategoryDto updateCategory(Long id, String categoryName, Long userId){
       Category category = repository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Category not found"));
        SecurityUtils.assertOwner(category.getUserId(),userId);
        category.setCategoryName(categoryName);
        var savedCat = repository.save(category);
        return categoryMapper.toDto(savedCat);
    }

    public List<CategoryDto> getCategoriesByUserId(Long userId){
        String userName = userLookUpService.getUserInfo(userId).userName();
        return repository.findAllByUserId(userId).stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    public Page<TransactionSplitDto> getSplitsByCategory(Long userId, Long categoryId, Pageable pageable){
        SecurityUtils.assertOwner(repository.findById(categoryId)
                .orElseThrow(()-> new ResourceNotFoundException("Category not found")).getUserId(),userId);
        var splitsPage = splitRepository.findAllByCategory_Id(categoryId,pageable);

        return transactionSplitMapper.toDtoPage(splitsPage);

    }
}
