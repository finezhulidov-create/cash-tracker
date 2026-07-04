package dev.zhulidov.cash_tracker.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @Email(message = "Email must be valid")
        @NotBlank
        String email,
        @Size(min = 2, max = 20)
        @NotBlank
        String username,
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{10,}$",
                message = "Password must be at least 10 characters, contain uppercase, lowercase, digit and special character"
        )
        String password) {
}
