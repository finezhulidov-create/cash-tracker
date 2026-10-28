package dev.zhulidov.cash_tracker.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Текущий баланс")
public record BalanceResponse(
        @Schema(example = "1500.50") BigDecimal balance
) {
}
