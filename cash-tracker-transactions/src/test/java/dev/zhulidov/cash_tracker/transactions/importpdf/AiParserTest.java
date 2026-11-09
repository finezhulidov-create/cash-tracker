package dev.zhulidov.cash_tracker.transactions.importpdf;

import dev.zhulidov.cash_tracker.transactions.dto.ParsedOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AiParserTest {

    private ChatClient chatClient;
    private AiParser parser;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ParserPrompt prompt = mock(ParserPrompt.class);
        when(prompt.text()).thenReturn("system prompt");
        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        parser = new AiParser(builder, prompt);
    }

    private void modelAnswers(ParsedOperations answer) {
        when(chatClient.prompt().user(anyString()).call().entity(ParsedOperations.class)).thenReturn(answer);
    }

    private void modelFails(RuntimeException failure) {
        when(chatClient.prompt().user(anyString()).call().entity(ParsedOperations.class)).thenThrow(failure);
    }

    @Test
    void returnsParsedAnswer() {
        var answer = new ParsedOperations(List.of(new ParsedOperations.OneOperation("28.10.2026 14:30", "Кофейня", "350,00")));
        modelAnswers(answer);

        assertSame(answer, parser.parse("page text"));
    }

    @Test
    void connectionProblem_isModelUnavailable() {
        // так выглядит и отказ в соединении, и таймаут: адрес LM Studio неверный или сервер выключен
        modelFails(new ResourceAccessException("I/O error", new ConnectException("Connection refused")));

        var e = assertThrows(StatementParsingException.class, () -> parser.parse("page text"));

        assertTrue(e.isModelUnavailable());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
        assertEquals(StatementParsingException.MODEL_UNAVAILABLE, e.getErrorCode());
    }

    @Test
    void modelServerErrors_areModelUnavailable() {
        modelFails(new TransientAiException("500 - model crashed"));
        assertTrue(assertThrows(StatementParsingException.class, () -> parser.parse("p")).isModelUnavailable());
    }

    @Test
    void modelNotLoaded_isModelUnavailable() {
        modelFails(new NonTransientAiException("404 - model not found"));
        assertTrue(assertThrows(StatementParsingException.class, () -> parser.parse("p")).isModelUnavailable());
    }

    @Test
    void answerThatIsNotJson_isBadResponse() {
        modelFails(new IllegalStateException("Could not parse the given text to the desired target type"));

        var e = assertThrows(StatementParsingException.class, () -> parser.parse("page text"));

        assertFalse(e.isModelUnavailable());
        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
        assertEquals(StatementParsingException.BAD_RESPONSE, e.getErrorCode());
    }

    @Test
    void errorMessageDoesNotLeakDetails() {
        modelFails(new ResourceAccessException("I/O error on POST request for \"http://192.168.31.14:1234/v1/chat/completions\""));

        var e = assertThrows(StatementParsingException.class, () -> parser.parse("page text"));

        assertFalse(e.getMessage().contains("192.168"));
    }
}
