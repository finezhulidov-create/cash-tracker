package dev.zhulidov.cash_tracker.controller;

import dev.zhulidov.cash_tracker.dto.*;
import dev.zhulidov.cash_tracker.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.model.Expense;
import dev.zhulidov.cash_tracker.model.UserPrincipal;
import dev.zhulidov.cash_tracker.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.service.CategoryService;
import dev.zhulidov.cash_tracker.service.ExpenseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Validated
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;
    private final ExpenseService expenseService;
    private final CategoryRepository repository;

    @PostMapping("/{categoryId}")
    public ResponseEntity<ExpenseDto> createExpense(@RequestBody @Valid ExpenseCreateRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable @Positive Long categoryId){
        return ResponseEntity.ok(expenseService.createExpense(request, principal.getId(),categoryId));
    }
    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@RequestBody @Valid CategoryCreateRequestDto request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.createCategory(request, principal.getId()));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDto> getCategory(   @AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable("categoryId") @Positive Long categoryId){
        return ResponseEntity.ok(service.getCategoryById(categoryId, principal.getId()));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory( @AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable("categoryId") @Positive  Long categoryId){
        service.deleteCategoryById(categoryId, principal.getId());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryDto> updateCategory(@RequestBody @Valid CategoryUpdateRequestDto requestDto,
                                                      @AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable("categoryId") @Positive  Long categoryId){
        return ResponseEntity.ok(service.updateCategory(categoryId, requestDto.categoryName(), principal.getId()));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<CategoryDto>> getCategoriesByUser(@AuthenticationPrincipal UserPrincipal principal){
        return ResponseEntity.ok(service.getCategoriesByUserId(principal.getId()));
    }

    @GetMapping("/{categoryId}/expenses")
    public ResponseEntity<List<ExpenseDto>> getExpensesByCategory(@AuthenticationPrincipal UserPrincipal principal,
                                                                  @PathVariable("categoryId") @Positive Long categoryId){
        return ResponseEntity.ok(expenseService.getExpensesByCategoryId(categoryId, principal.getId()));
    }

}

