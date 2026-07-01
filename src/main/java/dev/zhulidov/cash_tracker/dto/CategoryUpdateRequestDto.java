package dev.zhulidov.cash_tracker.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryUpdateRequestDto(
        @NotBlank
        String categoryName
) {
}
