package dev.zhulidov.cash_tracker.dto;

import dev.zhulidov.cash_tracker.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequestDto(
        @NotBlank
        @Size(min = 1, max = 50)
        String categoryName
       ) {
}
