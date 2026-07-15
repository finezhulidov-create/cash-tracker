package dev.zhulidov.cash_tracker.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.common.exception.InaccessibleResourceException;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.common.exception.TransactionSplitMismatchException;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import dev.zhulidov.cash_tracker.transactions.controller.TransactionController;
import dev.zhulidov.cash_tracker.transactions.dto.*;
import dev.zhulidov.cash_tracker.transactions.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final Long OWNER_ID = 1L;
    private static final Long TRANSACTION_ID = 100L;
    private static final Long CATEGORY_ID = 10L;

    private UsernamePasswordAuthenticationToken authToken() {
        var principal = new UserPrincipal(OWNER_ID, "owner@test.com", "hash");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    // Вложенные @NotNull categoryDto/transactionDto в TransactionSplitDto требуют полностью
    // заполненных объектов для прохождения валидации запроса, даже если сервис их не использует.
    private CategoryDto dummyCategoryDto() {
        return new CategoryDto(CATEGORY_ID, "Food", "owner");
    }

    private TransactionDto dummyTransactionDto() {
        return new TransactionDto(TRANSACTION_ID, OWNER_ID, new BigDecimal("2500.00"), "dummy", LocalDateTime.now());
    }

    private TransactionSplitDto splitDto(BigDecimal amount) {
        return new TransactionSplitDto(amount, dummyCategoryDto(), dummyTransactionDto());
    }

    // ==================== POST /transactions/create ====================

    @Test
    void createTransaction_whenValid_returns200() throws Exception {
        var request = new TransactionCreateRequest(
                List.of(splitDto(new BigDecimal("2000.00")), splitDto(new BigDecimal("500.00"))),
                "Supermarket",
                new BigDecimal("2500.00")
        );
        var responseDto = new TransactionDto(TRANSACTION_ID, OWNER_ID, request.amount(), request.description(), LocalDateTime.now());
        when(transactionService.createTransaction(eq(request), eq(OWNER_ID))).thenReturn(responseDto);

        mockMvc.perform(post("/transactions/create")
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TRANSACTION_ID))
                .andExpect(jsonPath("$.description").value("Supermarket"));
    }

    @Test
    void createTransaction_whenDescriptionMissing_returns400() throws Exception {
        var request = new TransactionCreateRequest(
                List.of(splitDto(new BigDecimal("2500.00"))), null, new BigDecimal("2500.00"));

        mockMvc.perform(post("/transactions/create")
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_whenSplitSumMismatch_returns400() throws Exception {
        var request = new TransactionCreateRequest(
                List.of(splitDto(new BigDecimal("100.00"))), "Supermarket", new BigDecimal("2500.00"));
        when(transactionService.createTransaction(eq(request), eq(OWNER_ID)))
                .thenThrow(new TransactionSplitMismatchException("Split amount must be compare to transaction amount"));

        mockMvc.perform(post("/transactions/create")
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("NOT_VALID_TRANSACTION_SPLIT_SUM"));
    }

    @Test
    void createTransaction_whenCategoryNotFound_returns404() throws Exception {
        var request = new TransactionCreateRequest(
                List.of(splitDto(new BigDecimal("2500.00"))), "Supermarket", new BigDecimal("2500.00"));
        when(transactionService.createTransaction(eq(request), eq(OWNER_ID)))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(post("/transactions/create")
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTransaction_whenUnauthenticated_returns401() throws Exception {
        var request = new TransactionCreateRequest(
                List.of(splitDto(new BigDecimal("2500.00"))), "Supermarket", new BigDecimal("2500.00"));

        mockMvc.perform(post("/transactions/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== DELETE /transactions/{id} ====================

    @Test
    void deleteTransaction_whenExists_returns200() throws Exception {
        mockMvc.perform(delete("/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken()))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTransaction_whenIdNotPositive_returns400() throws Exception {
        mockMvc.perform(delete("/transactions/{transId}", -1)
                        .with(authentication(authToken()))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteTransaction_whenNotFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Transaction not found"))
                .when(transactionService).deleteTransactionById(TRANSACTION_ID, OWNER_ID);

        mockMvc.perform(delete("/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken()))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTransaction_whenBelongsToAnotherUser_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new InaccessibleResourceException("Resource not Found"))
                .when(transactionService).deleteTransactionById(TRANSACTION_ID, OWNER_ID);

        mockMvc.perform(delete("/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken()))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ==================== PUT /transactions/{id} ====================

    @Test
    void updateTransaction_whenValid_returns200() throws Exception {
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("2500.00"))), "Updated", new BigDecimal("2500.00"), LocalDateTime.now());
        var responseDto = new TransactionDto(TRANSACTION_ID, OWNER_ID, request.amount(), request.description(), request.dateTime());
        when(transactionService.updateTransaction(eq(request), eq(TRANSACTION_ID), eq(OWNER_ID))).thenReturn(responseDto);

        mockMvc.perform(put("/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"));
    }

    @Test
    void updateTransaction_whenIdNotPositive_returns400() throws Exception {
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("2500.00"))), "Updated", new BigDecimal("2500.00"), LocalDateTime.now());

        mockMvc.perform(put("/transactions/{transId}", 0)
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTransaction_whenAmountMissing_returns400() throws Exception {
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("2500.00"))), "Updated", null, LocalDateTime.now());

        mockMvc.perform(put("/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTransaction_whenSplitSumMismatch_returns400() throws Exception {
        var request = new TransactionUpdateRequest(
                List.of(splitDto(new BigDecimal("100.00"))), "Updated", new BigDecimal("2500.00"), LocalDateTime.now());
        when(transactionService.updateTransaction(eq(request), eq(TRANSACTION_ID), eq(OWNER_ID)))
                .thenThrow(new TransactionSplitMismatchException("mismatch"));

        mockMvc.perform(put("/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /transactions/{id} ====================

    @Test
    void getTransaction_whenExists_returns200() throws Exception {
        var responseDto = new TransactionDto(TRANSACTION_ID, OWNER_ID, new BigDecimal("2500.00"), "Supermarket", LocalDateTime.now());
        when(transactionService.getById(TRANSACTION_ID, OWNER_ID)).thenReturn(responseDto);

        mockMvc.perform(get("/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TRANSACTION_ID));
    }

    @Test
    void getTransaction_whenIdNotPositive_returns400() throws Exception {
        mockMvc.perform(get("/transactions/{transId}", -5)
                        .with(authentication(authToken())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransaction_whenNotFound_returns404() throws Exception {
        when(transactionService.getById(TRANSACTION_ID, OWNER_ID))
                .thenThrow(new ResourceNotFoundException("Transaction not found"));

        mockMvc.perform(get("/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransaction_whenBelongsToAnotherUser_returns404() throws Exception {
        when(transactionService.getById(TRANSACTION_ID, OWNER_ID))
                .thenThrow(new InaccessibleResourceException("Resource not Found"));

        mockMvc.perform(get("/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken())))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /transactions/all ====================

    @Test
    void getAllTransactions_returns200WithPage() throws Exception {
        var dto = new TransactionDto(TRANSACTION_ID, OWNER_ID, new BigDecimal("2500.00"), "Supermarket", LocalDateTime.now());
        var page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
        when(transactionService.getAllTransactionsByUser(eq(OWNER_ID), any())).thenReturn(page);

        mockMvc.perform(get("/transactions/all")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getAllTransactions_whenEmpty_returns200WithEmptyPage() throws Exception {
        var page = new PageImpl<TransactionDto>(List.of(), PageRequest.of(0, 10), 0);
        when(transactionService.getAllTransactionsByUser(eq(OWNER_ID), any())).thenReturn(page);

        mockMvc.perform(get("/transactions/all")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ==================== GET /transactions/total ====================

    @Test
    void getTotalAmountByDateRange_returns200() throws Exception {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 1, 0, 0);
        when(transactionService.getTotalAmountByDateRange(OWNER_ID, from, to)).thenReturn(new BigDecimal("4200.00"));

        mockMvc.perform(get("/transactions/total")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(content().string("4200.00"));
    }

    @Test
    void getTotalAmountByDateRange_whenNoTransactions_returnsZero() throws Exception {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 2, 1, 0, 0);
        when(transactionService.getTotalAmountByDateRange(OWNER_ID, from, to)).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(get("/transactions/total")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    // ==================== GET /transactions/filter ====================

    @Test
    void getTransactions_withNoFilters_returns200() throws Exception {
        var dto = new TransactionDto(TRANSACTION_ID, OWNER_ID, new BigDecimal("2500.00"), "Supermarket", LocalDateTime.now());
        var page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
        when(transactionService.getTransactions(eq(OWNER_ID), any(TransactionSearchCriteria.class), any())).thenReturn(page);

        mockMvc.perform(get("/transactions/filter")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getTransactions_withCategoryAndAmountFilters_returns200() throws Exception {
        var page = new PageImpl<TransactionDto>(List.of(), PageRequest.of(0, 10), 0);
        when(transactionService.getTransactions(eq(OWNER_ID), any(TransactionSearchCriteria.class), any())).thenReturn(page);

        mockMvc.perform(get("/transactions/filter")
                        .param("categoryId", String.valueOf(CATEGORY_ID))
                        .param("minAmount", "100")
                        .param("maxAmount", "1000")
                        .with(authentication(authToken())))
                .andExpect(status().isOk());
    }

    @Test
    void getTransactions_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/transactions/filter"))
                .andExpect(status().isUnauthorized());
    }
}