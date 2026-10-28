package dev.zhulidov.cash_tracker.transactions.model;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionMapperTest {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 10, 5, 12, 30);

    private final TransactionMapper mapper = new TransactionMapperImpl();

    private Transaction transaction(Long id) {
        return Transaction.builder().id(id).userId(1L).amount(new BigDecimal("300.00"))
                .description("Supermarket").dateTime(WHEN).build();
    }

    @Test
    void toDto_mapsAllScalarFields() {
        var dto = mapper.toDto(transaction(100L));

        assertEquals(100L, dto.id());
        assertEquals(1L, dto.userId());
        assertEquals(new BigDecimal("300.00"), dto.amount());
        assertEquals("Supermarket", dto.description());
        assertEquals(WHEN, dto.dateTime());
    }

    @Test
    void toDto_whenTransactionIsNull_returnsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toDtoList_mapsEverySplitAmount() {
        var splits = List.of(
                TransactionSplit.builder().id(1L).amount(new BigDecimal("100")).build(),
                TransactionSplit.builder().id(2L).amount(new BigDecimal("200")).build());

        var result = mapper.toDtoList(splits);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("200"), result.get(1).amount());
    }

    @Test
    void toDtoPage_mapsContentAndKeepsPaginationMetadata() {
        var page = new PageImpl<>(List.of(transaction(1L), transaction(2L)), PageRequest.of(0, 2), 7);

        var result = mapper.toDtoPage(page);

        assertEquals(2, result.getContent().size());
        assertEquals(2L, result.getContent().get(1).id());
        assertEquals(7, result.getTotalElements());
    }
}
