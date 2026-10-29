package dev.zhulidov.cash_tracker.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.common.exception.InaccessibleResourceException;
import dev.zhulidov.cash_tracker.common.exception.InvalidRequestException;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.common.exception.TransactionSplitMismatchException;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import dev.zhulidov.cash_tracker.transactions.controller.TransactionController;
import dev.zhulidov.cash_tracker.transactions.dto.CategoryDto;
import dev.zhulidov.cash_tracker.transactions.dto.SplitRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionCreateRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionDto;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionSearchCriteria;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionSplitDto;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionUpdateRequest;
import dev.zhulidov.cash_tracker.transactions.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
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
    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 10, 28, 14, 30);

    private UsernamePasswordAuthenticationToken authToken() {
        var principal = new UserPrincipal(OWNER_ID, "owner@test.com", "hash");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private TransactionDto transactionDto() {
        var category = new CategoryDto(CATEGORY_ID, "Food", OWNER_ID);
        return new TransactionDto(TRANSACTION_ID, OWNER_ID, new BigDecimal("1000.00"), "Supermarket", WHEN,
                List.of(new TransactionSplitDto(1L, TRANSACTION_ID, new BigDecimal("700.00"), category),
                        new TransactionSplitDto(2L, TRANSACTION_ID, new BigDecimal("300.00"), category)));
    }

    private TransactionCreateRequest createRequest() {
        return new TransactionCreateRequest("Supermarket", new BigDecimal("1000.00"), WHEN,
                List.of(new SplitRequest(CATEGORY_ID, new BigDecimal("700.00")),
                        new SplitRequest(CATEGORY_ID, new BigDecimal("300.00"))));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    // ==================== POST /v1/transactions ====================

    @Test
    void createTransaction_whenValid_returnsTransactionWithSplits() throws Exception {
        var request = createRequest();
        when(transactionService.createTransaction(request, OWNER_ID)).thenReturn(transactionDto());

        mockMvc.perform(post("/v1/transactions")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TRANSACTION_ID))
                .andExpect(jsonPath("$.description").value("Supermarket"))
                .andExpect(jsonPath("$.dateTime").value("2026-10-28T14:30:00"))
                .andExpect(jsonPath("$.splits.length()").value(2))
                .andExpect(jsonPath("$.splits[0].amount").value(700.00))
                .andExpect(jsonPath("$.splits[0].category.categoryName").value("Food"))
                .andExpect(jsonPath("$.splits[0].transactionId").value(TRANSACTION_ID));
    }

    @Test
    void createTransaction_requestNeedsOnlyCategoryIdAndAmountPerSplit() throws Exception {
        // контракт для фронта: в части передаются только categoryId и amount, без вложенных DTO
        String body = """
                {"description":"Supermarket","amount":1000.00,
                 "splits":[{"categoryId":10,"amount":700.00},{"categoryId":10,"amount":300.00}]}""";
        var expected = new TransactionCreateRequest("Supermarket", new BigDecimal("1000.00"), null,
                List.of(new SplitRequest(10L, new BigDecimal("700.00")), new SplitRequest(10L, new BigDecimal("300.00"))));
        when(transactionService.createTransaction(expected, OWNER_ID)).thenReturn(transactionDto());

        mockMvc.perform(post("/v1/transactions")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void createTransaction_dateTimeIsOptional() throws Exception {
        var withoutDate = new TransactionCreateRequest("Supermarket", new BigDecimal("1000.00"), null,
                List.of(new SplitRequest(CATEGORY_ID, new BigDecimal("1000.00"))));
        when(transactionService.createTransaction(withoutDate, OWNER_ID)).thenReturn(transactionDto());

        mockMvc.perform(post("/v1/transactions")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(withoutDate)))
                .andExpect(status().isOk());
    }

    @Test
    void createTransaction_whenDescriptionIsBlank_returns400WithFieldError() throws Exception {
        var request = new TransactionCreateRequest("  ", new BigDecimal("1000.00"), WHEN,
                List.of(new SplitRequest(CATEGORY_ID, new BigDecimal("1000.00"))));

        mockMvc.perform(post("/v1/transactions")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.description").exists());

        verifyNoInteractions(transactionService);
    }

    @Test
    void createTransaction_whenAmountIsNotPositive_returns400() throws Exception {
        var request = new TransactionCreateRequest("Supermarket", BigDecimal.ZERO, WHEN,
                List.of(new SplitRequest(CATEGORY_ID, new BigDecimal("1000.00"))));

        mockMvc.perform(post("/v1/transactions")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void createTransaction_whenSplitsAreEmpty_returns400() throws Exception {
        var request = new TransactionCreateRequest("Supermarket", new BigDecimal("1000.00"), WHEN, List.of());

        mockMvc.perform(post("/v1/transactions")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.splits").exists());
    }

    @Test
    void createTransaction_whenSplitsAreMissing_returns400InsteadOfFailingWith500() throws Exception {
        mockMvc.perform(post("/v1/transactions")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Supermarket\",\"amount\":1000.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.splits").exists());
    }

    @Test
    void createTransaction_whenSplitIsInvalid_reportsFieldWithItsIndex() throws Exception {
        String body = """
                {"description":"Supermarket","amount":1000.00,
                 "splits":[{"amount":500.00},{"categoryId":10,"amount":-5}]}""";

        mockMvc.perform(post("/v1/transactions")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['splits[0].categoryId']").exists())
                .andExpect(jsonPath("$.errors['splits[1].amount']").exists());
    }

    @Test
    void createTransaction_whenSplitSumMismatch_returns400WithSplitSumCode() throws Exception {
        var request = createRequest();
        when(transactionService.createTransaction(request, OWNER_ID))
                .thenThrow(new TransactionSplitMismatchException("Split amount must be compare to transaction amount"));

        mockMvc.perform(post("/v1/transactions")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("NOT_VALID_TRANSACTION_SPLIT_SUM"));
    }

    @Test
    void createTransaction_whenCategoryNotFound_returns404() throws Exception {
        var request = createRequest();
        when(transactionService.createTransaction(request, OWNER_ID))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(post("/v1/transactions")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTransaction_whenCategoryBelongsToAnotherUser_returns404() throws Exception {
        var request = createRequest();
        when(transactionService.createTransaction(request, OWNER_ID))
                .thenThrow(new InaccessibleResourceException("Resource not Found"));

        mockMvc.perform(post("/v1/transactions")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTransaction_whenBodyIsMalformed_returns400() throws Exception {
        mockMvc.perform(post("/v1/transactions")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    @Test
    void createTransaction_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/v1/transactions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createRequest())))
                .andExpect(status().isUnauthorized());
    }

    // ==================== DELETE /v1/transactions/{id} ====================

    @Test
    void deleteTransaction_whenExists_returns200() throws Exception {
        mockMvc.perform(delete("/v1/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken())).with(csrf()))
                .andExpect(status().isOk());

        verify(transactionService).deleteTransactionById(TRANSACTION_ID, OWNER_ID);
    }

    @Test
    void deleteTransaction_whenIdNotPositive_returns400() throws Exception {
        mockMvc.perform(delete("/v1/transactions/{transId}", 0)
                        .with(authentication(authToken())).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteTransaction_whenNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Transaction not found"))
                .when(transactionService).deleteTransactionById(TRANSACTION_ID, OWNER_ID);

        mockMvc.perform(delete("/v1/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken())).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTransaction_whenBelongsToAnotherUser_returns404() throws Exception {
        doThrow(new InaccessibleResourceException("Resource not Found"))
                .when(transactionService).deleteTransactionById(TRANSACTION_ID, OWNER_ID);

        mockMvc.perform(delete("/v1/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken())).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ==================== PUT /v1/transactions/{id} ====================

    private TransactionUpdateRequest updateRequest() {
        return new TransactionUpdateRequest("Supermarket", new BigDecimal("1000.00"), WHEN,
                List.of(new SplitRequest(CATEGORY_ID, new BigDecimal("1000.00"))));
    }

    @Test
    void updateTransaction_whenValid_returnsUpdatedTransaction() throws Exception {
        var request = updateRequest();
        when(transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID)).thenReturn(transactionDto());

        mockMvc.perform(put("/v1/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TRANSACTION_ID))
                .andExpect(jsonPath("$.splits.length()").value(2));
    }

    @Test
    void updateTransaction_whenDateTimeIsMissing_returns400() throws Exception {
        var request = new TransactionUpdateRequest("Supermarket", new BigDecimal("1000.00"), null,
                List.of(new SplitRequest(CATEGORY_ID, new BigDecimal("1000.00"))));

        mockMvc.perform(put("/v1/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dateTime").exists());
    }

    @Test
    void updateTransaction_whenAmountIsMissing_returns400() throws Exception {
        var request = new TransactionUpdateRequest("Supermarket", null, WHEN,
                List.of(new SplitRequest(CATEGORY_ID, new BigDecimal("1000.00"))));

        mockMvc.perform(put("/v1/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void updateTransaction_whenSplitSumMismatch_returns400() throws Exception {
        var request = updateRequest();
        when(transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID))
                .thenThrow(new TransactionSplitMismatchException("mismatch"));

        mockMvc.perform(put("/v1/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("NOT_VALID_TRANSACTION_SPLIT_SUM"));
    }

    @Test
    void updateTransaction_whenNotFound_returns404() throws Exception {
        var request = updateRequest();
        when(transactionService.updateTransaction(request, TRANSACTION_ID, OWNER_ID))
                .thenThrow(new ResourceNotFoundException("Transaction not found"));

        mockMvc.perform(put("/v1/transactions/{transId}", TRANSACTION_ID)
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTransaction_whenIdNotPositive_returns400() throws Exception {
        mockMvc.perform(put("/v1/transactions/{transId}", 0)
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest())))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /v1/transactions/{id} ====================

    @Test
    void getTransaction_whenExists_returnsTransactionWithSplits() throws Exception {
        when(transactionService.getById(TRANSACTION_ID, OWNER_ID)).thenReturn(transactionDto());

        mockMvc.perform(get("/v1/transactions/{transId}", TRANSACTION_ID).with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TRANSACTION_ID))
                .andExpect(jsonPath("$.splits.length()").value(2));
    }

    @Test
    void getTransaction_whenIdNotPositive_returns400() throws Exception {
        mockMvc.perform(get("/v1/transactions/{transId}", -3).with(authentication(authToken())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransaction_whenNotFound_returns404() throws Exception {
        when(transactionService.getById(TRANSACTION_ID, OWNER_ID))
                .thenThrow(new ResourceNotFoundException("Transaction not found"));

        mockMvc.perform(get("/v1/transactions/{transId}", TRANSACTION_ID).with(authentication(authToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTransaction_whenBelongsToAnotherUser_returns404() throws Exception {
        when(transactionService.getById(TRANSACTION_ID, OWNER_ID))
                .thenThrow(new InaccessibleResourceException("Resource not Found"));

        mockMvc.perform(get("/v1/transactions/{transId}", TRANSACTION_ID).with(authentication(authToken())))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /v1/transactions (список с фильтрами) ====================

    @Test
    void getTransactions_withoutParameters_usesDefaultPageSortedByDateDescending() throws Exception {
        when(transactionService.getTransactions(eq(OWNER_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of(transactionDto()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/v1/transactions").with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].splits.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        var criteria = ArgumentCaptor.forClass(TransactionSearchCriteria.class);
        verify(transactionService).getTransactions(eq(OWNER_ID), criteria.capture(), pageable.capture());
        assertEquals(20, pageable.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("dateTime").getDirection());
        assertNull(criteria.getValue().getCategoryId());
        assertNull(criteria.getValue().getFrom());
    }

    @Test
    void getTransactions_bindsFiltersAndPagingFromQueryString() throws Exception {
        when(transactionService.getTransactions(eq(OWNER_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/v1/transactions")
                        .param("categoryId", "10")
                        .param("minAmount", "10.50")
                        .param("maxAmount", "100")
                        .param("from", "2026-10-01T00:00:00")
                        .param("to", "2026-10-31T23:59:59")
                        .param("q", "супермаркет")
                        .param("page", "1").param("size", "5").param("sort", "amount,asc")
                        .with(authentication(authToken())))
                .andExpect(status().isOk());

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        var criteria = ArgumentCaptor.forClass(TransactionSearchCriteria.class);
        verify(transactionService).getTransactions(eq(OWNER_ID), criteria.capture(), pageable.capture());
        assertEquals(10L, criteria.getValue().getCategoryId());
        assertEquals(new BigDecimal("10.50"), criteria.getValue().getMinAmount());
        assertEquals(new BigDecimal("100"), criteria.getValue().getMaxAmount());
        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0), criteria.getValue().getFrom());
        assertEquals(LocalDateTime.of(2026, 10, 31, 23, 59, 59), criteria.getValue().getTo());
        assertEquals("супермаркет", criteria.getValue().getQ());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
        assertEquals(Sort.Direction.ASC, pageable.getValue().getSort().getOrderFor("amount").getDirection());
    }

    @Test
    void getTransactions_whenDateHasWrongFormat_returns400() throws Exception {
        mockMvc.perform(get("/v1/transactions").param("from", "yesterday").with(authentication(authToken())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    @Test
    void getTransactions_whenServiceRejectsFilters_returns400WithInvalidRequestCode() throws Exception {
        when(transactionService.getTransactions(eq(OWNER_ID), any(), any()))
                .thenThrow(new InvalidRequestException("'from' must not be after 'to'"));

        mockMvc.perform(get("/v1/transactions")
                        .param("from", "2026-10-31T00:00:00").param("to", "2026-10-01T00:00:00")
                        .with(authentication(authToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void getTransactions_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/v1/transactions"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET /v1/transactions/total ====================

    @Test
    void getTotal_returnsSumForPeriod() throws Exception {
        when(transactionService.getTotalAmountByDateRange(OWNER_ID,
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 31, 23, 59, 59)))
                .thenReturn(new BigDecimal("123.45"));

        mockMvc.perform(get("/v1/transactions/total")
                        .param("from", "2026-10-01T00:00:00").param("to", "2026-10-31T23:59:59")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(content().string("123.45"));
    }

    @Test
    void getTotal_whenPeriodIsMissing_returns400WithMissingParameterCode() throws Exception {
        mockMvc.perform(get("/v1/transactions/total").param("from", "2026-10-01T00:00:00")
                        .with(authentication(authToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.errors.to").exists());
    }

    @Test
    void getTotal_whenDateHasWrongFormat_returns400WithInvalidParameterCode() throws Exception {
        mockMvc.perform(get("/v1/transactions/total")
                        .param("from", "2026-10-01").param("to", "2026-10-31T23:59:59")
                        .with(authentication(authToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.errors.from").exists());
    }

    @Test
    void getTotal_whenFromIsAfterTo_returns400() throws Exception {
        when(transactionService.getTotalAmountByDateRange(eq(OWNER_ID), any(), any()))
                .thenThrow(new InvalidRequestException("'from' must not be after 'to'"));

        mockMvc.perform(get("/v1/transactions/total")
                        .param("from", "2026-10-31T00:00:00").param("to", "2026-10-01T00:00:00")
                        .with(authentication(authToken())))
                .andExpect(status().isBadRequest());
    }
}
