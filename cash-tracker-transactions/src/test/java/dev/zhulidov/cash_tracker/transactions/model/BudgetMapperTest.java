package dev.zhulidov.cash_tracker.transactions.model;

import dev.zhulidov.cash_tracker.transactions.dto.BudgetUpdateRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BudgetMapperTest {

    private final BudgetMapper mapper = new BudgetMapperImpl();

    private Budget budget() {
        var category = Category.builder().id(10L).categoryName("Food").userId(1L).build();
        return Budget.builder().id(5L).userId(1L).limitAmount(new BigDecimal("1000.00"))
                .period(LocalDate.of(2026, 10, 1)).category(category).build();
    }

    @Test
    void toDto_mapsIdLimitPeriodAndCategory() {
        var dto = mapper.toDto(budget());

        assertEquals(5L, dto.id());
        assertEquals(new BigDecimal("1000.00"), dto.limitAmount());
        assertEquals(LocalDate.of(2026, 10, 1), dto.period());
        assertEquals(10L, dto.category().id());
        assertEquals("Food", dto.category().categoryName());
        assertEquals(1L, dto.category().userId());
    }

    @Test
    void toDto_whenBudgetIsNull_returnsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void updateEntityFromDto_changesOnlyLimitAmount() {
        var budget = budget();

        mapper.updateEntityFromDto(new BudgetUpdateRequest(new BigDecimal("2500.00"), LocalDate.of(2026, 12, 15)), budget);

        assertEquals(new BigDecimal("2500.00"), budget.getLimitAmount());
        // period, id, userId и category маппер не трогает: период нормализует сервис
        assertEquals(LocalDate.of(2026, 10, 1), budget.getPeriod());
        assertEquals(5L, budget.getId());
        assertEquals(1L, budget.getUserId());
        assertEquals(10L, budget.getCategory().getId());
    }

    @Test
    void updateEntityFromDto_whenRequestIsNull_leavesBudgetUntouched() {
        var budget = budget();

        mapper.updateEntityFromDto(null, budget);

        assertEquals(new BigDecimal("1000.00"), budget.getLimitAmount());
    }
}
