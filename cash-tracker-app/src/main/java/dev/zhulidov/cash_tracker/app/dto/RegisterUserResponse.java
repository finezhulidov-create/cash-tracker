package dev.zhulidov.cash_tracker.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Результат создания пользователя внутри приложения; наружу отдаётся токен (TokenResponse). */
@Schema(hidden = true)
public record RegisterUserResponse(Long id, String name, String email) {
}
