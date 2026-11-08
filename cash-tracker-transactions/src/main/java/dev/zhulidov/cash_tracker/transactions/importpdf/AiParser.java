package dev.zhulidov.cash_tracker.transactions.importpdf;

import dev.zhulidov.cash_tracker.transactions.dto.ParsedOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service

public class AiParser {

    private final ParserPrompt prompt;

    private final ChatClient chatClient;

    public AiParser(ChatClient.Builder builder, ParserPrompt parserPrompt) {
        this.prompt = parserPrompt;
        this.chatClient = builder
                .defaultSystem(prompt.text())
                .build();
    }

    public ParsedOperations parse(String pdf){
        return chatClient.prompt()
                .user(pdf)
                .call()
                .entity(ParsedOperations.class);
    }
}
