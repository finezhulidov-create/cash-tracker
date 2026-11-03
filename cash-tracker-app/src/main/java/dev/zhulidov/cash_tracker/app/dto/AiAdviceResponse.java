package dev.zhulidov.cash_tracker.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Анализ трат месяца и советы от ИИ")
public record AiAdviceResponse(
        @Schema(description = "Текст в формате Markdown: таблицы, заголовки, списки; HTML внутри нет")
        String text,
        @Schema(description = "Когда ответ был получен от модели (UTC)", example = "2026-10-31T10:05:00Z")
        Instant generatedAt,
        @Schema(description = "true, если ответ взят из кэша: данные месяца не менялись и платный запрос не делался")
        boolean cached
) {
}
