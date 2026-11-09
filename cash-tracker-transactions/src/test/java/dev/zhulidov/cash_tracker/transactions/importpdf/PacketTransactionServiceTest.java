package dev.zhulidov.cash_tracker.transactions.importpdf;

import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.transactions.dto.ReviewedTransactionRequest;
import dev.zhulidov.cash_tracker.transactions.dto.SplitRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionCreateRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionDto;
import dev.zhulidov.cash_tracker.transactions.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Атомарность пачки (всё или ничего) проверяет StatementImportIntegrationTest на настоящей базе. */
@ExtendWith(MockitoExtension.class)
class PacketTransactionServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 28, 14, 30);

    @Mock
    private TransactionService transactionService;

    private PacketTransactionService service;

    @BeforeEach
    void setUp() {
        service = new PacketTransactionService(transactionService);
    }

    private static TransactionCreateRequest request(String description, String amount) {
        BigDecimal sum = new BigDecimal(amount);
        return new TransactionCreateRequest(description, sum, AT, List.of(new SplitRequest(10L, sum)));
    }

    private static TransactionDto dto(long id, TransactionCreateRequest request) {
        return new TransactionDto(id, USER_ID, request.amount(), request.description(), request.dateTime(), List.of());
    }

    @Test
    void createsEveryTransactionInOrderAndReturnsTheirDtos() {
        var coffee = request("Кофейня", "350.00");
        var taxi = request("Такси", "1200.00");
        when(transactionService.createTransaction(coffee, USER_ID)).thenReturn(dto(1, coffee));
        when(transactionService.createTransaction(taxi, USER_ID)).thenReturn(dto(2, taxi));

        var result = service.createPacketTransactions(new ReviewedTransactionRequest(List.of(coffee, taxi)), USER_ID);

        assertEquals(List.of(1L, 2L), result.stream().map(TransactionDto::id).toList());
        InOrder inOrder = inOrder(transactionService);
        inOrder.verify(transactionService).createTransaction(coffee, USER_ID);
        inOrder.verify(transactionService).createTransaction(taxi, USER_ID);
    }

    @Test
    void failureOnOneTransaction_stopsAndPropagates() {
        var coffee = request("Кофейня", "350.00");
        var broken = request("Чужая категория", "100.00");
        var taxi = request("Такси", "1200.00");
        when(transactionService.createTransaction(coffee, USER_ID)).thenReturn(dto(1, coffee));
        when(transactionService.createTransaction(broken, USER_ID)).thenThrow(new ResourceNotFoundException("Category not found"));

        assertThrows(ResourceNotFoundException.class,
                () -> service.createPacketTransactions(new ReviewedTransactionRequest(List.of(coffee, broken, taxi)), USER_ID));

        verify(transactionService, never()).createTransaction(eq(taxi), any());
    }
}
