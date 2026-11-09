package dev.zhulidov.cash_tracker.app.controller;

import dev.zhulidov.cash_tracker.app.ai.MonthAdvisorService;
import dev.zhulidov.cash_tracker.app.dto.AiAdviceRequest;
import dev.zhulidov.cash_tracker.app.dto.AiAdviceResponse;
import dev.zhulidov.cash_tracker.app.dto.AiStatusResponse;
import dev.zhulidov.cash_tracker.common.dto.ErrorResponse;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI advisor", description = "Анализ трат месяца и советы от языковой модели (GigaChat). Необязательная функция")
public class AiController {

    private final MonthAdvisorService advisor;

    @Operation(summary = "Is the AI advisor available",
            description = "Включена ли функция на сервере. Фронтенд по ответу решает, показывать ли кнопку")
    @ApiResponse(responseCode = "200", description = "Статус")
    @GetMapping("/status")
    public ResponseEntity<AiStatusResponse> status() {
        return ResponseEntity.ok(new AiStatusResponse(advisor.isEnabled()));
    }

    @Operation(summary = "Analyse a month and get advice",
            description = "Отправляет модели сводку месяца текущего пользователя (суммы, бюджеты, названия категорий, " +
                    "без идентификаторов) и возвращает текст в Markdown. Платный запрос: для тех же данных ответ " +
                    "берётся из кэша, а число запросов на пользователя ограничено. Отвечает за десятки секунд")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Анализ и советы"),
            @ApiResponse(responseCode = "400", description = "Месяц в будущем или неверный формат",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "AI_BUSY: модель занята, или AI_RATE_LIMIT_EXCEEDED: " +
                    "исчерпан лимит запросов пользователя. Заголовок Retry-After — через сколько секунд повторить",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "AI_BAD_RESPONSE: модель вернула непригодный ответ",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "AI_DISABLED (функция не включена), AI_UNAVAILABLE, " +
                    "AI_AUTH_FAILED или AI_QUOTA_EXHAUSTED (сбой на стороне сервиса ИИ)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/advice")
    public ResponseEntity<AiAdviceResponse> advice(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestBody @Valid AiAdviceRequest request) {
        return ResponseEntity.ok(advisor.advise(principal.getId(), request.yearMonth()));
    }
}
