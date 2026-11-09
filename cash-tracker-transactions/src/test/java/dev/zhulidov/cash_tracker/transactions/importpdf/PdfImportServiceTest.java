package dev.zhulidov.cash_tracker.transactions.importpdf;

import dev.zhulidov.cash_tracker.common.exception.InvalidRequestException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfImportServiceTest {

    private final PdfImportService service = new PdfImportService();

    /** Собирает PDF, где каждая строка массива — отдельная страница. */
    static byte[] pdf(String... pages) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (String text : pages) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(font, 12);
                    content.newLineAtOffset(50, 700);
                    content.showText(text);
                    content.endText();
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private static MockMultipartFile file(byte[] content) {
        return new MockMultipartFile("file", "statement.pdf", "application/pdf", content);
    }

    @Test
    void extractText_returnsOnePageOfTextPerPdfPage() throws IOException {
        List<String> pages = service.extractText(file(pdf("28.10.2026 14:30 Coffee 350,00", "29.10.2026 09:15 Taxi 1200,00")));

        assertEquals(2, pages.size());
        assertTrue(pages.get(0).contains("Coffee"), pages.get(0));
        assertFalse(pages.get(0).contains("Taxi"), "страницы не должны смешиваться");
        assertTrue(pages.get(1).contains("Taxi"), pages.get(1));
    }

    @Test
    void extractText_notPdf_isInvalidRequest() {
        var notPdf = file("just some text, not a pdf".getBytes(StandardCharsets.UTF_8));

        assertThrows(InvalidRequestException.class, () -> service.extractText(notPdf));
    }

    @Test
    void extractText_emptyFile_isInvalidRequest() {
        assertThrows(InvalidRequestException.class, () -> service.extractText(file(new byte[0])));
    }
}
