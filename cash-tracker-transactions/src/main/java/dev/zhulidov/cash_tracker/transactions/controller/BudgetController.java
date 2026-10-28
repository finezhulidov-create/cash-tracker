package dev.zhulidov.cash_tracker.transactions.controller;

import dev.zhulidov.cash_tracker.common.dto.ErrorResponse;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetCreateRequest;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetDto;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetStatusDto;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetUpdateRequest;
import dev.zhulidov.cash_tracker.transactions.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/v1/budgets")
@RequiredArgsConstructor
@Validated
public class BudgetController {

    private final BudgetService service;

    @Operation(
            summary = "Create budget",
            description = "Создаёт лимит трат на указанный месяц. С categoryId: лимит на категорию. " +
                    "Без categoryId: общий бюджет на весь месяц (учитываются все траты). " +
                    "На месяц допускается один бюджет на категорию и один общий."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget successfully created"),
            @ApiResponse(responseCode = "400", description = "Not valid data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found or inaccessible",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Budget for this category and month already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<BudgetDto> create(@RequestBody @Valid BudgetCreateRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal){
        return ResponseEntity.ok(service.createBudget(request, principal.getId()));
    }

    @Operation(
            summary = "Get budgets by month",
            description = "Возвращает все бюджеты текущего пользователя за месяц (формат yearMonth: yyyy-MM)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budgets successfully returned"),
            @ApiResponse(responseCode = "400", description = "Not valid yearMonth",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<BudgetDto>> getBudgetsByMonth(
            @RequestParam("yearMonth") @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
            @AuthenticationPrincipal UserPrincipal me){
        return ResponseEntity.ok(service.getBudgetsByMonth(me.getId(), yearMonth));
    }

    @Operation(summary = "Get budget by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget successfully returned"),
            @ApiResponse(responseCode = "404", description = "Budget not found or inaccessible",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{budgetId}")
    public ResponseEntity<BudgetDto> getBudget(@PathVariable("budgetId") @Positive Long budgetId,
                                               @AuthenticationPrincipal UserPrincipal me){
        return ResponseEntity.ok(service.getBudget(budgetId, me.getId()));
    }

    @Operation(
            summary = "Get budget status",
            description = "assigned — назначенный лимит, spent — потрачено за месяц по категории, " +
                    "available — остаток (отрицательный при перерасходе)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status successfully returned"),
            @ApiResponse(responseCode = "404", description = "Budget not found or inaccessible",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{budgetId}/status")
    public ResponseEntity<BudgetStatusDto> getStatus(@PathVariable("budgetId") @Positive Long budgetId,
                                                     @AuthenticationPrincipal UserPrincipal me){
        return ResponseEntity.ok(service.getStatus(budgetId, me.getId()));
    }

    @Operation(
            summary = "Update budget",
            description = "Меняет лимит и месяц бюджета. Категория не меняется."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget successfully updated"),
            @ApiResponse(responseCode = "400", description = "Not valid data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Budget not found or inaccessible",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Budget for this category and month already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{budgetId}")
    public ResponseEntity<BudgetDto> updateBudget(@RequestBody @Valid BudgetUpdateRequest request,
                                                  @AuthenticationPrincipal UserPrincipal me,
                                                  @PathVariable("budgetId") @Positive Long budgetId){
        return ResponseEntity.ok(service.updateBudget(request, me.getId(), budgetId));
    }

    @Operation(summary = "Delete budget")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Budget not found or inaccessible",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(@PathVariable("budgetId") @Positive Long budgetId,
                                             @AuthenticationPrincipal UserPrincipal me){
        service.deleteBudget(budgetId, me.getId());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
