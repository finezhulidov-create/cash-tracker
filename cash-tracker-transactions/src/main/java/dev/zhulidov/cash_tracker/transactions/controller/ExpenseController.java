package dev.zhulidov.cash_tracker.transactions.controller;

import dev.zhulidov.cash_tracker.transactions.dto.ExpenseDto;
import dev.zhulidov.cash_tracker.transactions.dto.ExpenseUpdateRequestDto;
import dev.zhulidov.cash_tracker.transactions.dto.ExpensesByDateRangeRequestDto;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.transactions.service.ExpenseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
@Validated
@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService service;



    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable("expenseId") @Positive Long expenseId){
        service.deleteExpenseById(expenseId, principal.getId());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<ExpenseDto> updateExpense(@PathVariable @Positive Long expenseId,
                                                    @RequestBody @Valid ExpenseUpdateRequestDto updateExp,
                                                    @AuthenticationPrincipal UserPrincipal principal){
        return ResponseEntity.ok(service.updateExpense(updateExp.updateExpense(),expenseId, principal.getId()));
    }



    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getTotalAmount(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getTotalAmountByUserId(principal.getId()));

    }

    @GetMapping("/by_date")
    public ResponseEntity<List<ExpenseDto>> getExpensesByDateRange(@AuthenticationPrincipal UserPrincipal principal,
                                                                   @ModelAttribute @Valid ExpensesByDateRangeRequestDto requestDto
                                                                  ){
        return ResponseEntity.ok(service.getExpensesByDateRange(principal.getId(),
                requestDto.getFrom(), requestDto.getTo()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ExpenseDto>> getExpensesByUser(@AuthenticationPrincipal UserPrincipal principal){
        return ResponseEntity.ok(service.getExpensesByUserId(principal.getId()));
    }


}


