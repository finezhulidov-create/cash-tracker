package dev.zhulidov.cash_tracker.transactions.importpdf;

import dev.zhulidov.cash_tracker.common.exception.InvalidRequestException;
import dev.zhulidov.cash_tracker.transactions.dto.CategoryCreateRequestDto;
import dev.zhulidov.cash_tracker.transactions.dto.CategoryDto;
import dev.zhulidov.cash_tracker.transactions.dto.ImportPreviewItem;
import dev.zhulidov.cash_tracker.transactions.dto.ParsedOperations;
import dev.zhulidov.cash_tracker.transactions.dto.ParsedOperations.OneOperation;
import dev.zhulidov.cash_tracker.transactions.dto.SplitRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionCreateRequest;
import dev.zhulidov.cash_tracker.transactions.model.Category;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.transactions.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIParsingServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long DEFAULT_CATEGORY_ID = 10L;
    private static final String DEFAULT_CATEGORY = "Прочее";
    private static final String FALLBACK_DESCRIPTION = "Операция из выписки";

    @Mock
    private AiParser parser;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private PdfImportService importService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private DuplicateDetector detector;

    @Captor
    private ArgumentCaptor<List<TransactionCreateRequest>> requestsCaptor;

    private AIParsingService service;

    private final MultipartFile file = new MockMultipartFile("file", "statement.pdf", "application/pdf", new byte[]{1});

    @BeforeEach
    void setUp() {
        service = new AIParsingService(parser, categoryRepository, importService, categoryService, detector);
        lenient().when(categoryRepository.findFirstByUserIdAndCategoryName(USER_ID, DEFAULT_CATEGORY))
                .thenReturn(Optional.of(Category.builder()
                        .id(DEFAULT_CATEGORY_ID).categoryName(DEFAULT_CATEGORY).userId(USER_ID).build()));
        lenient().when(detector.markDupl(anyList(), any())).thenReturn(List.of());
    }

    // ==================== fixtures ====================

    private static OneOperation operation(String dateTime, String description, String amount) {
        return new OneOperation(dateTime, description, amount);
    }

    /** Каждая страница PDF отдаёт свой ответ модели. */
    private void statementPages(ParsedOperations... answers) {
        List<String> pages = new ArrayList<>();
        for (int i = 0; i < answers.length; i++) {
            pages.add("page " + i);
            when(parser.parse("page " + i)).thenReturn(answers[i]);
        }
        when(importService.extractText(file)).thenReturn(pages);
    }

    private static ParsedOperations answer(OneOperation... operations) {
        return new ParsedOperations(Arrays.asList(operations));
    }

    private List<TransactionCreateRequest> requestsSentToDetector() {
        service.generateTransactions(file, USER_ID);
        verify(detector).markDupl(requestsCaptor.capture(), eq(USER_ID));
        return requestsCaptor.getValue();
    }

    // ==================== разбор операций ====================

    @Test
    void expense_becomesRequestWithSingleSplitForWholeAmount() {
        statementPages(answer(operation("28.10.2026 14:30", "Кофейня", "-350,00 ₽")));

        var requests = requestsSentToDetector();

        assertEquals(1, requests.size());
        var request = requests.get(0);
        assertEquals("Кофейня", request.description());
        assertEquals(new BigDecimal("350.00"), request.amount());
        assertEquals(LocalDateTime.of(2026, 10, 28, 14, 30), request.dateTime());
        assertEquals(List.of(new SplitRequest(DEFAULT_CATEGORY_ID, new BigDecimal("350.00"))), request.splits());
    }

    @Test
    void operationsFromAllPages_areCollectedInOrder() {
        statementPages(
                answer(operation("28.10.2026 14:30", "Кофейня", "350,00")),
                answer(operation("29.10.2026 09:15", "Такси", "1 200,00")));

        var requests = requestsSentToDetector();

        assertEquals(List.of("Кофейня", "Такси"), requests.stream().map(TransactionCreateRequest::description).toList());
    }

    @Test
    void income_isSkipped() {
        statementPages(answer(
                operation("28.10.2026 10:00", "Зарплата", "+50 000,00"),
                operation("28.10.2026 14:30", "Кофейня", "350,00")));

        var requests = requestsSentToDetector();

        assertEquals(List.of("Кофейня"), requests.stream().map(TransactionCreateRequest::description).toList());
    }

    @Test
    void operationsWithUnparsableAmountOrDate_areSkipped() {
        statementPages(answer(
                operation("28.10.2026 14:30", "Сумма-мусор", "сто рублей"),
                operation("31.02.2026 14:30", "Дата-мусор", "350,00"),
                operation(null, "Без даты", "350,00"),
                operation("28.10.2026 14:30", "Без суммы", null),
                operation("28.10.2026 14:30", "Кофейня", "350,00")));

        var requests = requestsSentToDetector();

        assertEquals(List.of("Кофейня"), requests.stream().map(TransactionCreateRequest::description).toList());
    }

    @Test
    void brokenModelAnswers_doNotFailImport() {
        // страница без ответа, ответ без списка и null внутри списка — всё это LLM иногда присылает
        statementPages(
                null,
                new ParsedOperations(null),
                answer(null, operation("28.10.2026 14:30", "Кофейня", "350,00")));

        var requests = requestsSentToDetector();

        assertEquals(1, requests.size());
    }

    @Test
    void nothingToImport_detectorGetsEmptyList() {
        statementPages(answer(operation("28.10.2026 10:00", "Зарплата", "+50 000,00")));

        var requests = requestsSentToDetector();

        assertTrue(requests.isEmpty());
    }

    // ==================== описание ====================

    @Test
    void description_isTrimmed() {
        statementPages(answer(operation("28.10.2026 14:30", "   Кофейня  ", "350,00")));

        assertEquals("Кофейня", requestsSentToDetector().get(0).description());
    }

    @Test
    void missingOrBlankDescription_getsFallback() {
        statementPages(answer(
                operation("28.10.2026 14:30", null, "350,00"),
                operation("28.10.2026 14:31", "   ", "350,00")));

        var requests = requestsSentToDetector();

        assertEquals(FALLBACK_DESCRIPTION, requests.get(0).description());
        assertEquals(FALLBACK_DESCRIPTION, requests.get(1).description());
    }

    @Test
    void longDescription_fitsColumnLimit() {
        statementPages(answer(operation("28.10.2026 14:30", "а".repeat(400), "350,00")));

        String description = requestsSentToDetector().get(0).description();

        assertTrue(description.length() <= 255, "длина " + description.length());
        assertFalse(description.isBlank());
    }

    // ==================== категория по умолчанию ====================

    @Test
    void existingDefaultCategory_isReused() {
        statementPages(answer(operation("28.10.2026 14:30", "Кофейня", "350,00")));

        var requests = requestsSentToDetector();

        assertEquals(DEFAULT_CATEGORY_ID, requests.get(0).splits().get(0).categoryId());
        verifyNoInteractions(categoryService);
    }

    @Test
    void missingDefaultCategory_isCreatedOnceAndItsIdIsUsed() {
        when(categoryRepository.findFirstByUserIdAndCategoryName(USER_ID, DEFAULT_CATEGORY)).thenReturn(Optional.empty());
        when(categoryService.createCategory(new CategoryCreateRequestDto(DEFAULT_CATEGORY), USER_ID))
                .thenReturn(new CategoryDto(77L, DEFAULT_CATEGORY, USER_ID));
        statementPages(answer(
                operation("28.10.2026 14:30", "Кофейня", "350,00"),
                operation("29.10.2026 09:15", "Такси", "1200,00")));

        var requests = requestsSentToDetector();

        // регресс: id созданной категории терялся, и сплит уходил с categoryId = null
        assertEquals(77L, requests.get(0).splits().get(0).categoryId());
        assertEquals(77L, requests.get(1).splits().get(0).categoryId());
        verify(categoryService, times(1)).createCategory(any(), eq(USER_ID));
    }

    // ==================== связка с другими частями ====================

    @Test
    void returnsWhatDuplicateDetectorReturned() {
        statementPages(answer(operation("28.10.2026 14:30", "Кофейня", "350,00")));
        var preview = List.of(new ImportPreviewItem("Кофейня", new BigDecimal("350.00"),
                LocalDateTime.of(2026, 10, 28, 14, 30), List.of(), true));
        when(detector.markDupl(anyList(), eq(USER_ID))).thenReturn(preview);

        assertSame(preview, service.generateTransactions(file, USER_ID));
    }

    // ==================== сбои модели ====================

    @Test
    void unusablePage_isSkippedAndOtherPagesAreImported() {
        when(importService.extractText(file)).thenReturn(List.of("page 0", "page 1"));
        when(parser.parse("page 0")).thenThrow(StatementParsingException.badResponse(new IllegalStateException("not json")));
        when(parser.parse("page 1")).thenReturn(answer(operation("29.10.2026 09:15", "Такси", "1200,00")));

        var requests = requestsSentToDetector();

        assertEquals(List.of("Такси"), requests.stream().map(TransactionCreateRequest::description).toList());
    }

    @Test
    void allPagesUnusable_isBadResponse() {
        when(importService.extractText(file)).thenReturn(List.of("page 0", "page 1"));
        when(parser.parse(any())).thenThrow(StatementParsingException.badResponse(new IllegalStateException("not json")));

        var e = assertThrows(StatementParsingException.class, () -> service.generateTransactions(file, USER_ID));

        assertEquals(StatementParsingException.BAD_RESPONSE, e.getErrorCode());
        verifyNoInteractions(detector);
    }

    @Test
    void unavailableModel_stopsImportAtFirstPage() {
        when(importService.extractText(file)).thenReturn(List.of("page 0", "page 1", "page 2"));
        when(parser.parse("page 0")).thenThrow(StatementParsingException.modelUnavailable(new RuntimeException("refused")));

        var e = assertThrows(StatementParsingException.class, () -> service.generateTransactions(file, USER_ID));

        assertTrue(e.isModelUnavailable());
        // остальные страницы не отправляются: модель всё равно не ответит
        verify(parser, times(1)).parse(any());
        verifyNoInteractions(detector, categoryService);
    }

    @Test
    void notPdf_failsBeforeCallingModel() {
        when(importService.extractText(file)).thenThrow(new InvalidRequestException("Поддержка только пдф файлов"));

        assertThrows(InvalidRequestException.class, () -> service.generateTransactions(file, USER_ID));

        verifyNoInteractions(parser, detector, categoryService);
    }
}
