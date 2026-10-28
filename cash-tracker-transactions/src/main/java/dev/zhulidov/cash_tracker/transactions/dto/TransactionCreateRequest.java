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

@Schema(description = "Новая трата с разбивкой по категориям. Сумма частей должна равняться amount")
public record TransactionCreateRequest(
        @Schema(description = "Описание", example = "Супермаркет")
        @NotBlank
        @Size(max = 255)
        String description,
        @Schema(description = "Общая сумма траты, больше нуля", example = "1000.00")
        @NotNull
        @Positive
        BigDecimal amount,
        @Schema(description = "Дата и время траты без часового пояса (ISO-8601). Не указано: текущее время сервера в UTC",
                example = "2026-10-28T14:30:00")
        LocalDateTime dateTime,
        @Schema(description = "Разбивка по категориям, минимум одна часть")
        @NotEmpty
        @Valid
        List<SplitRequest> splits
) {
}
