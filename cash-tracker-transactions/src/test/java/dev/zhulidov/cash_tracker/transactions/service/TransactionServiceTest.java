package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.common.exception.InaccessibleResourceException;
import dev.zhulidov.cash_tracker.common.exception.InvalidRequestException;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.common.exception.TransactionSplitMismatchException;
import dev.zhulidov.cash_tracker.transactions.dto.SplitRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionCreateRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionDto;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionSearchCriteria;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionUpdateRequest;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 28, 12, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC);

    private static final Long OWNER_ID = 1L;
    private static final Long STRANGER_ID = 2L;
    private static final Long CATEGORY_ID = 10L;
    private static final Long OTHER_CATEGORY_ID = 20L;
    private static final Long TRANSACTION_ID = 100L;

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

    private TransactionService service;

    @BeforeEach
    void setUp() {
        lenient().when(cacheManager.getCache("splits")).thenReturn(splitsCache);
        service = new TransactionService(transactionRepository, categoryRepository, transactionMapper, cacheManager, FIXED_CLOCK);
    }

    // ==================== fixtures ====================

    private Category ownedCategory(Long categoryId) {
        return Category.builder().id(categoryId).categoryName("Food").userId(OWNER_ID).build();
    }

    private Category foreignCategory(Long categoryId) {
        return Category.builder().id(categoryId).categoryName("Food").userId(STRANGER_ID).build();
    }

    private SplitRequest split(String amount, Long categoryId) {
        return new SplitRequest(categoryId, new BigDecimal(amount));
    }

    private TransactionDto dto() {
        return new TransactionDto(TRANSACTION_ID, OWNER_ID, new BigDecimal("1000.00"), "Supermarket", NOW, List.of());
    }

    private Transaction existingTransaction(Long ownerId) {
        var trans = Transaction.builder().id(TRANSACTION_ID).userId(ownerId)
                .amount(new BigDecimal("1000.00")).description("Old").dateTime(NOW.minusDays(1)).build();
        var oldSplit = TransactionSplit.builder().id(1L).transaction(trans)
                .category(ownedCategory(CATEGORY_ID)).amount(new BigDecimal("1000.00")).build();
        trans.setSplits(new ArrayList<>(List.of(oldSplit)));
        return trans;
    }

    private TransactionCreateRequest createRequest(LocalDateTime dateTime, SplitRequest... splits) {
        return new TransactionCreateRequest("Supermarket", new BigDecimal("1000.00"), dateTime, List.of(splits));
    }

    // ==================== createTransaction ====================

    @Test
    void createTransaction_whenValid_savesAndEvictsCacheOncePerDistinctCategory() {
        var request = new TransactionCreateRequest("Supermarket", new BigDecimal("1000.00"), null,
                List.of(split("500.00", CATEGORY_ID), split("200.00", CATEGORY_ID), split("300.00", OTHER_CATEGORY_ID)));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory(CATEGORY_ID)));
        when(categoryRepository.findById(OTHER_CATEGORY_ID)).thenReturn(Optional.of(ownedCategory(OTHER_CATEGORY_ID)));
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(dto());

        var result = service.createTransaction(request, OWNER_ID);

        assertEquals(dto(), result);
        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        var saved = captor.getValue();
        assertEquals(OWNER_ID, saved.getUserId());
        assertEquals("Supermarket", saved.getDescription());
        assertEquals(new BigDecimal("1000.00"), saved.getAmount());
        assertEquals(3, saved.getSplits().size());
        assertTrue(saved.getSplits().stream().allMatch(s -> s.getTransaction() == saved));
        verify(splitsCache).evict(OWNER_ID + ":" + CATEGORY_ID);
        verify(splitsCache).evict(OWNER_ID + ":" + OTHER_CATEGORY_ID);
        verify(splitsCache, times(2)).evict(any());
    }

    @Test
    void createTransaction_whenDateIsNotProvided_usesCurrentTimeOfInjectedClock() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory(CATEGORY_ID)));
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(dto());

        service.createTransaction(createRequest(null, split("1000.00", CATEGORY_ID)), OWNER_ID);

        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(NOW, captor.getValue().getDateTime());
    }

    @Test
    void createTransaction_whenDateIsProvided_keepsItAsEntered() {
        var enteredByUser = LocalDateTime.of(2026, 9, 15, 8, 30);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory(CATEGORY_ID)));
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(dto());

        service.createTransaction(createRequest(enteredByUser, split("1000.00", CATEGORY_ID)), OWNER_ID);

        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(enteredByUser, captor.getValue().getDateTime());
    }

    @Test
    void createTransaction_whenCategoryRepeatsInSplits_looksItUpOnlyOnce() {
        var request = new TransactionCreateRequest("Supermarket", new BigDecimal("1000.00"), null,
                List.of(split("600.00", CATEGORY_ID), split("400.00", CATEGORY_ID)));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory(CATEGORY_ID)));
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(dto());

        service.createTransaction(request, OWNER_ID);

        verify(categoryRepository, times(1)).findById(CATEGORY_ID);
    }

    @Test
    void createTransaction_whenSplitsDoNotSumToAmount_throwsBeforeTouchingDatabase() {
        var request = createRequest(null, split("100.00", CATEGORY_ID));

        assertThrows(TransactionSplitMismatchException.class, () -> service.createTransaction(request, OWNER_ID));

        verifyNoInteractions(categoryRepository);
        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(splitsCache);
    }

    @Test
    void createTransaction_whenCategoryNotFound_throwsResourceNotFoundExceptionAndSavesNothing() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createTransaction(createRequest(null, split("1000.00", CATEGORY_ID)), OWNER_ID));

        verify(transactionRepository, never()).save(any());
        verifyNoInteractions(splitsCache);
    }

    @Test
    void createTransaction_whenCategoryBelongsToAnotherUser_throwsInaccessibleResourceException() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(foreignCategory(CATEGORY_ID)));

        assertThrows(InaccessibleResourceException.class,
                () -> service.createTransaction(createRequest(null, split("1000.00", CATEGORY_ID)), OWNER_ID));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_whenCacheIsNotConfigured_stillSucceeds() {
        when(cacheManager.getCache("splits")).thenReturn(null);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory(CATEGORY_ID)));
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(dto());

        assertEquals(dto(), service.createTransaction(createRequest(null, split("1000.00", CATEGORY_ID)), OWNER_ID));
    }

    // ==================== deleteTransactionById ====================

    @Test
    void deleteTransactionById_whenOwned_deletesAndEvictsCacheOfItsCategories() {
        var trans = existingTransaction(OWNER_ID);
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trans));

        service.deleteTransactionById(TRANSACTION_ID, OWNER_ID);

        verify(transactionRepository).delete(trans);
        verify(splitsCache).evict(OWNER_ID + ":" + CATEGORY_ID);
    }

    @Test
    void deleteTransactionById_whenNotFound_throwsResourceNotFoundException() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteTransactionById(TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void deleteTransactionById_whenBelongsToAnotherUser_throwsInaccessibleResourceException() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existingTransaction(STRANGER_ID)));

        assertThrows(InaccessibleResourceException.class, () -> service.deleteTransactionById(TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    // ==================== updateTransaction ====================

    @Test
    void updateTransaction_whenValid_replacesFieldsAndSplitsAndEvictsOldAndNewCategories() {
        var trans = existingTransaction(OWNER_ID);
        var newDate = LocalDateTime.of(2026, 10, 1, 9, 0);
        var request = new TransactionUpdateRequest("New", new BigDecimal("700.00"), newDate,
                List.of(split("400.00", OTHER_CATEGORY_ID), split("300.00", OTHER_CATEGORY_ID)));
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trans));
        when(categoryRepository.findById(OTHER_CATEGORY_ID)).thenReturn(Optional.of(ownedCategory(OTHER_CATEGORY_ID)));
        when(transactionMapper.toDto(trans)).thenReturn(dto());

        var result = service.updateTransaction(request, TRANSACTION_ID, OWNER_ID);

        assertEquals(dto(), result);
        assertEquals("New", trans.getDescription());
        assertEquals(new BigDecimal("700.00"), trans.getAmount());
        assertEquals(newDate, trans.getDateTime());
        assertEquals(2, trans.getSplits().size());
        assertTrue(trans.getSplits().stream().allMatch(s -> s.getCategory().getId().equals(OTHER_CATEGORY_ID)));
        verify(transactionRepository).saveAndFlush(trans);
        verify(splitsCache).evict(OWNER_ID + ":" + CATEGORY_ID);
        verify(splitsCache).evict(OWNER_ID + ":" + OTHER_CATEGORY_ID);
    }

    @Test
    void updateTransaction_whenSplitsDoNotSumToAmount_throwsAndLeavesTransactionUntouched() {
        var trans = existingTransaction(OWNER_ID);
        var request = new TransactionUpdateRequest("New", new BigDecimal("700.00"), NOW,
                List.of(split("100.00", CATEGORY_ID)));
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trans));

        assertThrows(TransactionSplitMismatchException.class,
                () -> service.updateTransaction(request, TRANSACTION_ID, OWNER_ID));

        assertEquals("Old", trans.getDescription());
        assertEquals(1, trans.getSplits().size());
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTransaction_whenNewCategoryBelongsToAnotherUser_throwsAndLeavesTransactionUntouched() {
        var trans = existingTransaction(OWNER_ID);
        var request = new TransactionUpdateRequest("New", new BigDecimal("700.00"), NOW,
                List.of(split("700.00", OTHER_CATEGORY_ID)));
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trans));
        when(categoryRepository.findById(OTHER_CATEGORY_ID)).thenReturn(Optional.of(foreignCategory(OTHER_CATEGORY_ID)));

        assertThrows(InaccessibleResourceException.class,
                () -> service.updateTransaction(request, TRANSACTION_ID, OWNER_ID));

        assertEquals("Old", trans.getDescription());
        assertEquals(CATEGORY_ID, trans.getSplits().get(0).getCategory().getId());
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateTransaction_whenTransactionNotFound_throwsResourceNotFoundException() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());
        var request = new TransactionUpdateRequest("New", new BigDecimal("700.00"), NOW, List.of(split("700.00", CATEGORY_ID)));

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateTransaction(request, TRANSACTION_ID, OWNER_ID));
    }

    @Test
    void updateTransaction_whenTransactionBelongsToAnotherUser_throwsInaccessibleResourceException() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existingTransaction(STRANGER_ID)));
        var request = new TransactionUpdateRequest("New", new BigDecimal("700.00"), NOW, List.of(split("700.00", CATEGORY_ID)));

        assertThrows(InaccessibleResourceException.class,
                () -> service.updateTransaction(request, TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).saveAndFlush(any());
    }

    // ==================== getById ====================

    @Test
    void getById_whenOwned_returnsMappedDto() {
        var trans = existingTransaction(OWNER_ID);
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(trans));
        when(transactionMapper.toDto(trans)).thenReturn(dto());

        assertEquals(dto(), service.getById(TRANSACTION_ID, OWNER_ID));
    }

    @Test
    void getById_whenNotFound_throwsResourceNotFoundException() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(TRANSACTION_ID, OWNER_ID));
    }

    @Test
    void getById_whenBelongsToAnotherUser_throwsInaccessibleResourceException() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existingTransaction(STRANGER_ID)));

        assertThrows(InaccessibleResourceException.class, () -> service.getById(TRANSACTION_ID, OWNER_ID));
    }

    // ==================== getTotalAmountByDateRange ====================

    @Test
    void getTotalAmountByDateRange_returnsSumFromRepository() {
        var from = LocalDateTime.of(2026, 10, 1, 0, 0);
        var to = LocalDateTime.of(2026, 10, 31, 23, 59);
        when(transactionRepository.sumAmountByUserIdAndDateTimeBetween(OWNER_ID, from, to))
                .thenReturn(new BigDecimal("123.45"));

        assertEquals(new BigDecimal("123.45"), service.getTotalAmountByDateRange(OWNER_ID, from, to));
    }

    @Test
    void getTotalAmountByDateRange_whenFromIsAfterTo_throwsInvalidRequestException() {
        var from = LocalDateTime.of(2026, 10, 31, 0, 0);
        var to = LocalDateTime.of(2026, 10, 1, 0, 0);

        assertThrows(InvalidRequestException.class, () -> service.getTotalAmountByDateRange(OWNER_ID, from, to));

        verifyNoInteractions(transactionRepository);
    }

    // ==================== getTransactions ====================

    @Test
    @SuppressWarnings("unchecked")
    void getTransactions_queriesWithSpecificationAndReturnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        var trans = existingTransaction(OWNER_ID);
        var page = new PageImpl<>(List.of(trans), pageable, 1);
        var mappedPage = new PageImpl<>(List.of(dto()), pageable, 1);
        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(transactionMapper.toDtoPage(page)).thenReturn(mappedPage);

        var result = service.getTransactions(OWNER_ID, new TransactionSearchCriteria(), pageable);

        assertEquals(mappedPage, result);
    }

    @Test
    void getTransactions_whenCriteriaAreFullyPopulated_stillQueriesOnce() {
        Pageable pageable = PageRequest.of(0, 10);
        var criteria = new TransactionSearchCriteria(CATEGORY_ID, new BigDecimal("10"), new BigDecimal("100"),
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 31, 23, 59));
        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));
        when(transactionMapper.toDtoPage(any())).thenReturn(new PageImpl<>(List.of()));

        service.getTransactions(OWNER_ID, criteria, pageable);

        verify(transactionRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getTransactions_whenFromIsAfterTo_throwsInvalidRequestException() {
        var criteria = new TransactionSearchCriteria(null, null, null,
                LocalDateTime.of(2026, 10, 31, 0, 0), LocalDateTime.of(2026, 10, 1, 0, 0));

        assertThrows(InvalidRequestException.class,
                () -> service.getTransactions(OWNER_ID, criteria, PageRequest.of(0, 10)));

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void getTransactions_whenMinAmountIsGreaterThanMax_throwsInvalidRequestException() {
        var criteria = new TransactionSearchCriteria(null, new BigDecimal("200"), new BigDecimal("100"), null, null);

        assertThrows(InvalidRequestException.class,
                () -> service.getTransactions(OWNER_ID, criteria, PageRequest.of(0, 10)));

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void getTransactions_whenOnlyOneDateBoundIsGiven_isAllowed() {
        Pageable pageable = PageRequest.of(0, 10);
        var criteria = new TransactionSearchCriteria(null, null, null, LocalDateTime.of(2026, 10, 1, 0, 0), null);
        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of()));
        when(transactionMapper.toDtoPage(any())).thenReturn(new PageImpl<>(List.of()));

        assertDoesNotThrow(() -> service.getTransactions(OWNER_ID, criteria, pageable));
    }
}
