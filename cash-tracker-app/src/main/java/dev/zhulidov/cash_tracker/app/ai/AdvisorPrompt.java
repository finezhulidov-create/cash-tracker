package dev.zhulidov.cash_tracker.app.ai;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Системный промпт советника: лежит в resources/ai/advisor-prompt.md, чтобы его можно было править без Java. */
@Component
public class AdvisorPrompt {

    private static final String RESOURCE = "ai/advisor-prompt.md";

    private final String text;

    public AdvisorPrompt() {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            this.text = new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + RESOURCE, e);
        }
    }

    public String text() {
        return text;
    }
}
