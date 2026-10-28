package dev.zhulidov.cash_tracker.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ответ на логин и регистрацию: accessToken передаётся в заголовке Authorization как "{tokenType} {accessToken}",
 * expiresIn — время жизни токена в секундах.
 */
@Schema(description = "JWT для заголовка Authorization")
public record TokenResponse(
        @Schema(description = "Токен доступа")
        String accessToken,
        @Schema(description = "Схема авторизации", example = "Bearer")
        String tokenType,
        @Schema(description = "Время жизни токена в секундах", example = "86400")
        long expiresIn
) {
}
