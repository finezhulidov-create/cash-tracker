package dev.zhulidov.cash_tracker.transactions.controller;

import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetCreateRequest;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetDto;
import dev.zhulidov.cash_tracker.transactions.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/budget")
@RequiredArgsConstructor
@Validated
public class BudgetController {

    private final BudgetService service;

    @PostMapping("/create")
    public ResponseEntity<BudgetDto> create(@RequestBody @Valid BudgetCreateRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal){
        return ResponseEntity.ok(service.createBudget(request, principal.getId()));
    }
}

