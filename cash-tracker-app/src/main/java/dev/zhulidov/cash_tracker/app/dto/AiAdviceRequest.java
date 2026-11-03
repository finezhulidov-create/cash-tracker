package dev.zhulidov.cash_tracker.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;

@Schema(description = "За какой месяц нужен совет")
public record AiAdviceRequest(
        @Schema(description = "Месяц в формате yyyy-MM, не в будущем", type = "string", example = "2026-10")
        @NotNull
        YearMonth yearMonth
) {
}
