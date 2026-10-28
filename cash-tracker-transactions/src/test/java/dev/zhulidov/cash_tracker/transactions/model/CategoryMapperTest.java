package dev.zhulidov.cash_tracker.transactions.model;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryMapperTest {

    private final CategoryMapper mapper = new CategoryMapperImpl();

    @Test
    void toDto_mapsIdNameAndUserId() {
        var dto = mapper.toDto(Category.builder().id(10L).categoryName("Food").userId(1L).build());

        assertEquals(10L, dto.id());
        assertEquals("Food", dto.categoryName());
        assertEquals(1L, dto.userId());
    }

    @Test
    void toDto_whenCategoryIsNull_returnsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toDtoPage_mapsContentAndKeepsPaginationMetadata() {
        var food = Category.builder().id(1L).categoryName("Food").userId(1L).build();
        var transport = Category.builder().id(2L).categoryName("Transport").userId(1L).build();
        var page = new PageImpl<>(List.of(food, transport), PageRequest.of(0, 2), 5);

        var result = mapper.toDtoPage(page);

        assertEquals(2, result.getContent().size());
        assertEquals("Transport", result.getContent().get(1).categoryName());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
    }
}
