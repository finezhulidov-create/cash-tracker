package dev.zhulidov.cash_tracker.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Доступен ли ИИ-советник на этом сервере")
public record AiStatusResponse(
        @Schema(description = "false, если администратор не включил функцию и не задал ключ GigaChat")
        boolean enabled
) {
}
