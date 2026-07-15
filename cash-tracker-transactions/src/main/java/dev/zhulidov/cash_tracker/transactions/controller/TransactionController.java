package dev.zhulidov.cash_tracker.transactions.controller;

import dev.zhulidov.cash_tracker.common.dto.ErrorResponse;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.transactions.dto.*;
import dev.zhulidov.cash_tracker.transactions.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Tag(name = "Transactions", description = "Operations with transactions")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    @Operation(
            summary = "Create transaction",
            description = "Create transactions with splits. " +
                    "Sum of splits must be equal with transaction amount."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction succesfuly created"),
            @ApiResponse(responseCode = "400", description = "Not valid data or split sum not equal transaction amount.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "One of the specified categories was not found or is unavailable." ,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))

    })
    @PostMapping("/create")
    public ResponseEntity<TransactionDto> createTransaction(@RequestBody @Valid TransactionCreateRequest request,
                                                                @AuthenticationPrincipal UserPrincipal principal){
        var dto = transactionService.createTransaction(request, principal.getId());
        return ResponseEntity.ok(dto);
    }

    @Operation(
            summary = "Delete transaction",
            description = "Delete transactions with all splits belonging to " +
                    "this transaction"
    )
    @ApiResponses({
             @ApiResponse(responseCode = "200", description = "Transaction succesful delete"),
            @ApiResponse(responseCode = "404", description = "one of the specified categories was not found or is unavailable" )
    }
    )
    @DeleteMapping("/{transId}")
    public ResponseEntity<Void> deleteTransaction(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable("transId")@Positive Long transId){
        transactionService.deleteTransactionById(transId, principal.getId());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(summary = "Update Transaction",
    description = "Update transaction and update splits"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction successfuly updated"),
            @ApiResponse(responseCode = "400", description = "Not valid data or split sum not equal transaction amount.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "One of the specified categories was not found or is unavailable." ,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{transId}")
    public ResponseEntity<TransactionDto> updateTransaction(@AuthenticationPrincipal UserPrincipal principal,
                                                            @PathVariable("transId")@Positive Long id,
                                                            @RequestBody @Valid TransactionUpdateRequest request){
       var dto = transactionService.updateTransaction(request,id, principal.getId());
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Get transaction by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction successfuly recieved"),
            @ApiResponse(responseCode = "404", description = "One of the specified categories was not found or is unavailable." ,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{transId}")
    public ResponseEntity<TransactionDto> getTransaction(@AuthenticationPrincipal UserPrincipal principal,
                                                         @PathVariable("transId")@Positive Long id){
        return ResponseEntity.ok(transactionService.getById(id, principal.getId()));
    }
    @Operation(summary = "Get all users transactions page")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions successfuly recieved"),

    })
    @GetMapping("/all")
    public Page<TransactionDto> getAllTransactions(@AuthenticationPrincipal UserPrincipal principal,
                                                   Pageable pageable){
        Page<TransactionDto> page = transactionService.getAllTransactionsByUser(principal.getId(), pageable);
        return new PagedModel<>(page);
    }
    @Operation(summary = "Get total amount by date range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Total amount successfuly received")
    })
    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getTotalAmountByDateRange(@AuthenticationPrincipal UserPrincipal principal,
                                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to){
        return ResponseEntity.ok(transactionService.getTotalAmountByDateRange(principal.getId(), from,to));
    }

    @Operation(
            summary = "Get transactions with filters",
            description = "Returns a paginated list of the current user's transactions, " +
                    "optionally filtered by category, amount range and date range. " +
                    "Any filter left unspecified is simply ignored."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions successfully retrieved")
    })
    @GetMapping("/filter")
    public PagedModel<TransactionDto> getTransactions(@AuthenticationPrincipal UserPrincipal principal, Pageable pageable,
                                                      @ModelAttribute TransactionSearchCriteria criteria){
        Page<TransactionDto> transactions = transactionService.getTransactions(principal.getId(), criteria, pageable);
        return new PagedModel<>(transactions);
    }
}

