package dev.zhulidov.cash_tracker.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.zhulidov.cash_tracker.app.dto.LoginRequest;
import dev.zhulidov.cash_tracker.app.dto.RegisterUserRequest;
import dev.zhulidov.cash_tracker.app.dto.RegisterUserResponse;
import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.app.service.UserService;
import dev.zhulidov.cash_tracker.common.exception.ResourceAlreadyExistsException;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /auth/** публичные, поэтому фильтры безопасности отключены (addFilters = false):
 * проверяем только контроллер, валидацию и формат ответа. Поведение самой цепочки
 * безопасности (401/403, JWT) проверяют SecurityConfigTest и интеграционные тесты.
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

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

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
                        .content(json(new LoginRequest("alex@test.com", "secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400));
    }

    @Test
    void login_normalizesEmailToLowerCaseBeforeAuthenticating() throws Exception {
        var principal = new UserPrincipal(7L, "alex@test.com", "hash");
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(jwtService.generateToken("alex@test.com", 7L)).thenReturn("jwt-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("ALEX@Test.COM", "secret"))))
                .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("alex@test.com", captor.getValue().getName());
    }

    @Test
    void login_whenCredentialsAreWrong_returns401WithInvalidCredentialsCode() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("alex@test.com", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_whenEmailIsNotValid_returns400WithFieldErrorAndDoesNotAuthenticate() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("not-an-email", "secret"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.email").exists());

        verifyNoInteractions(authenticationManager);
    }

    @Test
    void login_whenPasswordIsBlank_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("alex@test.com", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void login_whenBodyIsMissing_returns400() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    // ==================== POST /auth/register ====================

    @Test
    void register_whenRequestIsValid_returnsTokenImmediately() throws Exception {
        var request = new RegisterUserRequest("alex@test.com", "Alex", "secret-password");
        when(userService.createUser(request)).thenReturn(new RegisterUserResponse(7L, "Alex", "alex@test.com"));
        when(jwtService.generateToken("alex@test.com", 7L)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400));
    }

    @Test
    void register_whenEmailIsAlreadyRegistered_returns409WithEmailCode() throws Exception {
        var request = new RegisterUserRequest("alex@test.com", "Alex", "secret-password");
        when(userService.createUser(request))
                .thenThrow(new ResourceAlreadyExistsException("EMAIL_ALREADY_EXISTS", "Email is already registered"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));

        verifyNoInteractions(jwtService);
    }

    @Test
    void register_whenEmailIsNotValid_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterUserRequest("not-an-email", "Alex", "secret-password"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void register_whenUsernameIsTooShort_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterUserRequest("alex@test.com", "A", "secret-password"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    @Test
    void register_whenPasswordIsShorterThanEightCharacters_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterUserRequest("alex@test.com", "Alex", "1234567"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void register_whenPasswordIsLongerThanBcryptLimit_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterUserRequest("alex@test.com", "Alex", "a".repeat(73)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void register_whenPasswordIsExactlyEightCharacters_isAccepted() throws Exception {
        var request = new RegisterUserRequest("alex@test.com", "Alex", "12345678");
        when(userService.createUser(request)).thenReturn(new RegisterUserResponse(7L, "Alex", "alex@test.com"));
        when(jwtService.generateToken("alex@test.com", 7L)).thenReturn("jwt-token");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk());
    }
}
