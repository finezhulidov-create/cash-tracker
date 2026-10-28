package dev.zhulidov.cash_tracker.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Новый бюджет на месяц")
public record BudgetCreateRequest(
        @Schema(description = "Лимит трат за месяц, больше нуля", example = "5000.00")
        @NotNull
        @Positive
        BigDecimal amount,
        @Schema(description = "Любая дата месяца, бюджет привязывается к первому числу", example = "2026-10-01")
        @NotNull
        LocalDate period,
        @Schema(description = "Id категории. Не указан: общий бюджет на весь месяц (учитываются все траты)",
                example = "10", nullable = true)
        @Positive
        Long categoryId

) {
}
