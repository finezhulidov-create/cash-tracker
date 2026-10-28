package dev.zhulidov.cash_tracker.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zhulidov.cash_tracker.app.dto.LoginRequest;
import dev.zhulidov.cash_tracker.app.dto.RegisterUserRequest;
import dev.zhulidov.cash_tracker.app.dto.RegisterUserResponse;
import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.app.service.UserService;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /auth/** публичные, поэтому фильтры безопасности отключены (addFilters = false):
 * проверяем только контроллер, валидацию и формат ответа. Поведение самой цепочки
 * безопасности (401/403, JWT) проверяется интеграционными тестами.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserService userService;
    // JwtFilter создаётся как бин, поэтому его зависимость нужно замокать
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // ==================== POST /auth/login ====================

    @Test
    void login_whenCredentialsAreValid_returnsTokenResponse() throws Exception {
        var principal = new UserPrincipal(7L, "alex@test.com", "hash");
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(jwtService.generateToken("alex@test.com", 7L)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("alex@test.com", "secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400));
    }

    @Test
    void login_whenEmailIsNotValid_returns400AndDoesNotAuthenticate() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("not-an-email", "secret"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authenticationManager);
    }

    @Test
    void login_whenPasswordIsBlank_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("alex@test.com", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_whenBodyIsMissing_returns400() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ==================== POST /auth/register ====================

    @Test
    void register_whenRequestIsValid_returnsNameAndEmail() throws Exception {
        var request = new RegisterUserRequest("alex@test.com", "Alex", "secret-password");
        when(userService.createUser(request)).thenReturn(new RegisterUserResponse("Alex", "alex@test.com"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alex"))
                .andExpect(jsonPath("$.email").value("alex@test.com"));
    }

    @Test
    void register_whenEmailIsNotValid_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterUserRequest("not-an-email", "Alex", "secret-password"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void register_whenUsernameIsTooShort_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterUserRequest("alex@test.com", "A", "secret-password"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_whenPasswordIsBlank_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterUserRequest("alex@test.com", "Alex", ""))))
                .andExpect(status().isBadRequest());
    }
}
