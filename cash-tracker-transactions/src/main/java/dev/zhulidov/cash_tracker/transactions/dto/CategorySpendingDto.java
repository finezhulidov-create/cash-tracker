package dev.zhulidov.cash_tracker.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Сколько потрачено в одной категории")
public record CategorySpendingDto(
        @Schema(example = "10") Long categoryId,
        @Schema(example = "Food") String categoryName,
        @Schema(example = "1700.50") BigDecimal spent
) {
}
