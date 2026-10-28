package dev.zhulidov.cash_tracker.transactions.model;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionSplitMapperTest {

    private final TransactionSplitMapper mapper = newMapper();

    /** Вложенный CategoryMapper в Spring внедряется в поле; без контекста задаём его вручную. */
    private static TransactionSplitMapper newMapper() {
        var mapper = new TransactionSplitMapperImpl();
        ReflectionTestUtils.setField(mapper, "categoryMapper", new CategoryMapperImpl());
        return mapper;
    }

    private TransactionSplit split(Long id, String amount) {
        var category = Category.builder().id(10L).categoryName("Food").userId(1L).build();
        var transaction = Transaction.builder().id(100L).userId(1L).build();
        return TransactionSplit.builder().id(id).amount(new BigDecimal(amount))
                .category(category).transaction(transaction).build();
    }

    @Test
    void toDto_mapsIdAmountCategoryAndParentTransactionId() {
        var dto = mapper.toDto(split(5L, "100.00"));

        assertEquals(5L, dto.id());
        assertEquals(100L, dto.transactionId());
        assertEquals(new BigDecimal("100.00"), dto.amount());
        assertEquals(10L, dto.category().id());
        assertEquals("Food", dto.category().categoryName());
    }

    @Test
    void toDto_whenTransactionIsNotSet_leavesTransactionIdEmpty() {
        var withoutTransaction = TransactionSplit.builder().id(5L).amount(BigDecimal.TEN).build();

        var dto = mapper.toDto(withoutTransaction);

        assertNull(dto.transactionId());
        assertNull(dto.category());
    }

    @Test
    void toDto_whenSplitIsNull_returnsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toDtoPage_mapsContentAndKeepsPaginationMetadata() {
        var page = new PageImpl<>(List.of(split(1L, "100"), split(2L, "200")), PageRequest.of(1, 2), 6);

        var result = mapper.toDtoPage(page);

        assertEquals(2, result.getContent().size());
        assertEquals(new BigDecimal("200"), result.getContent().get(1).amount());
        assertEquals(6, result.getTotalElements());
        assertEquals(1, result.getNumber());
    }
}
