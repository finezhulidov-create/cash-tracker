package dev.zhulidov.cash_tracker.transactions.importpdf;

import dev.zhulidov.cash_tracker.transactions.dto.ImportPreviewItem;
import dev.zhulidov.cash_tracker.transactions.dto.SplitRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionCreateRequest;
import dev.zhulidov.cash_tracker.transactions.model.ExistingTx;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DuplicateDetectorTest {

    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 10L;
    private static final LocalDateTime COFFEE_AT = LocalDateTime.of(2026, 10, 28, 14, 30);
    private static final LocalDateTime TAXI_AT = LocalDateTime.of(2026, 10, 29, 9, 15);

    @Mock
    private TransactionRepository repository;

    private DuplicateDetector detector;

    @BeforeEach
    void setUp() {
        detector = new DuplicateDetector(repository);
    }

    // ==================== fixtures ====================

    private record Existing(LocalDateTime dateTime, BigDecimal amount) implements ExistingTx {
        @Override
        public LocalDateTime getDateTime() {
            return dateTime;
        }

        @Override
        public BigDecimal getAmount() {
            return amount;
        }
    }

    private static ExistingTx inDb(LocalDateTime at, String amount) {
        return new Existing(at, new BigDecimal(amount));
    }

    private static TransactionCreateRequest op(LocalDateTime at, String amount) {
        BigDecimal sum = new BigDecimal(amount);
        return new TransactionCreateRequest("Операция", sum, at, List.of(new SplitRequest(CATEGORY_ID, sum)));
    }

    /** Подменяет базу; диапазон проверяется так же, как в запросе: dateTime >= from и dateTime < to. */
    private void databaseContains(ExistingTx... rows) {
        when(repository.findForDedup(eq(USER_ID), any(), any())).thenAnswer(invocation -> {
            LocalDateTime from = invocation.getArgument(1);
            LocalDateTime to = invocation.getArgument(2);
            return List.of(rows).stream()
                    .filter(row -> !row.getDateTime().isBefore(from) && row.getDateTime().isBefore(to))
                    .toList();
        });
    }

    private static List<Boolean> flags(List<ImportPreviewItem> preview) {
        return preview.stream().map(ImportPreviewItem::duplicate).toList();
    }

    // ==================== подсчёт ====================

    @Test
    void emptyDatabase_allOperationsAreNew() {
        databaseContains();

        var preview = detector.markDupl(List.of(op(COFFEE_AT, "350"), op(TAXI_AT, "1200")), USER_ID);

        assertEquals(List.of(false, false), flags(preview));
    }

    @Test
    void twoIdenticalPurchases_oneAlreadyInDatabase_onlyOneIsDuplicate() {
        databaseContains(inDb(COFFEE_AT, "350.00"));

        var preview = detector.markDupl(List.of(op(COFFEE_AT, "350"), op(COFFEE_AT, "350")), USER_ID);

        assertEquals(List.of(true, false), flags(preview));
    }

    @Test
    void twoIdenticalPurchases_noneInDatabase_bothAreNew() {
        databaseContains();

        var preview = detector.markDupl(List.of(op(COFFEE_AT, "350"), op(COFFEE_AT, "350")), USER_ID);

        assertEquals(List.of(false, false), flags(preview));
    }

    @Test
    void sameStatementImportedAgain_everythingIsDuplicate() {
        databaseContains(inDb(COFFEE_AT, "350.00"), inDb(COFFEE_AT, "350.00"), inDb(TAXI_AT, "1200.00"));

        var preview = detector.markDupl(
                List.of(op(COFFEE_AT, "350"), op(COFFEE_AT, "350"), op(TAXI_AT, "1200")), USER_ID);

        assertEquals(List.of(true, true, true), flags(preview));
    }

    @Test
    void overlappingStatements_onlyNewOperationsAreNew() {
        databaseContains(inDb(COFFEE_AT, "350.00"));

        var preview = detector.markDupl(List.of(op(COFFEE_AT, "350"), op(TAXI_AT, "1200")), USER_ID);

        assertEquals(List.of(true, false), flags(preview));
    }

    @Test
    void amountScaleAndSecondsDoNotMatter() {
        // в базе секунды и два знака, в выписке ни того, ни другого
        databaseContains(inDb(COFFEE_AT.withSecond(42), "350.00"));

        var preview = detector.markDupl(List.of(op(COFFEE_AT, "350")), USER_ID);

        assertEquals(List.of(true), flags(preview));
    }

    @Test
    void differentAmountOrMinute_isNotDuplicate() {
        databaseContains(inDb(COFFEE_AT, "350.00"));

        var preview = detector.markDupl(
                List.of(op(COFFEE_AT, "351"), op(COFFEE_AT.plusMinutes(1), "350")), USER_ID);

        assertEquals(List.of(false, false), flags(preview));
    }

    // ==================== границы запроса ====================

    @Test
    void latestOperationOfStatement_isFoundInDatabase() {
        // регресс: без plusMinutes(1) запрос «< to» не видел последнюю минуту выписки
        databaseContains(inDb(TAXI_AT.withSecond(30), "1200.00"));

        var preview = detector.markDupl(List.of(op(COFFEE_AT, "350"), op(TAXI_AT, "1200")), USER_ID);

        assertEquals(List.of(false, true), flags(preview));
    }

    @Test
    void singleOperationStatement_isFoundInDatabase() {
        // регресс: при одной операции from == to, и диапазон был пустым
        databaseContains(inDb(COFFEE_AT, "350.00"));

        var preview = detector.markDupl(List.of(op(COFFEE_AT, "350")), USER_ID);

        assertEquals(List.of(true), flags(preview));
    }

    @Test
    void queryRange_coversWholeMinutesOfUnsortedStatement() {
        when(repository.findForDedup(anyLong(), any(), any())).thenReturn(List.of());

        // банки часто печатают выписку от новых к старым
        detector.markDupl(List.of(op(TAXI_AT.withSecond(10), "1200"), op(COFFEE_AT.withSecond(50), "350")), USER_ID);

        verify(repository).findForDedup(USER_ID, COFFEE_AT, TAXI_AT.plusMinutes(1));
    }

    @Test
    void emptyStatement_returnsEmptyListWithoutQuery() {
        assertEquals(List.of(), detector.markDupl(List.of(), USER_ID));

        verifyNoInteractions(repository);
    }

    // ==================== содержимое предпросмотра ====================

    @Test
    void preview_keepsOrderAndFieldsOfOperations() {
        databaseContains();
        var coffee = op(COFFEE_AT, "350");
        var taxi = op(TAXI_AT, "1200");

        var preview = detector.markDupl(List.of(taxi, coffee), USER_ID);

        assertEquals(2, preview.size());
        assertEquals(taxi.dateTime(), preview.get(0).dateTime());
        assertEquals(taxi.amount(), preview.get(0).amount());
        assertEquals(taxi.description(), preview.get(0).description());
        assertEquals(taxi.splits(), preview.get(0).splits());
        assertEquals(coffee.dateTime(), preview.get(1).dateTime());
    }
}
