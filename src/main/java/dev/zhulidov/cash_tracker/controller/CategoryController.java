package dev.zhulidov.cash_tracker.controller;

import dev.zhulidov.cash_tracker.dto.CategoryCreateRequestDto;
import dev.zhulidov.cash_tracker.dto.CategoryDto;
import dev.zhulidov.cash_tracker.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@RequestBody @Valid CategoryCreateRequestDto request) {
        return ResponseEntity.ok(service.createCategory(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategory(@PathVariable @Valid Long id){
        return ResponseEntity.ok(service.getCategoryById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable @Valid Long id){
        service.deleteCategoryById(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable @Valid Long id, @RequestBody @NotBlank String categoryName){
        return ResponseEntity.ok(service.updateCategory(id,categoryName));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<CategoryDto>> getCategoriesByUser(@PathVariable @Valid Long userId){
        return ResponseEntity.ok(service.getCategoriesByUserId(userId));
    }
}

