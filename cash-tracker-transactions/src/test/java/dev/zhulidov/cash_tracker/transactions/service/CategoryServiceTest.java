package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.common.exception.InaccessibleResourceException;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.transactions.dto.CategoryCreateRequestDto;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository repository;
    @Mock
    private TransactionSplitRepository splitRepository;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private TransactionSplitMapper transactionSplitMapper;

    @InjectMocks
    private CategoryService categoryService;

    private static final Long OWNER_ID = 1L;
    private static final Long STRANGER_ID = 2L;
    private static final Long CATEGORY_ID = 10L;

    private Category ownedCategory() {
        return Category.builder().id(CATEGORY_ID).categoryName("Food").userId(OWNER_ID).build();
    }

    private Category foreignCategory() {
        return Category.builder().id(CATEGORY_ID).categoryName("Food").userId(STRANGER_ID).build();
    }

    // ==================== createCategory ====================

    @Test
    void createCategory_buildsWithUserIdFromToken_savesAndReturnsDto() {
        var request = new CategoryCreateRequestDto("Food");
        var saved = Category.builder().id(CATEGORY_ID).categoryName("Food").userId(OWNER_ID).build();
        var expectedDto = new CategoryDto(CATEGORY_ID, "Food", null);

        when(repository.save(any(Category.class))).thenReturn(saved);
        when(categoryMapper.toDto(saved)).thenReturn(expectedDto);

        var result = categoryService.createCategory(request, OWNER_ID);

        assertEquals(expectedDto, result);
        var captor = org.mockito.ArgumentCaptor.forClass(Category.class);
        verify(repository).save(captor.capture());
        assertEquals(OWNER_ID, captor.getValue().getUserId());
        assertEquals("Food", captor.getValue().getCategoryName());
    }

    // ==================== getCategoryById ====================

    @Test
    void getCategoryById_whenOwned_returnsDto() {
        var category = ownedCategory();
        var expectedDto = new CategoryDto(CATEGORY_ID, "Food", null);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(categoryMapper.toDto(category)).thenReturn(expectedDto);

        var result = categoryService.getCategoryById(CATEGORY_ID, OWNER_ID);

        assertEquals(expectedDto, result);
    }

    @Test
    void getCategoryById_whenNotFound_throwsResourceNotFoundException() {
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getCategoryById(CATEGORY_ID, OWNER_ID));
    }

    @Test
    void getCategoryById_whenBelongsToAnotherUser_throwsInaccessibleResourceException() {
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(foreignCategory()));

        assertThrows(InaccessibleResourceException.class,
                () -> categoryService.getCategoryById(CATEGORY_ID, OWNER_ID));
    }

    // ==================== deleteCategoryById ====================

    @Test
    void deleteCategoryById_whenOwned_deletes() {
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory()));

        categoryService.deleteCategoryById(CATEGORY_ID, OWNER_ID);

        verify(repository).deleteById(CATEGORY_ID);
    }

    @Test
    void deleteCategoryById_whenNotFound_throwsResourceNotFoundException() {
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.deleteCategoryById(CATEGORY_ID, OWNER_ID));

        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteCategoryById_whenBelongsToAnotherUser_throwsInaccessibleResourceException() {
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(foreignCategory()));

        assertThrows(InaccessibleResourceException.class,
                () -> categoryService.deleteCategoryById(CATEGORY_ID, OWNER_ID));

        verify(repository, never()).deleteById(any());
    }

    // ==================== updateCategory ====================

    @Test
    void updateCategory_whenOwned_updatesNameAndReturnsDto() {
        var category = ownedCategory();
        var expectedDto = new CategoryDto(CATEGORY_ID, "Groceries", null);
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(repository.save(category)).thenReturn(category);
        when(categoryMapper.toDto(category)).thenReturn(expectedDto);

        var result = categoryService.updateCategory(CATEGORY_ID, "Groceries", OWNER_ID);

        assertEquals(expectedDto, result);
        assertEquals("Groceries", category.getCategoryName());
        verify(repository).save(category);
    }

    @Test
    void updateCategory_whenNotFound_throwsResourceNotFoundException() {
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.updateCategory(CATEGORY_ID, "Groceries", OWNER_ID));

        verify(repository, never()).save(any());
    }

    @Test
    void updateCategory_whenBelongsToAnotherUser_throwsInaccessibleResourceException() {
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(foreignCategory()));

        assertThrows(InaccessibleResourceException.class,
                () -> categoryService.updateCategory(CATEGORY_ID, "Groceries", OWNER_ID));

        verify(repository, never()).save(any());
    }

    // ==================== getCategoriesByUserId ====================

    @Test
    void getCategoriesByUserId_whenCategoriesExist_returnsMappedPage() {
        var cat1 = Category.builder().id(1L).categoryName("Food").userId(OWNER_ID).build();
        var cat2 = Category.builder().id(2L).categoryName("Transport").userId(OWNER_ID).build();
        var dto1 = new CategoryDto(1L, "Food", null);
        var dto2 = new CategoryDto(2L, "Transport", null);
        var pageable = PageRequest.of(0, 10);
        var entityPage = new PageImpl<>(List.of(cat1, cat2), pageable, 2);
        var dtoPage = new PageImpl<>(List.of(dto1, dto2), pageable, 2);

        when(repository.findAllByUserId(OWNER_ID, pageable)).thenReturn(entityPage);
        when(categoryMapper.toDtoPage(entityPage)).thenReturn(dtoPage);

        var result = categoryService.getCategoriesByUserId(OWNER_ID, pageable);

        assertEquals(dtoPage, result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void getCategoriesByUserId_whenNoCategories_returnsEmptyPage() {
        var pageable = PageRequest.of(0, 10);
        var emptyEntityPage = new PageImpl<Category>(List.of(), pageable, 0);
        var emptyDtoPage = new PageImpl<CategoryDto>(List.of(), pageable, 0);

        when(repository.findAllByUserId(OWNER_ID, pageable)).thenReturn(emptyEntityPage);
        when(categoryMapper.toDtoPage(emptyEntityPage)).thenReturn(emptyDtoPage);

        var result = categoryService.getCategoriesByUserId(OWNER_ID, pageable);

        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    // ==================== getSplitsByCategory ====================

    @Test
    void getSplitsByCategory_whenCategoryNotFound_throwsResourceNotFoundException() {
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getSplitsByCategory(OWNER_ID, CATEGORY_ID, PageRequest.of(0, 10)));

        verify(splitRepository, never()).findAllByCategory_Id(any(Long.class));
    }

    @Test
    void getSplitsByCategory_whenCategoryBelongsToAnotherUser_throwsInaccessibleResourceException() {
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(foreignCategory()));

        assertThrows(InaccessibleResourceException.class,
                () -> categoryService.getSplitsByCategory(OWNER_ID, CATEGORY_ID, PageRequest.of(0, 10)));

        verify(splitRepository, never()).findAllByCategory_Id(any(Long.class));
    }

    @Test
    void getSplitsByCategory_whenNoSplits_returnsEmptyPage() {
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory()));
        when(splitRepository.findAllByCategory_Id(CATEGORY_ID)).thenReturn(List.of());

        var result = categoryService.getSplitsByCategory(OWNER_ID, CATEGORY_ID, PageRequest.of(0, 10));

        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getSplitsByCategory_whenRequestedPageWithinBounds_returnsCorrectSlice() {
        var split1 = TransactionSplit.builder().id(1L).amount(new BigDecimal("100")).build();
        var split2 = TransactionSplit.builder().id(2L).amount(new BigDecimal("200")).build();
        var split3 = TransactionSplit.builder().id(3L).amount(new BigDecimal("300")).build();
        var dto1 = new TransactionSplitDto(new BigDecimal("100"), null, null);
        var dto2 = new TransactionSplitDto(new BigDecimal("200"), null, null);
        var dto3 = new TransactionSplitDto(new BigDecimal("300"), null, null);

        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory()));
        when(splitRepository.findAllByCategory_Id(CATEGORY_ID)).thenReturn(List.of(split1, split2, split3));
        when(transactionSplitMapper.toDto(split1)).thenReturn(dto1);
        when(transactionSplitMapper.toDto(split2)).thenReturn(dto2);
        when(transactionSplitMapper.toDto(split3)).thenReturn(dto3);

        // страница 0, размер 2 -> первые два элемента
        var page0 = categoryService.getSplitsByCategory(OWNER_ID, CATEGORY_ID, PageRequest.of(0, 2));
        assertEquals(List.of(dto1, dto2), page0.getContent());
        assertEquals(3, page0.getTotalElements());

        // страница 1, размер 2 -> оставшийся один элемент
        var page1 = categoryService.getSplitsByCategory(OWNER_ID, CATEGORY_ID, PageRequest.of(1, 2));
        assertEquals(List.of(dto3), page1.getContent());
        assertEquals(3, page1.getTotalElements());
    }

    @Test
    void getSplitsByCategory_whenRequestedPageBeyondBounds_returnsEmptyContentButCorrectTotal() {
        var split1 = TransactionSplit.builder().id(1L).amount(new BigDecimal("100")).build();
        when(repository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory()));
        when(splitRepository.findAllByCategory_Id(CATEGORY_ID)).thenReturn(List.of(split1));

        var result = categoryService.getSplitsByCategory(OWNER_ID, CATEGORY_ID, PageRequest.of(5, 10));

        assertTrue(result.getContent().isEmpty());
        assertEquals(1, result.getTotalElements());
    }
}