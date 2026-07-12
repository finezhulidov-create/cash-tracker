package dev.zhulidov.cash_tracker.transactions.controller;

import dev.zhulidov.cash_tracker.transactions.dto.*;

import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.transactions.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    //todo контроллеры работы со сплитами(и в сервисе то же самое)
    //todo redis добавить


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
    @Operation(
            summary = "Get all splits by Category ID",
            description = "Retrieve all splits belonging to this category."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All splits successfully retrieved."),
            @ApiResponse(responseCode = "404", description = "Category not found or inaccessible")
    })
    @GetMapping("/{categoryId}/splits")
    public Page<TransactionSplitDto> getSplits(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable("categoryId") @Positive Long categoryId,
                                               Pageable pageable){
        return service.getSplitsByCategory(principal.getId(), categoryId, pageable);
    }

}

