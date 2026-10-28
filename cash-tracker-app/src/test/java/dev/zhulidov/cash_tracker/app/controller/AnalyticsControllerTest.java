package dev.zhulidov.cash_tracker.app.controller;

import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.common.exception.InvalidRequestException;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import dev.zhulidov.cash_tracker.transactions.controller.AnalyticsController;
import dev.zhulidov.cash_tracker.transactions.dto.BudgetStatusDto;
import dev.zhulidov.cash_tracker.transactions.dto.CategoryDto;
import dev.zhulidov.cash_tracker.transactions.dto.CategorySpendingDto;
import dev.zhulidov.cash_tracker.transactions.dto.MonthSummaryDto;
import dev.zhulidov.cash_tracker.transactions.dto.MonthlySpendingDto;
import dev.zhulidov.cash_tracker.transactions.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final Long OWNER_ID = 1L;

    private UsernamePasswordAuthenticationToken authToken() {
        var principal = new UserPrincipal(OWNER_ID, "owner@test.com", "hash");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    // ==================== GET /v1/analytics/by-category ====================

    @Test
    void byCategory_returnsSpendingPerCategory() throws Exception {
        when(analyticsService.spendingByCategory(OWNER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(new CategorySpendingDto(10L, "Food", new BigDecimal("1700.50")),
                        new CategorySpendingDto(20L, "Home", new BigDecimal("300.00"))));

        mockMvc.perform(get("/v1/analytics/by-category")
                        .param("from", "2026-10-01").param("to", "2026-10-31")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].categoryId").value(10))
                .andExpect(jsonPath("$[0].categoryName").value("Food"))
                .andExpect(jsonPath("$[0].spent").value(1700.50));
    }

    @Test
    void byCategory_whenNothingSpent_returnsEmptyArray() throws Exception {
        when(analyticsService.spendingByCategory(eq(OWNER_ID), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/v1/analytics/by-category")
                        .param("from", "2026-10-01").param("to", "2026-10-31")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void byCategory_whenDateIsMissing_returns400() throws Exception {
        mockMvc.perform(get("/v1/analytics/by-category").param("from", "2026-10-01")
                        .with(authentication(authToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"));

        verifyNoInteractions(analyticsService);
    }

    @Test
    void byCategory_whenDateHasWrongFormat_returns400() throws Exception {
        mockMvc.perform(get("/v1/analytics/by-category")
                        .param("from", "01.10.2026").param("to", "2026-10-31")
                        .with(authentication(authToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"));
    }

    @Test
    void byCategory_whenFromIsAfterTo_returns400() throws Exception {
        when(analyticsService.spendingByCategory(eq(OWNER_ID), any(), any()))
                .thenThrow(new InvalidRequestException("'from' must not be after 'to'"));

        mockMvc.perform(get("/v1/analytics/by-category")
                        .param("from", "2026-10-31").param("to", "2026-10-01")
                        .with(authentication(authToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void byCategory_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/v1/analytics/by-category").param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET /v1/analytics/monthly ====================

    @Test
    void monthly_returnsTwelveMonthsWithYearMonthAsString() throws Exception {
        var months = IntStream.rangeClosed(1, 12)
                .mapToObj(m -> new MonthlySpendingDto(YearMonth.of(2026, m), BigDecimal.valueOf(m * 100L)))
                .toList();
        when(analyticsService.monthlySpending(OWNER_ID, 2026)).thenReturn(months);

        mockMvc.perform(get("/v1/analytics/monthly").param("year", "2026").with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].month").value("2026-01"))
                .andExpect(jsonPath("$[9].month").value("2026-10"))
                .andExpect(jsonPath("$[9].spent").value(1000));
    }

    @Test
    void monthly_whenYearIsOutOfRange_returns400() throws Exception {
        mockMvc.perform(get("/v1/analytics/monthly").param("year", "1999").with(authentication(authToken())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/v1/analytics/monthly").param("year", "2101").with(authentication(authToken())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(analyticsService);
    }

    @Test
    void monthly_whenYearIsMissingOrNotANumber_returns400() throws Exception {
        mockMvc.perform(get("/v1/analytics/monthly").with(authentication(authToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"));
        mockMvc.perform(get("/v1/analytics/monthly").param("year", "abc").with(authentication(authToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"));
    }

    // ==================== GET /v1/analytics/summary ====================

    @Test
    void summary_returnsTotalOverallBudgetAndCategoryBudgets() throws Exception {
        var food = new CategoryDto(10L, "Food", OWNER_ID);
        var period = LocalDate.of(2026, 10, 1);
        var summary = new MonthSummaryDto(YearMonth.of(2026, 10), new BigDecimal("12000.00"),
                new BudgetStatusDto(3L, period, null, new BigDecimal("30000.00"),
                        new BigDecimal("12000.00"), new BigDecimal("18000.00")),
                List.of(new BudgetStatusDto(1L, period, food, new BigDecimal("5000.00"),
                        new BigDecimal("700.00"), new BigDecimal("4300.00"))));
        when(analyticsService.monthSummary(OWNER_ID, YearMonth.of(2026, 10))).thenReturn(summary);

        mockMvc.perform(get("/v1/analytics/summary").param("yearMonth", "2026-10").with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearMonth").value("2026-10"))
                .andExpect(jsonPath("$.totalSpent").value(12000.00))
                .andExpect(jsonPath("$.overall.available").value(18000.00))
                .andExpect(jsonPath("$.overall.category").doesNotExist())
                .andExpect(jsonPath("$.categories.length()").value(1))
                .andExpect(jsonPath("$.categories[0].category.categoryName").value("Food"))
                .andExpect(jsonPath("$.categories[0].available").value(4300.00));
    }

    @Test
    void summary_whenNoOverallBudget_overallIsNull() throws Exception {
        when(analyticsService.monthSummary(OWNER_ID, YearMonth.of(2026, 10)))
                .thenReturn(new MonthSummaryDto(YearMonth.of(2026, 10), BigDecimal.ZERO, null, List.of()));

        mockMvc.perform(get("/v1/analytics/summary").param("yearMonth", "2026-10").with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overall").doesNotExist())
                .andExpect(jsonPath("$.categories.length()").value(0));
    }

    @Test
    void summary_whenYearMonthHasWrongFormat_returns400() throws Exception {
        mockMvc.perform(get("/v1/analytics/summary").param("yearMonth", "2026-13").with(authentication(authToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"));
    }

    @Test
    void summary_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/v1/analytics/summary").param("yearMonth", "2026-10"))
                .andExpect(status().isUnauthorized());
    }
}
