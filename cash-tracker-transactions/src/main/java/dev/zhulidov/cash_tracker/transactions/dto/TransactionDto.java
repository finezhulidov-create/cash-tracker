package dev.zhulidov.cash_tracker.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Трата с разбивкой по категориям")
public record TransactionDto(
        @Schema(example = "100") Long id,
        @Schema(example = "1") Long userId,
        @Schema(description = "Общая сумма, равна сумме частей", example = "1000.00") BigDecimal amount,
        @Schema(example = "Супермаркет") String description,
        @Schema(example = "2026-10-28T14:30:00") LocalDateTime dateTime,
        @Schema(description = "Разбивка по категориям") List<TransactionSplitDto> splits
) {
}
