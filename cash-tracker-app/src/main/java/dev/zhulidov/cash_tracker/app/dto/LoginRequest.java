package dev.zhulidov.cash_tracker.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Данные для входа")
public record LoginRequest(
        @Schema(example = "alex@example.com")
        @Email
        @NotBlank
        String email,
        @Schema(example = "correct-horse-battery")
        @NotBlank
        String password
) {
}
