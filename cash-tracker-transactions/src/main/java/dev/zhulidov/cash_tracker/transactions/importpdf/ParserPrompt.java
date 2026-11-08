package dev.zhulidov.cash_tracker.transactions.importpdf;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
public class ParserPrompt {

    private final String RESOURCE = "ai/parser-prompt.md";

    private final String text;

    public ParserPrompt(){
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()){
            this.text = new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read resource "+ RESOURCE, e);
        }
    }

    public String text(){
        return text;
    }
}
