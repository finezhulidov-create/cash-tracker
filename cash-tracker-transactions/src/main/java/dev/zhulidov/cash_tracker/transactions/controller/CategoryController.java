package dev.zhulidov.cash_tracker.transactions.controller;

import dev.zhulidov.cash_tracker.common.dto.ErrorResponse;
import dev.zhulidov.cash_tracker.transactions.dto.*;

import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.transactions.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Validated
@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;




    @Operation(
            summary = "Create category"

    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category successfully created"),
            @ApiResponse(responseCode = "400", description = "Not valid data ",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@RequestBody @Valid CategoryCreateRequestDto request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.createCategory(request, principal.getId()));
    }
    @Operation(
            summary = "Return category"

    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category successfully returned"),
            @ApiResponse(responseCode = "400", description = "Not valid data ",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "One of the specified categories was not found or is unavailable." ,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDto> getCategory(   @AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable("categoryId") @Positive Long categoryId){
        return ResponseEntity.ok(service.getCategoryById(categoryId, principal.getId()));
    }
    @Operation(
            summary = "Delete category"

    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Not valid data ",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "One of the specified categories was not found or is unavailable." ,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory( @AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable("categoryId") @Positive  Long categoryId){
        service.deleteCategoryById(categoryId, principal.getId());
        return ResponseEntity.status(HttpStatus.OK).build();
    }
    @Operation(
            summary = "Update category"

    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category successfully updated"),
            @ApiResponse(responseCode = "400", description = "Not valid data ",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "One of the specified categories was not found or is unavailable." ,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryDto> updateCategory(@RequestBody @Valid CategoryUpdateRequestDto requestDto,
                                                      @AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable("categoryId") @Positive  Long categoryId){
        return ResponseEntity.ok(service.updateCategory(categoryId, requestDto.categoryName(), principal.getId()));
    }
    @Operation(
            summary = "Get all Catrgories for User"

    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categories successfully returned"),
            @ApiResponse(responseCode = "400", description = "Not valid data ",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "One of the specified categories was not found or is unavailable." ,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/mine")
    public PagedModel<CategoryDto> getCategoriesByUser(@AuthenticationPrincipal UserPrincipal principal,
                                                       Pageable pageable){
            Page<CategoryDto> page = service.getCategoriesByUserId(principal.getId(), pageable);
            return new PagedModel<>(page);
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

