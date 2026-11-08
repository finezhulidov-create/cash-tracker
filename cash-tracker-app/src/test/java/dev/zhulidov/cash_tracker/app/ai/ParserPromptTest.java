package dev.zhulidov.cash_tracker.app.ai;

import dev.zhulidov.cash_tracker.transactions.dto.ParsedOperations;
import dev.zhulidov.cash_tracker.transactions.importpdf.ParserPrompt;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** Промпт лежит в ресурсах модуля app, поэтому и тест здесь: из модуля transactions его не видно. */
class ParserPromptTest {

    @Test
    void promptIsLoadedFromClasspath() {
        String text = new ParserPrompt().text();

        assertFalse(text.isBlank());
        assertEquals(text.strip(), text);
    }

    @Test
    void promptNamesEveryFieldOfModelAnswer() {
        // если поле в промпте и в DTO называется по-разному, Jackson молча оставит его null
        String text = new ParserPrompt().text();

        Arrays.stream(ParsedOperations.OneOperation.class.getRecordComponents())
                .map(component -> component.getName())
                .forEach(field -> assertTrue(text.contains(field + ":"),
                        "в промпте нет поля «" + field + "» из ParsedOperations.OneOperation"));
    }
}
