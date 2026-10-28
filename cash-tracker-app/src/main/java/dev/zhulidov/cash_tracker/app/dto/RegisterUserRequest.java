package dev.zhulidov.cash_tracker.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Данные для регистрации")
public record RegisterUserRequest(
        @Schema(description = "Email, он же логин. Хранится в нижнем регистре", example = "alex@example.com")
        @NotBlank
        @Email(message = "Email must be valid")
        @Size(max = 255)
        String email,
        @Schema(description = "Отображаемое имя, 2–20 символов", example = "Alex")
        @NotBlank
        @Size(min = 2, max = 20)
        String username,
        @Schema(description = "Пароль, 8–72 символа (72 — предел bcrypt)", example = "correct-horse-battery")
        @NotBlank
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password) {
}
