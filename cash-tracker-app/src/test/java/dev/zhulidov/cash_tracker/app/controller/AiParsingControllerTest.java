package dev.zhulidov.cash_tracker.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.common.exception.InvalidRequestException;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import dev.zhulidov.cash_tracker.transactions.controller.AiParsingController;
import dev.zhulidov.cash_tracker.transactions.dto.ImportPreviewItem;
import dev.zhulidov.cash_tracker.transactions.dto.ReviewedTransactionRequest;
import dev.zhulidov.cash_tracker.transactions.dto.SplitRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionCreateRequest;
import dev.zhulidov.cash_tracker.transactions.dto.TransactionDto;
import dev.zhulidov.cash_tracker.transactions.importpdf.AIParsingService;
import dev.zhulidov.cash_tracker.transactions.importpdf.PacketTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiParsingController.class)
class AiParsingControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AIParsingService parsingService;
    @MockitoBean
    private PacketTransactionService packetTransactionService;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final Long OWNER_ID = 1L;
    private static final Long CATEGORY_ID = 10L;
    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 10, 28, 14, 30);

    private UsernamePasswordAuthenticationToken authToken() {
        var principal = new UserPrincipal(OWNER_ID, "owner@test.com", "hash");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("file", "statement.pdf", "application/pdf", new byte[]{'%', 'P', 'D', 'F'});
    }

    private static TransactionCreateRequest request(String description, String amount) {
        BigDecimal sum = new BigDecimal(amount);
        return new TransactionCreateRequest(description, sum, WHEN, List.of(new SplitRequest(CATEGORY_ID, sum)));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    // ==================== POST /v1/parsing ====================

    @Test
    void parse_returnsPreviewWithDuplicateFlags() throws Exception {
        var splits = List.of(new SplitRequest(CATEGORY_ID, new BigDecimal("350.00")));
        when(parsingService.generateTransactions(any(), eq(OWNER_ID))).thenReturn(List.of(
                new ImportPreviewItem("Кофейня", new BigDecimal("350.00"), WHEN, splits, true),
                new ImportPreviewItem("Кофейня", new BigDecimal("350.00"), WHEN, splits, false)));

        mockMvc.perform(multipart("/v1/parsing").file(pdf())
                        .with(authentication(authToken())).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].description").value("Кофейня"))
                .andExpect(jsonPath("$[0].amount").value(350.00))
                .andExpect(jsonPath("$[0].dateTime").value("2026-10-28T14:30:00"))
                .andExpect(jsonPath("$[0].splits[0].categoryId").value(CATEGORY_ID))
                .andExpect(jsonPath("$[0].duplicate").value(true))
                .andExpect(jsonPath("$[1].duplicate").value(false));
    }

    @Test
    void parse_passesUploadedFileOfCurrentUser() throws Exception {
        when(parsingService.generateTransactions(any(), any())).thenReturn(List.of());

        mockMvc.perform(multipart("/v1/parsing").file(pdf())
                        .with(authentication(authToken())).with(csrf()))
                .andExpect(status().isOk());

        verify(parsingService).generateTransactions(argThat(file ->
                "statement.pdf".equals(file.getOriginalFilename()) && file.getSize() == 4), eq(OWNER_ID));
    }

    @Test
    void parse_withoutFile_returns400() throws Exception {
        mockMvc.perform(multipart("/v1/parsing")
                        .with(authentication(authToken())).with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(parsingService);
    }

    @Test
    void parse_notPdf_returns400WithErrorCode() throws Exception {
        when(parsingService.generateTransactions(any(), any()))
                .thenThrow(new InvalidRequestException("Поддержка только пдф файлов"));

        mockMvc.perform(multipart("/v1/parsing").file(pdf())
                        .with(authentication(authToken())).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void parse_unauthenticated_returns401() throws Exception {
        mockMvc.perform(multipart("/v1/parsing").file(pdf()).with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(parsingService);
    }

    // ==================== POST /v1/parsing/save ====================

    @Test
    void save_createsTransactionsOfCurrentUser() throws Exception {
        var coffee = request("Кофейня", "350.00");
        var body = new ReviewedTransactionRequest(List.of(coffee));
        when(packetTransactionService.createPacketTransactions(body, OWNER_ID)).thenReturn(List.of(
                new TransactionDto(100L, OWNER_ID, coffee.amount(), coffee.description(), WHEN, List.of())));

        mockMvc.perform(post("/v1/parsing/save")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].description").value("Кофейня"));
    }

    @Test
    void save_emptyList_returns400() throws Exception {
        mockMvc.perform(post("/v1/parsing/save")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReviewedTransactionRequest(List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.transactions").exists());

        verifyNoInteractions(packetTransactionService);
    }

    @Test
    void save_tooManyTransactions_returns400() throws Exception {
        var tooMany = Collections.nCopies(1001, request("Кофейня", "350.00"));

        mockMvc.perform(post("/v1/parsing/save")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReviewedTransactionRequest(tooMany))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.transactions").exists());

        verifyNoInteractions(packetTransactionService);
    }

    @Test
    void save_validatesEveryTransaction() throws Exception {
        // пользователь мог отредактировать предпросмотр как угодно: каждая операция проверяется как обычная трата
        var good = request("Кофейня", "350.00");
        var negative = request("Возврат", "-100.00");
        var blank = request("  ", "100.00");

        mockMvc.perform(post("/v1/parsing/save")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReviewedTransactionRequest(List.of(good, negative, blank)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors['transactions[1].amount']").exists())
                .andExpect(jsonPath("$.errors['transactions[2].description']").exists());

        verifyNoInteractions(packetTransactionService);
    }

    @Test
    void save_foreignCategory_returns404() throws Exception {
        var body = new ReviewedTransactionRequest(List.of(request("Кофейня", "350.00")));
        when(packetTransactionService.createPacketTransactions(body, OWNER_ID))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(post("/v1/parsing/save")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void save_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/v1/parsing/save").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReviewedTransactionRequest(List.of(request("Кофейня", "350.00"))))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(packetTransactionService);
    }
}
