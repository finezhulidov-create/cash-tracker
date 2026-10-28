package dev.zhulidov.cash_tracker.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.common.exception.InaccessibleResourceException;
import dev.zhulidov.cash_tracker.common.exception.ResourceAlreadyExistsException;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import dev.zhulidov.cash_tracker.transactions.controller.BudgetController;
import dev.zhulidov.cash_tracker.transactions.dto.*;
import dev.zhulidov.cash_tracker.transactions.service.BudgetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BudgetController.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BudgetService budgetService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final Long OWNER_ID = 1L;
    private static final Long BUDGET_ID = 5L;
    private static final Long CATEGORY_ID = 10L;
    private static final LocalDate OCTOBER = LocalDate.of(2026, 10, 1);

    private UsernamePasswordAuthenticationToken authToken() {
        var principal = new UserPrincipal(OWNER_ID, "owner@test.com", "hash");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private BudgetDto budgetDto() {
        return new BudgetDto(BUDGET_ID, new BigDecimal("5000.00"), OCTOBER,
                new CategoryDto(CATEGORY_ID, "Food", OWNER_ID));
    }

    // ==================== POST /v1/budgets ====================

    @Test
    void create_whenValid_returnsBudgetWithId() throws Exception {
        var request = new BudgetCreateRequest(new BigDecimal("5000.00"), OCTOBER, CATEGORY_ID);
        when(budgetService.createBudget(request, OWNER_ID)).thenReturn(budgetDto());

        mockMvc.perform(post("/v1/budgets")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BUDGET_ID))
                .andExpect(jsonPath("$.limitAmount").value(5000.00))
                .andExpect(jsonPath("$.period").value("2026-10-01"))
                .andExpect(jsonPath("$.category.id").value(CATEGORY_ID));
    }

    @Test
    void create_whenAmountIsNotPositive_returns400() throws Exception {
        var request = new BudgetCreateRequest(BigDecimal.ZERO, OCTOBER, CATEGORY_ID);

        mockMvc.perform(post("/v1/budgets")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(budgetService);
    }

    @Test
    void create_whenPeriodIsMissing_returns400() throws Exception {
        var request = new BudgetCreateRequest(new BigDecimal("100"), null, CATEGORY_ID);

        mockMvc.perform(post("/v1/budgets")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenCategoryIdIsMissing_createsOverallBudgetWithoutCategory() throws Exception {
        var request = new BudgetCreateRequest(new BigDecimal("30000.00"), OCTOBER, null);
        when(budgetService.createBudget(request, OWNER_ID))
                .thenReturn(new BudgetDto(7L, new BigDecimal("30000.00"), OCTOBER, null));

        mockMvc.perform(post("/v1/budgets")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.category").doesNotExist());
    }

    @Test
    void create_whenCategoryIdIsNotPositive_returns400() throws Exception {
        var request = new BudgetCreateRequest(new BigDecimal("100"), OCTOBER, 0L);

        mockMvc.perform(post("/v1/budgets")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.categoryId").exists());
    }

    @Test
    void create_whenCategoryNotFound_returns404() throws Exception {
        var request = new BudgetCreateRequest(new BigDecimal("100"), OCTOBER, CATEGORY_ID);
        when(budgetService.createBudget(request, OWNER_ID)).thenThrow(new ResourceNotFoundException("Category Not Found"));

        mockMvc.perform(post("/v1/budgets")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_whenBudgetAlreadyExists_returns409() throws Exception {
        var request = new BudgetCreateRequest(new BigDecimal("100"), OCTOBER, CATEGORY_ID);
        when(budgetService.createBudget(request, OWNER_ID))
                .thenThrow(new ResourceAlreadyExistsException("Budget for this category and month already exists"));

        mockMvc.perform(post("/v1/budgets")
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_ALREADY_EXISTS"));
    }

    @Test
    void create_whenUnauthenticated_returns401() throws Exception {
        var request = new BudgetCreateRequest(new BigDecimal("100"), OCTOBER, CATEGORY_ID);

        mockMvc.perform(post("/v1/budgets").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET /v1/budgets?yearMonth= ====================

    @Test
    void getBudgetsByMonth_returnsBudgetsOfThatMonth() throws Exception {
        when(budgetService.getBudgetsByMonth(OWNER_ID, YearMonth.of(2026, 10))).thenReturn(List.of(budgetDto()));

        mockMvc.perform(get("/v1/budgets").param("yearMonth", "2026-10").with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(BUDGET_ID));
    }

    @Test
    void getBudgetsByMonth_whenNoBudgets_returnsEmptyArray() throws Exception {
        when(budgetService.getBudgetsByMonth(OWNER_ID, YearMonth.of(2026, 10))).thenReturn(List.of());

        mockMvc.perform(get("/v1/budgets").param("yearMonth", "2026-10").with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getBudgetsByMonth_whenYearMonthIsMissing_returns400() throws Exception {
        mockMvc.perform(get("/v1/budgets").with(authentication(authToken())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBudgetsByMonth_whenYearMonthHasWrongFormat_returns400() throws Exception {
        mockMvc.perform(get("/v1/budgets").param("yearMonth", "2026-13").with(authentication(authToken())))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /v1/budgets/{id} ====================

    @Test
    void getBudget_whenOwned_returnsBudget() throws Exception {
        when(budgetService.getBudget(BUDGET_ID, OWNER_ID)).thenReturn(budgetDto());

        mockMvc.perform(get("/v1/budgets/{id}", BUDGET_ID).with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BUDGET_ID))
                .andExpect(jsonPath("$.category.categoryName").value("Food"));
    }

    @Test
    void getBudget_whenIdIsNotPositive_returns400() throws Exception {
        mockMvc.perform(get("/v1/budgets/{id}", 0).with(authentication(authToken())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(budgetService);
    }

    @Test
    void getBudget_whenNotFound_returns404() throws Exception {
        when(budgetService.getBudget(BUDGET_ID, OWNER_ID)).thenThrow(new ResourceNotFoundException("Budget not found"));

        mockMvc.perform(get("/v1/budgets/{id}", BUDGET_ID).with(authentication(authToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBudget_whenBelongsToAnotherUser_returns404() throws Exception {
        when(budgetService.getBudget(BUDGET_ID, OWNER_ID)).thenThrow(new InaccessibleResourceException("Resource not Found"));

        mockMvc.perform(get("/v1/budgets/{id}", BUDGET_ID).with(authentication(authToken())))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /v1/budgets/{id}/status ====================

    @Test
    void getStatus_returnsAssignedSpentAndAvailable() throws Exception {
        var status = new BudgetStatusDto(BUDGET_ID, OCTOBER, new CategoryDto(CATEGORY_ID, "Food", OWNER_ID),
                new BigDecimal("5000.00"), new BigDecimal("1200.50"), new BigDecimal("3799.50"));
        when(budgetService.getStatus(BUDGET_ID, OWNER_ID)).thenReturn(status);

        mockMvc.perform(get("/v1/budgets/{id}/status", BUDGET_ID).with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetId").value(BUDGET_ID))
                .andExpect(jsonPath("$.assigned").value(5000.00))
                .andExpect(jsonPath("$.spent").value(1200.50))
                .andExpect(jsonPath("$.available").value(3799.50));
    }

    @Test
    void getStatus_whenNotFound_returns404() throws Exception {
        when(budgetService.getStatus(BUDGET_ID, OWNER_ID)).thenThrow(new ResourceNotFoundException("Budget not found"));

        mockMvc.perform(get("/v1/budgets/{id}/status", BUDGET_ID).with(authentication(authToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStatus_whenIdIsNotPositive_returns400() throws Exception {
        mockMvc.perform(get("/v1/budgets/{id}/status", -1).with(authentication(authToken())))
                .andExpect(status().isBadRequest());
    }

    // ==================== PUT /v1/budgets/{id} ====================

    @Test
    void updateBudget_whenValid_returnsUpdatedBudget() throws Exception {
        var request = new BudgetUpdateRequest(new BigDecimal("7000.00"), OCTOBER);
        when(budgetService.updateBudget(request, OWNER_ID, BUDGET_ID))
                .thenReturn(new BudgetDto(BUDGET_ID, new BigDecimal("7000.00"), OCTOBER,
                        new CategoryDto(CATEGORY_ID, "Food", OWNER_ID)));

        mockMvc.perform(put("/v1/budgets/{id}", BUDGET_ID)
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limitAmount").value(7000.00));
    }

    @Test
    void updateBudget_whenLimitIsMissing_returns400() throws Exception {
        var request = new BudgetUpdateRequest(null, OCTOBER);

        mockMvc.perform(put("/v1/budgets/{id}", BUDGET_ID)
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(budgetService, never()).updateBudget(any(), any(), any());
    }

    @Test
    void updateBudget_whenPeriodIsMissing_returns400() throws Exception {
        var request = new BudgetUpdateRequest(new BigDecimal("100"), null);

        mockMvc.perform(put("/v1/budgets/{id}", BUDGET_ID)
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateBudget_whenTargetMonthAlreadyHasBudget_returns409() throws Exception {
        var request = new BudgetUpdateRequest(new BigDecimal("100"), OCTOBER);
        when(budgetService.updateBudget(eq(request), eq(OWNER_ID), eq(BUDGET_ID)))
                .thenThrow(new ResourceAlreadyExistsException("Budget for this category and month already exists"));

        mockMvc.perform(put("/v1/budgets/{id}", BUDGET_ID)
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateBudget_whenNotFound_returns404() throws Exception {
        var request = new BudgetUpdateRequest(new BigDecimal("100"), OCTOBER);
        when(budgetService.updateBudget(request, OWNER_ID, BUDGET_ID))
                .thenThrow(new ResourceNotFoundException("Budget not found"));

        mockMvc.perform(put("/v1/budgets/{id}", BUDGET_ID)
                        .with(authentication(authToken())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE /v1/budgets/{id} ====================

    @Test
    void deleteBudget_whenOwned_returns200AndDeletes() throws Exception {
        mockMvc.perform(delete("/v1/budgets/{id}", BUDGET_ID)
                        .with(authentication(authToken())).with(csrf()))
                .andExpect(status().isOk());

        verify(budgetService).deleteBudget(BUDGET_ID, OWNER_ID);
    }

    @Test
    void deleteBudget_whenNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Budget not found")).when(budgetService).deleteBudget(BUDGET_ID, OWNER_ID);

        mockMvc.perform(delete("/v1/budgets/{id}", BUDGET_ID)
                        .with(authentication(authToken())).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBudget_whenIdIsNotPositive_returns400() throws Exception {
        mockMvc.perform(delete("/v1/budgets/{id}", 0)
                        .with(authentication(authToken())).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteBudget_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/v1/budgets/{id}", BUDGET_ID).with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
