package dev.zhulidov.cash_tracker.common.dto;

import jakarta.validation.constraints.NotNull;

public record RegistrationVerificationDto(
        @NotNull
        String email,
        @NotNull
        SendingOperations operations
) {
}
