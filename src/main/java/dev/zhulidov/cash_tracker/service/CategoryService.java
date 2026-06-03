package dev.zhulidov.cash_tracker.service;

import dev.zhulidov.cash_tracker.dto.CategoryCreateRequestDto;
import dev.zhulidov.cash_tracker.dto.CategoryDto;
import dev.zhulidov.cash_tracker.model.Category;
import dev.zhulidov.cash_tracker.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {
    CategoryRepository repository;
    UserRepository userRepository;

    public CategoryDto createCategory(CategoryCreateRequestDto requestDto){
        var user = userRepository.findById(requestDto.userId()).orElseThrow();
        var category = Category.builder()
                .categoryName(requestDto.categoryName())
                .user(user)
                .build();
        var savedCategory = repository.save(category);
        return new CategoryDto(savedCategory.getCategoryName(),savedCategory.getUser().getUserName());
    }

    public CategoryDto getCategory(Long id){
        var category = repository.findById(id).orElseThrow(()-> new RuntimeException("Category not found"));
        return new CategoryDto(category.getCategoryName(), category.getUser().getUserName());
    }

    public void deleteCategory(Long id){
        repository.deleteById(id);
    }

    public CategoryDto updateCategory(Long id, String categoryName){
       Category category = repository.findById(id).orElseThrow(()-> new RuntimeException("Category not found"));
        category.setCategoryName(categoryName);
        var savedCat = repository.save(category);
        return new CategoryDto(savedCat.getCategoryName(), savedCat.getUser().getUserName());
    }
}
