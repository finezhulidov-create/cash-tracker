package dev.zhulidov.cash_tracker.transactions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequestDto(
        @NotBlank
        @Size(min = 1, max = 50)
        String categoryName
       ) {
}
