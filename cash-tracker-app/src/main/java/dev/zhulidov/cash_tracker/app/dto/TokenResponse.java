package dev.zhulidov.cash_tracker.app.dto;

/**
 * Ответ на логин: accessToken передаётся в заголовке Authorization как "{tokenType} {accessToken}",
 * expiresIn — время жизни токена в секундах.
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
