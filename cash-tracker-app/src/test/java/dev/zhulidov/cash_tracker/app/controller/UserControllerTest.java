package dev.zhulidov.cash_tracker.app.controller;

import dev.zhulidov.cash_tracker.app.dto.UserDto;
import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.app.service.UserService;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private static final Long OWNER_ID = 1L;

    private UsernamePasswordAuthenticationToken authToken() {
        var principal = new UserPrincipal(OWNER_ID, "owner@test.com", "hash");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    // ==================== GET /v1/users/me ====================

    @Test
    void getUser_returnsNameEmailAndBalance() throws Exception {
        when(userService.getUserById(OWNER_ID))
                .thenReturn(new UserDto("Alex", "alex@test.com", new BigDecimal("250.75")));

        mockMvc.perform(get("/v1/users/me").with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alex"))
                .andExpect(jsonPath("$.email").value("alex@test.com"))
                .andExpect(jsonPath("$.balance").value(250.75));
    }

    @Test
    void getUser_whenUserWasDeleted_returns404() throws Exception {
        when(userService.getUserById(OWNER_ID)).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/v1/users/me").with(authentication(authToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUser_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== DELETE /v1/users/delete ====================

    @Test
    void deleteUser_deletesCurrentUser() throws Exception {
        mockMvc.perform(delete("/v1/users/delete")
                        .with(authentication(authToken()))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(userService).deleteUser(OWNER_ID);
    }

    @Test
    void deleteUser_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/v1/users/delete").with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    // ==================== PUT /v1/users/me/balance ====================

    @Test
    void updateBalance_whenAmountIsValid_returnsNewBalanceAsObject() throws Exception {
        when(userService.updateUserBalance(OWNER_ID, new BigDecimal("1500.50"))).thenReturn(new BigDecimal("1500.50"));

        mockMvc.perform(put("/v1/users/me/balance")
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\": 1500.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500.50));
    }

    @Test
    void updateBalance_whenAmountIsZero_isAllowed() throws Exception {
        when(userService.updateUserBalance(OWNER_ID, BigDecimal.ZERO)).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(put("/v1/users/me/balance")
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\": 0}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateBalance_whenAmountIsNegative_returns400WithFieldErrorAndDoesNotUpdate() throws Exception {
        mockMvc.perform(put("/v1/users/me/balance")
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\": -10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.balance").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void updateBalance_whenBalanceFieldIsMissing_returns400() throws Exception {
        mockMvc.perform(put("/v1/users/me/balance")
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.balance").exists());
    }

    @Test
    void updateBalance_whenBodyIsMissing_returns400() throws Exception {
        mockMvc.perform(put("/v1/users/me/balance")
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateBalance_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/v1/users/me/balance")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\": 100}"))
                .andExpect(status().isUnauthorized());
    }
}
