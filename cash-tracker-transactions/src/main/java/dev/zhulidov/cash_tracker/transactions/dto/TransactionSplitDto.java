package dev.zhulidov.cash_tracker.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Часть траты, отнесённая к одной категории")
public record TransactionSplitDto(
        @Schema(example = "55") Long id,
        @Schema(description = "Id траты, которой принадлежит эта часть", example = "100") Long transactionId,
        @Schema(example = "700.00") BigDecimal amount,
        CategoryDto category) {
}
