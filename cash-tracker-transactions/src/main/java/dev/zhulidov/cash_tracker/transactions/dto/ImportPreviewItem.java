package dev.zhulidov.cash_tracker.transactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;



import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ImportPreviewItem(
        @Schema(description = "Описание", example = "Супермаркет")

        String description,
        @Schema(description = "Общая сумма траты, больше нуля", example = "1000.00")

        BigDecimal amount,
        @Schema(description = "Дата и время траты без часового пояса (ISO-8601)",
                example = "2026-10-28T14:30:00")
        LocalDateTime dateTime,
        @Schema(description = "Разбивка по категориям, минимум одна часть")

        List<SplitRequest> splits,
        @Schema(description = "Маркер дублей")
        boolean duplicate
) {
}
