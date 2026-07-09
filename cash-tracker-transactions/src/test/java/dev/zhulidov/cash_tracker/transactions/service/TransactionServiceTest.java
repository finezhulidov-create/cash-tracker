package dev.zhulidov.cash_tracker.transactions.service;

import dev.zhulidov.cash_tracker.common.exception.InaccessibleResourceException;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.common.exception.TransactionSplitMismatchException;
import dev.zhulidov.cash_tracker.transactions.dto.*;
import dev.zhulidov.cash_tracker.transactions.model.Category;
import dev.zhulidov.cash_tracker.transactions.model.Transaction;
import dev.zhulidov.cash_tracker.transactions.model.TransactionMapper;
import dev.zhulidov.cash_tracker.transactions.repository.CategoryRepository;
import dev.zhulidov.cash_tracker.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private static final Long OWNER_ID = 1L;
    private static final Long STRANGER_ID = 2L;
    private static final Long CATEGORY_ID = 10L;
    private static final Long TRANSACTION_ID = 100L;

    private Category ownedCategory;
    private Category foreignCategory;

    @BeforeEach
    void setUp() {
        ownedCategory = mock(Category.class);
        lenient().when(ownedCategory.getUserId()).thenReturn(OWNER_ID);

        foreignCategory = mock(Category.class);
        lenient().when(foreignCategory.getUserId()).thenReturn(STRANGER_ID);
    }

    private CategoryDto categoryDtoRef(Long categoryId) {
        return new CategoryDto(categoryId, "Food", "someone");
    }

    private TransactionSplitDto splitDto(BigDecimal amount, Long categoryId) {
        // transactionDto здесь не участвует в логике сервиса, поэтому передаём null
        return new TransactionSplitDto(amount, categoryDtoRef(categoryId), null);
    }

    // ==================== createTransaction ====================

    @Test
    void createTransaction_whenSplitsMatchAmountAndCategoryOwned_savesAndReturnsDto() {
        var request = new TransactionCreateRequest(
                List.of(splitDto(new BigDecimal("2000.00"), CATEGORY_ID),
                        splitDto(new BigDecimal("500.00"), CATEGORY_ID)),
                "Supermarket",
                new BigDecimal("2500.00")
        );
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory));
        var expectedDto = new TransactionDto(null, OWNER_ID, request.amount(), request.description(), null);
        when(transactionMapper.toDto(any(Transaction.class))).thenReturn(expectedDto);

        var result = transactionService.createTransaction(request, OWNER_ID);

        assertEquals(expectedDto, result);
        var captor = org.mockito.ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertEquals(OWNER_ID, saved.getUserId());
        assertEquals(new BigDecimal("2500.00"), saved.getAmount());
        assertEquals(2, saved.getSplits().size());
    }

    @Test
    void createTransaction_whenCategoryNotFound_throwsResourceNotFoundException() {
        var request = new TransactionCreateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)),
                "Supermarket",
                new BigDecimal("2500.00")
        );
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.createTransaction(request, OWNER_ID));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_whenCategoryBelongsToAnotherUser_throwsInaccessibleResourceException() {
        var request = new TransactionCreateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)),
                "Supermarket",
                new BigDecimal("2500.00")
        );
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(foreignCategory));

        assertThrows(InaccessibleResourceException.class,
                () -> transactionService.createTransaction(request, OWNER_ID));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_whenSplitSumDoesNotMatchAmount_throwsTransactionSplitMismatchException() {
        var request = new TransactionCreateRequest(
                List.of(splitDto(new BigDecimal("2000.00"), CATEGORY_ID),
                        splitDto(new BigDecimal("400.00"), CATEGORY_ID)), // 2400 != 2500
                "Supermarket",
                new BigDecimal("2500.00")
        );
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory));

        assertThrows(TransactionSplitMismatchException.class,
                () -> transactionService.createTransaction(request, OWNER_ID));

        verify(transactionRepository, never()).save(any());
    }

    // ==================== updateTransaction ====================

    @Test
    void updateTransaction_whenValid_updatesFieldsAndSplitsAndReturnsDto() {
        var existing = Transaction.builder()
                .id(TRANSACTION_ID)
                .userId(OWNER_ID)
                .amount(new BigDecimal("1000.00"))
                .description("Old")
                .dateTime(LocalDateTime.now().minusDays(1))
                .splits(new java.util.ArrayList<>())
                .build();
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)),
                "New description",
                new BigDecimal("2500.00"),
                LocalDateTime.now()
        );
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory));
        var expectedDto = new TransactionDto(TRANSACTION_ID, OWNER_ID, request.amount(), request.description(), request.dateTime());
        when(transactionMapper.toDto(existing)).thenReturn(expectedDto);

        var result = transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID);

        assertEquals(expectedDto, result);
        assertEquals("New description", existing.getDescription());
        assertEquals(new BigDecimal("2500.00"), existing.getAmount());
        assertEquals(1, existing.getSplits().size());
        verify(transactionRepository).save(existing);
    }

    @Test
    void updateTransaction_whenTransactionNotFound_throwsResourceNotFoundException() {
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)),
                "New description",
                new BigDecimal("2500.00"),
                LocalDateTime.now()
        );
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransaction_whenTransactionBelongsToAnotherUser_throwsInaccessibleResourceException() {
        var foreignTransaction = Transaction.builder()
                .id(TRANSACTION_ID)
                .userId(STRANGER_ID)
                .amount(new BigDecimal("1000.00"))
                .description("Old")
                .dateTime(LocalDateTime.now())
                .splits(new java.util.ArrayList<>())
                .build();
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)),
                "New description",
                new BigDecimal("2500.00"),
                LocalDateTime.now()
        );
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(foreignTransaction));

        assertThrows(InaccessibleResourceException.class,
                () -> transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).save(any());
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void updateTransaction_whenNewCategoryNotFound_throwsResourceNotFoundException() {
        var existing = Transaction.builder()
                .id(TRANSACTION_ID)
                .userId(OWNER_ID)
                .amount(new BigDecimal("1000.00"))
                .description("Old")
                .dateTime(LocalDateTime.now())
                .splits(new java.util.ArrayList<>())
                .build();
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)),
                "New description",
                new BigDecimal("2500.00"),
                LocalDateTime.now()
        );
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransaction_whenNewCategoryBelongsToAnotherUser_throwsInaccessibleResourceException() {
        var existing = Transaction.builder()
                .id(TRANSACTION_ID)
                .userId(OWNER_ID)
                .amount(new BigDecimal("1000.00"))
                .description("Old")
                .dateTime(LocalDateTime.now())
                .splits(new java.util.ArrayList<>())
                .build();
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("2500.00"), CATEGORY_ID)),
                "New description",
                new BigDecimal("2500.00"),
                LocalDateTime.now()
        );
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(foreignCategory));

        assertThrows(InaccessibleResourceException.class,
                () -> transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransaction_whenSplitSumDoesNotMatchAmount_throwsTransactionSplitMismatchException() {
        var existing = Transaction.builder()
                .id(TRANSACTION_ID)
                .userId(OWNER_ID)
                .amount(new BigDecimal("1000.00"))
                .description("Old")
                .dateTime(LocalDateTime.now())
                .splits(new java.util.ArrayList<>())
                .build();
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("100.00"), CATEGORY_ID)), // не совпадает с amount
                "New description",
                new BigDecimal("2500.00"),
                LocalDateTime.now()
        );
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(ownedCategory));

        assertThrows(TransactionSplitMismatchException.class,
                () -> transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).save(any());
    }

    // ==================== deleteTransactionById ====================

    @Test
    void deleteTransactionById_whenOwned_deletesTransaction() {
        var existing = Transaction.builder().id(TRANSACTION_ID).userId(OWNER_ID).build();
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existing));

        transactionService.deleteTransactionById(TRANSACTION_ID, OWNER_ID);

        verify(transactionRepository).deleteById(TRANSACTION_ID);
    }

    @Test
    void deleteTransactionById_whenNotFound_throwsResourceNotFoundException() {
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.deleteTransactionById(TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).deleteById(any());
    }

    @Test
    void deleteTransactionById_whenBelongsToAnotherUser_throwsInaccessibleResourceException() {
        var foreignTransaction = Transaction.builder().id(TRANSACTION_ID).userId(STRANGER_ID).build();
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(foreignTransaction));

        assertThrows(InaccessibleResourceException.class,
                () -> transactionService.deleteTransactionById(TRANSACTION_ID, OWNER_ID));

        verify(transactionRepository, never()).deleteById(any());
    }

    // ==================== getById ====================

    @Test
    void getById_whenOwned_returnsDto() {
        var existing = Transaction.builder().id(TRANSACTION_ID).userId(OWNER_ID).build();
        var expectedDto = new TransactionDto(TRANSACTION_ID, OWNER_ID, BigDecimal.TEN, "desc", LocalDateTime.now());
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(existing));
        when(transactionMapper.toDto(existing)).thenReturn(expectedDto);

        var result = transactionService.getById(TRANSACTION_ID, OWNER_ID);

        assertEquals(expectedDto, result);
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

        var result = transactionService.getAllTransactionsByUser(OWNER_ID, pageable);

        assertEquals(dtoPage, result);
        verify(transactionRepository).findAllByUserId(OWNER_ID, pageable);
    }

    // ==================== getTotalAmountByDateRange ====================

    @Test
    void getTotalAmountByDateRange_delegatesToRepositoryAggregateQuery() {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();
        when(transactionRepository.sumAmountByUserIdAndDateTimeBetween(OWNER_ID, from, to))
                .thenReturn(new BigDecimal("4200.00"));

        var result = transactionService.getTotalAmountByDateRange(OWNER_ID, from, to);

        assertEquals(new BigDecimal("4200.00"), result);
        verify(transactionRepository).sumAmountByUserIdAndDateTimeBetween(OWNER_ID, from, to);
    }

    // ==================== getAllTransactionsByDateRange ====================

    @Test
    void getAllTransactionsByDateRange_delegatesToRepositoryAndMapper() {
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();
        Pageable pageable = Pageable.ofSize(10);
        Transaction entity = Transaction.builder().id(TRANSACTION_ID).userId(OWNER_ID).build();
        Page<Transaction> entityPage = new PageImpl<>(List.of(entity));
        Page<TransactionDto> dtoPage = new PageImpl<>(List.of(
                new TransactionDto(TRANSACTION_ID, OWNER_ID, BigDecimal.TEN, "desc", LocalDateTime.now())));

        when(transactionRepository.findAllByUserIdAndDateTimeBetween(OWNER_ID, from, to, pageable))
                .thenReturn(entityPage);
        when(transactionMapper.toDtoPage(entityPage)).thenReturn(dtoPage);

        var result = transactionService.getAllTransactionsByDateRange(OWNER_ID, from, to, pageable);

        assertEquals(dtoPage, result);
    }
}
