package dev.zhulidov.cash_tracker.transactions.model;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionMapperTest {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 10, 5, 12, 30);

    private final TransactionMapper mapper = newMapper();

    /** Вложенные мапперы в Spring внедряются в поля; без контекста собираем цепочку вручную. */
    private static TransactionMapper newMapper() {
        var splitMapper = new TransactionSplitMapperImpl();
        ReflectionTestUtils.setField(splitMapper, "categoryMapper", new CategoryMapperImpl());
        var mapper = new TransactionMapperImpl();
        ReflectionTestUtils.setField(mapper, "transactionSplitMapper", splitMapper);
        return mapper;
    }

    private Transaction transaction(Long id) {
        var category = Category.builder().id(10L).categoryName("Food").userId(1L).build();
        var trans = Transaction.builder().id(id).userId(1L).amount(new BigDecimal("300.00"))
                .description("Supermarket").dateTime(WHEN).build();
        trans.setSplits(List.of(
                TransactionSplit.builder().id(1L).transaction(trans).category(category).amount(new BigDecimal("100.00")).build(),
                TransactionSplit.builder().id(2L).transaction(trans).category(category).amount(new BigDecimal("200.00")).build()));
        return trans;
    }

    @Test
    void toDto_mapsScalarFieldsAndSplitsWithCategories() {
        var dto = mapper.toDto(transaction(100L));

        assertEquals(100L, dto.id());
        assertEquals(1L, dto.userId());
        assertEquals(new BigDecimal("300.00"), dto.amount());
        assertEquals("Supermarket", dto.description());
        assertEquals(WHEN, dto.dateTime());
        assertEquals(2, dto.splits().size());
        assertEquals(new BigDecimal("200.00"), dto.splits().get(1).amount());
        assertEquals(100L, dto.splits().get(0).transactionId());
        assertEquals("Food", dto.splits().get(0).category().categoryName());
    }

    @Test
    void toDto_whenTransactionIsNull_returnsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toDtoPage_mapsContentAndKeepsPaginationMetadata() {
        var page = new PageImpl<>(List.of(transaction(1L), transaction(2L)), PageRequest.of(0, 2), 7);

        var result = mapper.toDtoPage(page);

        assertEquals(2, result.getContent().size());
        assertEquals(2L, result.getContent().get(1).id());
        assertEquals(2, result.getContent().get(1).splits().size());
        assertEquals(7, result.getTotalElements());
    }
}
