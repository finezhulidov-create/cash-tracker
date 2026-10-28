package dev.zhulidov.cash_tracker.transactions.controller;

import dev.zhulidov.cash_tracker.common.dto.ErrorResponse;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionCreateRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionDto;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionSearchCriteria;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionUpdateRequest;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Tag(name = "Transactions", description = "Траты с разбивкой по категориям")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    @Operation(
            summary = "Create transaction",
            description = "Создаёт трату и разбивает её по категориям. Сумма частей должна равняться общей сумме. " +
                    "Если dateTime не указан, берётся текущее время сервера (UTC)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Трата создана, в ответе её части с id"),
            @ApiResponse(responseCode = "400", description = "Невалидные данные или сумма частей не равна сумме траты (NOT_VALID_TRANSACTION_SPLIT_SUM)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Категория не найдена или принадлежит другому пользователю",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TransactionDto> createTransaction(@RequestBody @Valid TransactionCreateRequest request,
                                                            @AuthenticationPrincipal UserPrincipal principal){
        return ResponseEntity.ok(transactionService.createTransaction(request, principal.getId()));
    }

    @Operation(summary = "Delete transaction", description = "Удаляет трату вместе со всеми её частями.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Трата удалена"),
            @ApiResponse(responseCode = "404", description = "Трата не найдена или принадлежит другому пользователю",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{transId}")
    public ResponseEntity<Void> deleteTransaction(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable("transId") @Positive Long transId){
        transactionService.deleteTransactionById(transId, principal.getId());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(summary = "Update transaction", description = "Полностью заменяет трату и её разбивку.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Трата обновлена"),
            @ApiResponse(responseCode = "400", description = "Невалидные данные или сумма частей не равна сумме траты",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Трата или категория не найдена / недоступна",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{transId}")
    public ResponseEntity<TransactionDto> updateTransaction(@AuthenticationPrincipal UserPrincipal principal,
                                                            @PathVariable("transId") @Positive Long id,
                                                            @RequestBody @Valid TransactionUpdateRequest request){
        return ResponseEntity.ok(transactionService.updateTransaction(request, id, principal.getId()));
    }

    @Operation(summary = "Get transaction by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Трата с разбивкой"),
            @ApiResponse(responseCode = "404", description = "Трата не найдена или принадлежит другому пользователю",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{transId}")
    public ResponseEntity<TransactionDto> getTransaction(@AuthenticationPrincipal UserPrincipal principal,
                                                         @PathVariable("transId") @Positive Long id){
        return ResponseEntity.ok(transactionService.getById(id, principal.getId()));
    }

    @Operation(
            summary = "List transactions",
            description = "Постраничный список трат текущего пользователя, по умолчанию от новых к старым " +
                    "(sort=dateTime,desc). Необязательные фильтры: категория, диапазон сумм, диапазон дат " +
                    "(можно указать только одну границу)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница трат"),
            @ApiResponse(responseCode = "400", description = "Неверные фильтры (from позже to, minAmount больше maxAmount) или поле сортировки",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public PagedModel<TransactionDto> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject @PageableDefault(size = 20, sort = "dateTime", direction = Sort.Direction.DESC) Pageable pageable,
            @ParameterObject @ModelAttribute TransactionSearchCriteria criteria){
        Page<TransactionDto> transactions = transactionService.getTransactions(principal.getId(), criteria, pageable);
        return new PagedModel<>(transactions);
    }

    @Operation(summary = "Total amount for a period",
            description = "Сумма всех трат за период, обе границы включены.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Сумма (0, если трат нет)"),
            @ApiResponse(responseCode = "400", description = "Не указана или неверна граница периода, либо from позже to",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getTotalAmountByDateRange(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to){
        return ResponseEntity.ok(transactionService.getTotalAmountByDateRange(principal.getId(), from, to));
    }
}
