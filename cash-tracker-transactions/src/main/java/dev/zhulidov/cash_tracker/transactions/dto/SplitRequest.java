package dev.zhulidov.cash_tracker.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Часть траты, отнесённая к одной категории")
public record SplitRequest(
        @Schema(description = "Id категории пользователя", example = "10")
        @NotNull
        @Positive
        Long categoryId,
        @Schema(description = "Сумма этой части, больше нуля", example = "700.00")
        @NotNull
        @Positive
        BigDecimal amount
) {
}
