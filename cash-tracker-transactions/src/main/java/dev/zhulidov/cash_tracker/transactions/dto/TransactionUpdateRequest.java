package dev.zhulidov.cash_tracker.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Полная замена траты: описание, сумма, дата и вся разбивка передаются заново")
public record TransactionUpdateRequest(
        @Schema(example = "Супермаркет")
        @NotBlank
        @Size(max = 255)
        String description,
        @Schema(example = "1000.00")
        @NotNull
        @Positive
        BigDecimal amount,
        @Schema(description = "Дата и время траты без часового пояса (ISO-8601)", example = "2026-10-28T14:30:00")
        @NotNull
        LocalDateTime dateTime,
        @NotEmpty
        @Valid
        List<SplitRequest> splits
) {
}
