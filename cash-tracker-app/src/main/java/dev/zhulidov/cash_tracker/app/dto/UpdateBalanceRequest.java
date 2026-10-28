package dev.zhulidov.cash_tracker.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Schema(description = "Новое значение баланса")
public record UpdateBalanceRequest(
        @Schema(description = "Баланс, не меньше нуля", example = "1500.50")
        @NotNull
        @PositiveOrZero
        BigDecimal balance
) {
}
