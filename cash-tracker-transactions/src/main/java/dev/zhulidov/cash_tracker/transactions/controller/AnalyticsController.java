package dev.zhulidov.cash_tracker.transactions.controller;

import dev.zhulidov.cash_tracker.common.dto.ErrorResponse;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.transactions.dto.CategorySpendingDto;
import dev.zhulidov.cash_tracker.transactions.dto.MonthSummaryDto;
import dev.zhulidov.cash_tracker.transactions.dto.MonthlySpendingDto;
import dev.zhulidov.cash_tracker.transactions.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Tag(name = "Analytics", description = "Агрегаты для дашборда")
@Validated
@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService service;

    @Operation(summary = "Spending by category",
            description = "Траты по категориям за период (обе даты включены), от больших сумм к меньшим. " +
                    "Подходит для круговой диаграммы.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список {категория, сумма}; пустой, если трат нет"),
            @ApiResponse(responseCode = "400", description = "Не указана дата или from позже to",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/by-category")
    public ResponseEntity<List<CategorySpendingDto>> byCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Начало периода", example = "2026-10-01")
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Конец периода (включительно)", example = "2026-10-31")
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.spendingByCategory(principal.getId(), from, to));
    }

    @Operation(summary = "Spending by month",
            description = "Траты по месяцам года: всегда 12 записей, месяцы без трат дают 0. Подходит для графика динамики.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "12 месяцев с суммами"),
            @ApiResponse(responseCode = "400", description = "Год не указан или вне диапазона 2000–2100",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlySpendingDto>> monthly(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Год", example = "2026")
            @RequestParam("year") @Min(2000) @Max(2100) int year) {
        return ResponseEntity.ok(service.monthlySpending(principal.getId(), year));
    }

    @Operation(summary = "Month summary",
            description = "Сводка месяца: все траты, общий бюджет (если задан) и бюджеты по категориям с остатками.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Сводка"),
            @ApiResponse(responseCode = "400", description = "yearMonth не указан или неверного формата",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/summary")
    public ResponseEntity<MonthSummaryDto> summary(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Месяц в формате yyyy-MM", example = "2026-10")
            @RequestParam("yearMonth") @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        return ResponseEntity.ok(service.monthSummary(principal.getId(), yearMonth));
    }
}
