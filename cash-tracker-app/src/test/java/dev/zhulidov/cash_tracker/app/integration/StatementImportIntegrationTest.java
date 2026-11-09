package dev.zhulidov.cash_tracker.app.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import dev.zhulidov.cash_tracker.app.AbstractIntegrationTest;
import dev.zhulidov.cash_tracker.transactions.dto.ParsedOperations;
import dev.zhulidov.cash_tracker.transactions.dto.ParsedOperations.OneOperation;
import dev.zhulidov.cash_tracker.transactions.importpdf.AiParser;
import dev.zhulidov.cash_tracker.transactions.importpdf.StatementParsingException;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Импорт выписки целиком: настоящий PDF → PDFBox → (подменённая) модель → дедупликация по базе → сохранение.
 * Подменяется только AiParser, чтобы не ходить в LM Studio; всё остальное — настоящие Postgres, Redis и JWT.
 * База общая и не откатывается, поэтому у каждого теста свой пользователь.
 */
class StatementImportIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "secret-password-1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private AiParser aiParser;

    // ==================== helpers ====================

    private RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    private String registerUser() throws Exception {
        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "import-" + UUID.randomUUID() + "@test.com",
                                "username", "Importer",
                                "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static MockMultipartFile statementPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText("Statement");
                content.endText();
            }
            document.save(out);
            return new MockMultipartFile("file", "statement.pdf", "application/pdf", out.toByteArray());
        }
    }

    /** Что «вернёт модель» для любой страницы. */
    private void modelReturns(OneOperation... operations) {
        when(aiParser.parse(anyString())).thenReturn(new ParsedOperations(List.of(operations)));
    }

    private String parse(String token) throws Exception {
        return mockMvc.perform(multipart("/v1/parsing").file(statementPdf()).with(bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Превращает предпросмотр в тело /save: пользователь сохраняет все операции, ничего не меняя. */
    private String saveBodyFrom(String preview) throws Exception {
        List<Map<String, Object>> items = JsonPath.read(preview, "$");
        List<Map<String, Object>> transactions = items.stream().map(item -> {
            Map<String, Object> transaction = new LinkedHashMap<>(item);
            transaction.remove("duplicate");
            return transaction;
        }).toList();
        return objectMapper.writeValueAsString(Map.of("transactions", transactions));
    }

    private long transactionCount(String token) throws Exception {
        String body = mockMvc.perform(get("/v1/transactions").with(bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.page.totalElements")).longValue();
    }

    private static long categoryIdOf(String preview, int index) {
        return ((Number) JsonPath.read(preview, "$[" + index + "].splits[0].categoryId")).longValue();
    }

    // ==================== сценарии ====================

    @Test
    void fullFlow_parseSaveAndReimportMarksEverythingAsDuplicate() throws Exception {
        String token = registerUser();
        modelReturns(
                new OneOperation("28.10.2026 14:30", "Coffee", "350,00"),
                new OneOperation("28.10.2026 14:30", "Coffee", "350,00"),  // вторая такая же покупка в ту же минуту
                new OneOperation("28.10.2026 10:00", "Salary", "+50 000,00"),
                new OneOperation("29.10.2026 09:15", "Taxi", "1 200,00"));

        String preview = parse(token);

        // поступление отброшено, две одинаковые покупки не склеились, в базе пока ничего нет
        assertEquals(3, intAt(preview, "$.length()"));
        assertEquals(List.of(false, false, false), JsonPath.read(preview, "$[*].duplicate"));
        assertEquals(List.of("Coffee", "Coffee", "Taxi"), JsonPath.read(preview, "$[*].description"));

        // для операций создана категория «Прочее» этого пользователя
        var category = categoryRepository.findById(categoryIdOf(preview, 0)).orElseThrow();
        assertEquals("Прочее", category.getCategoryName());

        mockMvc.perform(post("/v1/parsing/save").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBodyFrom(preview)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].dateTime").value("2026-10-28T14:30:00"))
                .andExpect(jsonPath("$[0].splits[0].category.categoryName").value("Прочее"));
        assertEquals(3, transactionCount(token));

        // та же выписка ещё раз: всё уже есть, включая обе одинаковые покупки
        String again = parse(token);

        assertEquals(List.of(true, true, true), JsonPath.read(again, "$[*].duplicate"));
        // категория переиспользована, а не создана второй раз
        assertEquals(category.getId(), categoryIdOf(again, 0));
        assertEquals(1, categoryRepository.findAllByUserId(category.getUserId()).stream()
                .filter(c -> c.getCategoryName().equals("Прочее")).count());
    }

    @Test
    void overlappingStatement_onlyNewOperationsAreNew() throws Exception {
        String token = registerUser();
        modelReturns(new OneOperation("28.10.2026 14:30", "Coffee", "350,00"));
        String first = parse(token);
        mockMvc.perform(post("/v1/parsing/save").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBodyFrom(first)))
                .andExpect(status().isOk());

        // вторая выписка содержит ту же покупку (в базе — без секунд, в выписке — с секундами) и одну новую
        modelReturns(
                new OneOperation("28.10.2026 14:30:40", "Coffee", "350"),
                new OneOperation("28.10.2026 14:30", "Coffee", "350,00"),
                new OneOperation("30.10.2026 19:00", "Cinema", "800,00"));

        String second = parse(token);

        assertEquals(List.of(true, false, false), JsonPath.read(second, "$[*].duplicate"));
    }

    @Test
    void otherUsersTransactions_areNotDuplicates() throws Exception {
        String alice = registerUser();
        String bob = registerUser();
        modelReturns(new OneOperation("28.10.2026 14:30", "Coffee", "350,00"));
        mockMvc.perform(post("/v1/parsing/save").with(bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBodyFrom(parse(alice))))
                .andExpect(status().isOk());

        String bobsPreview = parse(bob);

        assertEquals(List.of(false), JsonPath.read(bobsPreview, "$[*].duplicate"));
    }

    @Test
    void save_isAllOrNothing() throws Exception {
        String alice = registerUser();
        String bob = registerUser();
        modelReturns(new OneOperation("28.10.2026 14:30", "Coffee", "350,00"));
        long bobsCategory = categoryIdOf(parse(bob), 0);
        long alicesCategory = categoryIdOf(parse(alice), 0);

        // первая операция корректна, вторая ссылается на чужую категорию
        String body = objectMapper.writeValueAsString(Map.of("transactions", List.of(
                Map.of("description", "Coffee", "amount", 350.00, "dateTime", "2026-10-28T14:30:00",
                        "splits", List.of(Map.of("categoryId", alicesCategory, "amount", 350.00))),
                Map.of("description", "Stolen", "amount", 100.00, "dateTime", "2026-10-28T15:00:00",
                        "splits", List.of(Map.of("categoryId", bobsCategory, "amount", 100.00))))));

        mockMvc.perform(post("/v1/parsing/save").with(bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        // первая операция откатилась вместе со второй
        assertEquals(0, transactionCount(alice));
        assertEquals(0, transactionCount(bob));
    }

    @Test
    void parse_notPdf_returns400() throws Exception {
        String token = registerUser();
        var notPdf = new MockMultipartFile("file", "statement.pdf", "application/pdf",
                "definitely not a pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/v1/parsing").file(notPdf).with(bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void parse_modelUnavailable_returns503AndSavesNothing() throws Exception {
        String token = registerUser();
        when(aiParser.parse(anyString()))
                .thenThrow(StatementParsingException.modelUnavailable(new RuntimeException("connection refused")));

        mockMvc.perform(multipart("/v1/parsing").file(statementPdf()).with(bearer(token)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("IMPORT_MODEL_UNAVAILABLE"));

        assertEquals(0, transactionCount(token));
    }

    @Test
    void parse_withoutToken_returns401() throws Exception {
        mockMvc.perform(multipart("/v1/parsing").file(statementPdf()))
                .andExpect(status().isUnauthorized());
    }

    private static int intAt(String json, String path) {
        return ((Number) JsonPath.read(json, path)).intValue();
    }
}
