package dev.zhulidov.cash_tracker.transactions.model;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionSplitMapperTest {

    private final TransactionSplitMapper mapper = new TransactionSplitMapperImpl();

    private TransactionSplit split(Long id, String amount) {
        var category = Category.builder().id(10L).categoryName("Food").userId(1L).build();
        return TransactionSplit.builder().id(id).amount(new BigDecimal(amount)).category(category).build();
    }

    @Test
    void toDto_mapsAmountAndCategoryButNotParentTransaction() {
        var dto = mapper.toDto(split(1L, "100.00"));

        assertEquals(new BigDecimal("100.00"), dto.amount());
        assertEquals(10L, dto.categoryDto().id());
        assertEquals("Food", dto.categoryDto().categoryName());
        // обратная ссылка на транзакцию намеренно не мапится (иначе бесконечная рекурсия)
        assertNull(dto.transactionDto());
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
