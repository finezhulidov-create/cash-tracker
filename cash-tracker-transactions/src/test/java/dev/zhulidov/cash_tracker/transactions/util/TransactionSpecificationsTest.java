package dev.zhulidov.cash_tracker.transactions.util;

import dev.zhulidov.cash_tracker.transactions.model.Transaction;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Здесь проверяются только ветки, которые решают, нужен ли фильтр вообще.
 * Сами предикаты (join по сплитам, диапазоны) проверяются на настоящей БД в TransactionSpecificationsIT.
 */
class TransactionSpecificationsTest {

    @Test
    void hasCategoryId_whenNull_returnsNullSpecification() {
        assertNull(TransactionSpecifications.hasCategoryId(null));
    }

    @Test
    void dateBetween_whenAnyBoundIsNull_returnsNullSpecification() {
        var now = LocalDateTime.now();

        assertNull(TransactionSpecifications.dateBetween(null, now));
        assertNull(TransactionSpecifications.dateBetween(now, null));
        assertNull(TransactionSpecifications.dateBetween(null, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void amountBetween_whenBothBoundsAreNull_buildsNoPredicate() {
        var spec = TransactionSpecifications.amountBetween(null, null);

        var predicate = spec.toPredicate(
                mock(Root.class), mock(CriteriaQuery.class), mock(CriteriaBuilder.class));

        assertNull(predicate);
    }

    @Test
    void nonNullFilters_returnSpecifications() {
        assertNotNull(TransactionSpecifications.hasUserId(1L));
        assertNotNull(TransactionSpecifications.hasCategoryId(1L));
        assertNotNull(TransactionSpecifications.amountBetween(BigDecimal.ONE, null));
        assertNotNull(TransactionSpecifications.dateBetween(LocalDateTime.now(), LocalDateTime.now()));
    }
}
