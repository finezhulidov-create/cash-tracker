package dev.zhulidov.cash_tracker.app.dto;

import dev.zhulidov.cash_tracker.app.event.SendingOperations;
import jakarta.validation.constraints.NotNull;

public record RegistrationVerificationDto(
        @NotNull
        String email,
        @NotNull
        SendingOperations operations
) {
}
