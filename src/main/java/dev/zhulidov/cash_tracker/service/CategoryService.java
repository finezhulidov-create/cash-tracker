package dev.zhulidov.cash_tracker.service;

import dev.zhulidov.cash_tracker.dto.CategoryCreateRequestDto;
import dev.zhulidov.cash_tracker.dto.CategoryDto;
import dev.zhulidov.cash_tracker.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.model.Category;
import dev.zhulidov.cash_tracker.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import dev.zhulidov.cash_tracker.util.SecurityUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
  private final   CategoryRepository repository;
  private final   UserRepository userRepository;

    public CategoryDto createCategory(CategoryCreateRequestDto requestDto, Long userId){
        var user = userRepository.findById(userId).orElseThrow(
                ()-> new ResourceNotFoundException("User with id: "+userId+" not found")
        );
        var category = Category.builder()
                .categoryName(requestDto.categoryName())
                .user(user)
                .build();
        var savedCategory = repository.save(category);
        return new CategoryDto(savedCategory.getId(), savedCategory.getCategoryName(),savedCategory.getUser().getName());
    }

    public CategoryDto getCategoryById(Long id, Long userId){

        var category = repository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Category not found"));
        SecurityUtils.assertOwner(category.getUser().getId(),userId);
        return new CategoryDto(category.getId(), category.getCategoryName(), category.getUser().getName());
    }

    public void deleteCategoryById(Long id, Long userId){
        var category = repository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Category not found"));
        SecurityUtils.assertOwner(category.getUser().getId(),userId);
        repository.deleteById(id);

    }

    public CategoryDto updateCategory(Long id, String categoryName, Long userId){
       Category category = repository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Category not found"));
        SecurityUtils.assertOwner(category.getUser().getId(),userId);
        category.setCategoryName(categoryName);
        var savedCat = repository.save(category);
        return new CategoryDto(savedCat.getId(),savedCat.getCategoryName(), savedCat.getUser().getName());

    }

    public List<CategoryDto> getCategoriesByUserId(Long userId){
        return repository.findAllByUser_Id(userId).stream()
                .map(cat-> new CategoryDto(cat.getId(),cat.getCategoryName(), cat.getUser().getName()))
                .toList();
    }
}
