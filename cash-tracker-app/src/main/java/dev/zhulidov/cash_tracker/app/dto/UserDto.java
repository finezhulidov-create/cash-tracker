package dev.zhulidov.cash_tracker.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Профиль текущего пользователя")
public record UserDto(
        @Schema(example = "Alex") String name,
        @Schema(example = "alex@example.com") String email,
        @Schema(description = "Баланс, который пользователь задаёт вручную; от трат не меняется", example = "1500.50")
        BigDecimal balance) {
}
