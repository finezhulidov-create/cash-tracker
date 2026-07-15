package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.common.exception.InaccessibleResourceException;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.common.exception.TransactionSplitMismatchException;
import dev.zhulidov.cash_tracker.transactions.dto.*;
import dev.zhulidov.cash_tracker.transactions.model.Category;
import dev.zhulidov.cash_tracker.transactions.model.Transaction;
import dev.zhulidov.cash_tracker.transactions.model.TransactionMapper;
import dev.zhulidov.cash_tracker.transactions.model.TransactionSplit;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache splitsCache;

    @InjectMocks
    private TransactionService transactionService;

    private static final Long OWNER_ID = 1L;
    private static final Long STRANGER_ID = 2L;
    private static final Long CATEGORY_ID = 10L;
    private static final Long OTHER_CATEGORY_ID = 20L;
    private static final Long TRANSACTION_ID = 100L;

    private Category ownedCategory(Long categoryId) {
        return Category.builder().id(categoryId).categoryName("Food").userId(OWNER_ID).build();
    }

    private Category foreignCategory(Long categoryId) {
        return Category.builder().id(categoryId).categoryName("Food").userId(STRANGER_ID).build();
    }

    private CategoryDto categoryDtoRef(Long categoryId) {
        return new CategoryDto(categoryId, "Food", "owner");
    }

    private TransactionSplitDto splitDto(BigDecimal amount, Long categoryId) {
        return new TransactionSplitDto(amount, categoryDtoRef(categoryId), null);
    }

    @BeforeEach
    void setUp() {
        lenient().when(cacheManager.getCache("splits")).thenReturn(splitsCache);
    }

    // ==================== createTransaction ====================

    @Test
    void createTransaction_whenValid_savesEvictsCacheForEachDistinctCategoryAndReturnsDto() {
        var request = new TransactionCreateRequest(
                List.of(splitDto(new BigDecimal("2000.00"), CATEGORY_ID),
                        splitDto(new BigDecimal("500.00"), CATEGORY_ID),
                        splitDto(new BigDecimal("300.00"), OTHER_CATEGORY_ID)),
                "Supermarket",
                new BigDecimal("2800.00")
        );
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory(CATEGORY_ID)));
        when(categoryRepository.findById(OTHER_CATEGORY_ID)).thenReturn(Optional.of(ownedCategory(OTHER_CATEGORY_ID)));
        var expectedDto = new TransactionDto(TRANSACTION_ID, OWNER_ID, request.amount(), request.description(), LocalDateTime.now());
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(expectedDto);

        var result = transactionService.createTransaction(request, OWNER_ID);

        assertEquals(expectedDto, result);
        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getSplits().size());

        // эвикт должен произойти по каждой уникальной категории (две штуки), не по числу сплитов (три)
        verify(splitsCache).evict(OWNER_ID + ":" + CATEGORY_ID);
        verify(splitsCache).evict(OWNER_ID + ":" + OTHER_CATEGORY_ID);
        verify(splitsCache, times(2)).evict(any());
    }

    @Test
    void createTransaction_whenCategoryNotFound_throwsResourceNotFoundException_andDoesNotSaveOrEvict() {
        var request = new TransactionCreateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)), "Supermarket", new BigDecimal("2500.00"));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.createTransaction(request, OWNER_ID));

        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(splitsCache);
    }

    @Test
    void createTransaction_whenCategoryBelongsToAnotherUser_throwsInaccessibleResourceException() {
        var request = new TransactionCreateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)), "Supermarket", new BigDecimal("2500.00"));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(foreignCategory(CATEGORY_ID)));

        assertThrows(InaccessibleResourceException.class,
                () -> transactionService.createTransaction(request, OWNER_ID));

        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(splitsCache);
    }

    @Test
    void createTransaction_whenSplitSumDoesNotMatchAmount_throwsTransactionSplitMismatchException() {
        var request = new TransactionCreateRequest(
                List.of(splitDto(new BigDecimal("2000.00"), CATEGORY_ID),
                        splitDto(new BigDecimal("400.00"), CATEGORY_ID)), // 2400 != 2500
                "Supermarket", new BigDecimal("2500.00"));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory(CATEGORY_ID)));

        assertThrows(TransactionSplitMismatchException.class,
                () -> transactionService.createTransaction(request, OWNER_ID));

        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(splitsCache);
    }

    @Test
    void createTransaction_whenCacheManagerReturnsNullCache_doesNotThrow() {
        when(cacheManager.getCache("splits")).thenReturn(null);
        var request = new TransactionCreateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)), "Supermarket", new BigDecimal("2500.00"));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory(CATEGORY_ID)));
        when(transactionMapper.toDto(any(Transaction.class)))
                .thenReturn(new TransactionDto(TRANSACTION_ID, OWNER_ID, request.amount(), request.description(), LocalDateTime.now()));

        assertDoesNotThrow(() -> transactionService.createTransaction(request, OWNER_ID));
    }

    // ==================== deleteTransactionById ====================

    @Test
    void deleteTransactionById_whenOwned_deletesAndEvictsCacheForOldCategories() {
        var split1 = TransactionSplit.builder().id(1L).amount(new BigDecimal("2000.00")).category(ownedCategory(CATEGORY_ID)).build();
        var split2 = TransactionSplit.builder().id(2L).amount(new BigDecimal("500.00")).category(ownedCategory(OTHER_CATEGORY_ID)).build();
        var existing = Transaction.builder().id(TRANSACTION_ID).userId(OWNER_ID)
                .splits(new ArrayList<>(List.of(split1, split2))).build();
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existing));

        transactionService.deleteTransactionById(TRANSACTION_ID, OWNER_ID);

        verify(transactionRepository).deleteById(TRANSACTION_ID);
        verify(splitsCache).evict(OWNER_ID + ":" + CATEGORY_ID);
        verify(splitsCache).evict(OWNER_ID + ":" + OTHER_CATEGORY_ID);
    }

    @Test
    void deleteTransactionById_whenNotFound_throwsResourceNotFoundException() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.deleteTransactionById(TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).deleteById(any());
        verifyNoInteractions(splitsCache);
    }

    @Test
    void deleteTransactionById_whenBelongsToAnotherUser_throwsInaccessibleResourceException() {
        var foreignTransaction = Transaction.builder().id(TRANSACTION_ID).userId(STRANGER_ID).build();
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(foreignTransaction));

        assertThrows(InaccessibleResourceException.class,
                () -> transactionService.deleteTransactionById(TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).deleteById(any());
        verifyNoInteractions(splitsCache);
    }

    // ==================== updateTransaction ====================

    @Test
    void updateTransaction_whenValid_updatesFieldsAndEvictsBothOldAndNewCategoryCaches() {
        var oldSplit = TransactionSplit.builder().id(1L).amount(new BigDecimal("1000.00")).category(ownedCategory(OTHER_CATEGORY_ID)).build();
        var existing = Transaction.builder()
                .id(TRANSACTION_ID)
                .userId(OWNER_ID)
                .amount(new BigDecimal("1000.00"))
                .description("Old")
                .dateTime(LocalDateTime.now().minusDays(1))
                .splits(new ArrayList<>(List.of(oldSplit)))
                .build();
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)),
                "New description",
                new BigDecimal("2500.00"),
                LocalDateTime.now()
        );
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory(CATEGORY_ID)));
        var expectedDto = new TransactionDto(TRANSACTION_ID, OWNER_ID, request.amount(), request.description(), request.dateTime());
        when(transactionMapper.toDto(existing)).thenReturn(expectedDto);

        var result = transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID);

        assertEquals(expectedDto, result);
        assertEquals("New description", existing.getDescription());
        assertEquals(new BigDecimal("2500.00"), existing.getAmount());
        assertEquals(1, existing.getSplits().size());
        verify(transactionRepository).save(existing);

        // и старая категория (была удалена из транзакции), и новая должны быть эвикнуты
        verify(splitsCache).evict(OWNER_ID + ":" + OTHER_CATEGORY_ID);
        verify(splitsCache).evict(OWNER_ID + ":" + CATEGORY_ID);
    }

    @Test
    void updateTransaction_whenTransactionNotFound_throwsResourceNotFoundException() {
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)), "New", new BigDecimal("2500.00"), LocalDateTime.now());
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(splitsCache);
    }

    @Test
    void updateTransaction_whenTransactionBelongsToAnotherUser_throwsInaccessibleResourceException() {
        var foreignTransaction = Transaction.builder().id(TRANSACTION_ID).userId(STRANGER_ID)
                .splits(new ArrayList<>()).build();
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)), "New", new BigDecimal("2500.00"), LocalDateTime.now());
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(foreignTransaction));

        assertThrows(InaccessibleResourceException.class,
                () -> transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).save(any());
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void updateTransaction_whenNewCategoryNotFound_throwsResourceNotFoundException() {
        var existing = Transaction.builder().id(TRANSACTION_ID).userId(OWNER_ID)
                .amount(new BigDecimal("1000.00")).description("Old").dateTime(LocalDateTime.now())
                .splits(new ArrayList<>()).build();
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)), "New", new BigDecimal("2500.00"), LocalDateTime.now());
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransaction_whenNewCategoryBelongsToAnotherUser_throwsInaccessibleResourceException() {
        var existing = Transaction.builder().id(TRANSACTION_ID).userId(OWNER_ID)
                .amount(new BigDecimal("1000.00")).description("Old").dateTime(LocalDateTime.now())
                .splits(new ArrayList<>()).build();
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)), "New", new BigDecimal("2500.00"), LocalDateTime.now());
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(foreignCategory(CATEGORY_ID)));

        assertThrows(InaccessibleResourceException.class,
                () -> transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransaction_whenSplitSumDoesNotMatchAmount_throwsTransactionSplitMismatchException() {
        var existing = Transaction.builder().id(TRANSACTION_ID).userId(OWNER_ID)
                .amount(new BigDecimal("1000.00")).description("Old").dateTime(LocalDateTime.now())
                .splits(new ArrayList<>()).build();
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("100.00"), CATEGORY_ID)), "New", new BigDecimal("2500.00"), LocalDateTime.now());
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory(CATEGORY_ID)));

        assertThrows(TransactionSplitMismatchException.class,
                () -> transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(splitsCache);
    }

    // ==================== getById ====================

    @Test
    void getById_whenOwned_returnsDto() {
        var existing = Transaction.builder().id(TRANSACTION_ID).userId(OWNER_ID).build();
        var expectedDto = new TransactionDto(TRANSACTION_ID, OWNER_ID, BigDecimal.TEN, "desc", LocalDateTime.now());
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existing));
        when(transactionMapper.toDto(existing)).thenReturn(expectedDto);

        assertEquals(expectedDto, transactionService.getById(TRANSACTION_ID, OWNER_ID));
    }

    @Test
    void getById_whenNotFound_throwsResourceNotFoundException() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.getById(TRANSACTION_ID, OWNER_ID));
    }

    @Test
    void getById_whenBelongsToAnotherUser_throwsInaccessibleResourceException() {
        var foreignTransaction = Transaction.builder().id(TRANSACTION_ID).userId(STRANGER_ID).build();
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(foreignTransaction));

        assertThrows(InaccessibleResourceException.class,
                () -> transactionService.getById(TRANSACTION_ID, OWNER_ID));
    }

    // ==================== getAllTransactionsByUser ====================

    @Test
    void getAllTransactionsByUser_delegatesToRepositoryAndMapper() {
        Pageable pageable = Pageable.ofSize(10);
        Transaction entity = Transaction.builder().id(TRANSACTION_ID).userId(OWNER_ID).build();
        Page<Transaction> entityPage = new PageImpl<>(List.of(entity));
        Page<TransactionDto> dtoPage = new PageImpl<>(List.of(
                new TransactionDto(TRANSACTION_ID, OWNER_ID, BigDecimal.TEN, "desc", LocalDateTime.now())));

        when(transactionRepository.findAllByUserId(OWNER_ID, pageable)).thenReturn(entityPage);
        when(transactionMapper.toDtoPage(entityPage)).thenReturn(dtoPage);

        assertEquals(dtoPage, transactionService.getAllTransactionsByUser(OWNER_ID, pageable));
    }

    @Test
    void getAllTransactionsByUser_whenNoTransactions_returnsEmptyPage() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Transaction> emptyEntityPage = new PageImpl<>(List.of());
        Page<TransactionDto> emptyDtoPage = new PageImpl<>(List.of());
        when(transactionRepository.findAllByUserId(OWNER_ID, pageable)).thenReturn(emptyEntityPage);
        when(transactionMapper.toDtoPage(emptyEntityPage)).thenReturn(emptyDtoPage);

        var result = transactionService.getAllTransactionsByUser(OWNER_ID, pageable);

        assertTrue(result.getContent().isEmpty());
    }

    // ==================== getTotalAmountByDateRange ====================

    @Test
    void getTotalAmountByDateRange_delegatesToRepositoryAggregateQuery() {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();
        when(transactionRepository.sumAmountByUserIdAndDateTimeBetween(OWNER_ID, from, to))
                .thenReturn(new BigDecimal("4200.00"));

        assertEquals(new BigDecimal("4200.00"), transactionService.getTotalAmountByDateRange(OWNER_ID, from, to));
    }

    @Test
    void getTotalAmountByDateRange_whenNoTransactions_returnsZero() {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();
        when(transactionRepository.sumAmountByUserIdAndDateTimeBetween(OWNER_ID, from, to))
                .thenReturn(BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, transactionService.getTotalAmountByDateRange(OWNER_ID, from, to));
    }

    // ==================== getTransactions (Specification-фильтры) ====================

    @Test
    void getTransactions_withNoFilters_delegatesToRepositoryWithSpecification() {
        var criteria = new TransactionSearchCriteria(null, null, null, null, null);
        Pageable pageable = Pageable.ofSize(10);
        Transaction entity = Transaction.builder().id(TRANSACTION_ID).userId(OWNER_ID).build();
        Page<Transaction> entityPage = new PageImpl<>(List.of(entity));
        Page<TransactionDto> dtoPage = new PageImpl<>(List.of(
                new TransactionDto(TRANSACTION_ID, OWNER_ID, BigDecimal.TEN, "desc", LocalDateTime.now())));

        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);
        when(transactionMapper.toDtoPage(entityPage)).thenReturn(dtoPage);

        var result = transactionService.getTransactions(OWNER_ID, criteria, pageable);

        assertEquals(dtoPage, result);
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getTransactions_withAllFiltersSet_delegatesToRepositoryWithSpecification() {
        var criteria = new TransactionSearchCriteria(CATEGORY_ID, new BigDecimal("100"),
                new BigDecimal("1000"), LocalDateTime.now().minusDays(30), LocalDateTime.now());
        Pageable pageable = Pageable.ofSize(10);
        Page<Transaction> emptyEntityPage = new PageImpl<>(List.of());
        Page<TransactionDto> emptyDtoPage = new PageImpl<>(List.of());

        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyEntityPage);
        when(transactionMapper.toDtoPage(emptyEntityPage)).thenReturn(emptyDtoPage);

        var result = transactionService.getTransactions(OWNER_ID, criteria, pageable);

        assertTrue(result.getContent().isEmpty());
    }
}