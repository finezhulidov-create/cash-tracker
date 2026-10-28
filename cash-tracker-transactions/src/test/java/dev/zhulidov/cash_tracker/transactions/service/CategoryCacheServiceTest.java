package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.transactions.dto.CategoryDto;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionSplitDto;
import dev.zhulidov.cash_tracker.transactions.model.Category;
import dev.zhulidov.cash_tracker.transactions.model.CategoryMapper;
import dev.zhulidov.cash_tracker.transactions.model.TransactionSplit;
import dev.zhulidov.cash_tracker.transactions.model.TransactionSplitMapper;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionSplitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryCacheServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionSplitRepository transactionSplitRepository;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private TransactionSplitMapper splitMapper;

    @InjectMocks
    private CategoryCacheService cacheService;

    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 10L;

    @Test
    void findAllByUser_mapsEveryCategoryToDto() {
        var food = Category.builder().id(1L).categoryName("Food").userId(USER_ID).build();
        var transport = Category.builder().id(2L).categoryName("Transport").userId(USER_ID).build();
        var foodDto = new CategoryDto(1L, "Food", USER_ID);
        var transportDto = new CategoryDto(2L, "Transport", USER_ID);
        when(categoryRepository.findAllByUserId(USER_ID)).thenReturn(List.of(food, transport));
        when(categoryMapper.toDto(food)).thenReturn(foodDto);
        when(categoryMapper.toDto(transport)).thenReturn(transportDto);

        assertEquals(List.of(foodDto, transportDto), cacheService.findAllByUser(USER_ID));
    }

    @Test
    void findAllByUser_whenUserHasNoCategories_returnsEmptyList() {
        when(categoryRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

        assertTrue(cacheService.findAllByUser(USER_ID).isEmpty());
    }

    @Test
    void findSplitsByCategory_mapsEverySplitToDto() {
        var split = TransactionSplit.builder().id(1L).amount(new BigDecimal("100")).build();
        var dto = new TransactionSplitDto(1L, 100L, new BigDecimal("100"), null);
        when(transactionSplitRepository.findAllByCategory_Id(CATEGORY_ID)).thenReturn(List.of(split));
        when(splitMapper.toDto(split)).thenReturn(dto);

        assertEquals(List.of(dto), cacheService.findSplitsByCategory(USER_ID, CATEGORY_ID));
    }

    @Test
    void findSplitsByCategory_whenNoSplits_returnsEmptyList() {
        when(transactionSplitRepository.findAllByCategory_Id(CATEGORY_ID)).thenReturn(List.of());

        assertTrue(cacheService.findSplitsByCategory(USER_ID, CATEGORY_ID).isEmpty());
    }
}
