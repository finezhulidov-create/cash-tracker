package dev.zhulidov.cash_tracker.controller;

import dev.zhulidov.cash_tracker.dto.ExpenseCreateRequest;
import dev.zhulidov.cash_tracker.dto.ExpenseDto;
import dev.zhulidov.cash_tracker.service.ExpenseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService service;

    @PostMapping
    public ResponseEntity<ExpenseDto> createExpense(@RequestBody  ExpenseCreateRequest request){
        return ResponseEntity.ok(service.createExpense(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable  Long id){
        service.deleteExpenseById(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseDto> updateExpense(@PathVariable  Long id, @RequestBody @NotBlank String updateExp){
        return ResponseEntity.ok(service.updateExpense(updateExp,id));
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<List<ExpenseDto>> getExpensesByCategory(@PathVariable  Long id){
        return ResponseEntity.ok(service.getExpensesByCategoryId(id));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<BigDecimal> getTotalAmount(@PathVariable  Long id) {
        return ResponseEntity.ok(service.getTotalAmountByUserId(id));

    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<ExpenseDto>> getExpensesByDateRange(@PathVariable  Long id,
                                                                   @RequestParam @NotNull LocalDateTime from,
                                                                   @RequestParam  @NotNull LocalDateTime to){
        return ResponseEntity.ok(service.getExpensiesByDateRange(id, from, to));
    }

    @GetMapping("/users/{userId}/all")
    public ResponseEntity<List<ExpenseDto>> getExpensesByUser(@PathVariable @Valid Long userId){
        return ResponseEntity.ok(service.getExpensesByUserId(userId));
    }


}


