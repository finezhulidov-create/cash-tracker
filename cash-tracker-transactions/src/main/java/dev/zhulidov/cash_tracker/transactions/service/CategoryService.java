package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.common.service.UserLookUpService;
import dev.zhulidov.cash_tracker.transactions.dto.CategoryCreateRequestDto;
import dev.zhulidov.cash_tracker.transactions.dto.CategoryDto;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.transactions.model.Category;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import dev.zhulidov.cash_tracker.common.util.SecurityUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
  private final   CategoryRepository repository;
  private final UserLookUpService userLookUpService;

    public CategoryDto createCategory(CategoryCreateRequestDto requestDto, Long userId){

        var category = Category.builder()
                .categoryName(requestDto.categoryName())
                .userId(userLookUpService.getUserInfo(userId).id())
                .build();
        var savedCategory = repository.save(category);
        return new CategoryDto(savedCategory.getId(), savedCategory.getCategoryName(),userLookUpService.getUserInfo(userId).userName());
    }

    public CategoryDto getCategoryById(Long id, Long userId){

        var category = repository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Category not found"));
        SecurityUtils.assertOwner(category.getUserId(),userId);
        return new CategoryDto(category.getId(), category.getCategoryName(), userLookUpService.getUserInfo(userId).userName());
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
        return new CategoryDto(savedCat.getId(),savedCat.getCategoryName(), userLookUpService.getUserInfo(userId).userName());

    }

    public List<CategoryDto> getCategoriesByUserId(Long userId){
        return repository.findAllByUserId(userId).stream()
                .map(cat-> new CategoryDto(cat.getId(),cat.getCategoryName(), userLookUpService.getUserInfo(userId).userName()))
                .toList();
    }
}
