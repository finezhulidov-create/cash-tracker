package dev.zhulidov.cash_tracker.transactions.importpdf;

import dev.zhulidov.cash_tracker.transactions.dto.ParsedOperations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

@Service
public class AiParser {

    private final ChatClient chatClient;

    public AiParser(ChatClient.Builder builder, ParserPrompt parserPrompt) {
        this.chatClient = builder
                .defaultSystem(parserPrompt.text())
                .build();
    }

    /**
     * Отдаёт модели текст одной страницы выписки.
     *
     * @throws StatementParsingException модель недоступна (нет соединения, таймаут, ошибка сервера модели)
     *                                   или ответила не тем JSON
     */
    public ParsedOperations parse(String page) {
        try {
            return chatClient.prompt()
                    .user(page)
                    .call()
                    .entity(ParsedOperations.class);
        } catch (ResourceAccessException | TransientAiException | NonTransientAiException e) {
            // нет соединения или таймаут; сервер модели ответил 5xx; 4xx — например, модель не загружена
            throw StatementParsingException.modelUnavailable(e);
        } catch (StatementParsingException e) {
            throw e;
        } catch (RuntimeException e) {
            // ответ пришёл, но не разбирается в ParsedOperations
            throw StatementParsingException.badResponse(e);
        }
    }
}
