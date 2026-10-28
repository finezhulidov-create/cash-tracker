package dev.zhulidov.cash_tracker.app.config;

import dev.zhulidov.cash_tracker.app.controller.AuthController;
import dev.zhulidov.cash_tracker.app.controller.UserController;
import dev.zhulidov.cash_tracker.app.dto.UserDto;
import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.app.service.UserService;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Проверяет настоящий SecurityConfig вместе с настоящими JwtFilter и RateLimitFilter (в отличие от остальных
 * контроллерных тестов, где конфигурация безопасности подменяется стандартной). Docker не нужен.
 */
@WebMvcTest({UserController.class, AuthController.class})
@Import({SecurityConfig.class, AppConfig.class})
class SecurityConfigTest {

    private static final String ORIGIN = "http://localhost:3000";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    private StringRedisTemplate redis;

    private ValueOperations<String, String> ops;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L);
    }

    private void givenValidTokenOfExistingUser() {
        when(jwtService.isTokenValid("good")).thenReturn(true);
        when(jwtService.extractEmail("good")).thenReturn("alex@test.com");
        when(customUserDetailsService.loadUserByUsername("alex@test.com"))
                .thenReturn(new UserPrincipal(1L, "alex@test.com", "hash"));
    }

    // ==================== аутентификация: 401 в формате ErrorResponse ====================

    @Test
    void protectedEndpoint_withoutToken_returns401WithJsonError() throws Exception {
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401() throws Exception {
        when(jwtService.isTokenValid("bad")).thenReturn(false);

        mockMvc.perform(get("/v1/users/me").header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void protectedEndpoint_withTokenOfDeletedUser_returns401InsteadOf500() throws Exception {
        when(jwtService.isTokenValid("good")).thenReturn(true);
        when(jwtService.extractEmail("good")).thenReturn("ghost@test.com");
        when(customUserDetailsService.loadUserByUsername("ghost@test.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        mockMvc.perform(get("/v1/users/me").header("Authorization", "Bearer good"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidToken_isAllowed() throws Exception {
        givenValidTokenOfExistingUser();
        when(userService.getUserById(1L)).thenReturn(new UserDto("Alex", "alex@test.com", BigDecimal.ZERO));

        mockMvc.perform(get("/v1/users/me").header("Authorization", "Bearer good"))
                .andExpect(status().isOk());
    }

    @Test
    void stateChangingRequest_doesNotRequireCsrfToken() throws Exception {
        givenValidTokenOfExistingUser();
        when(userService.updateUserBalance(1L, BigDecimal.TEN)).thenReturn(BigDecimal.TEN);

        mockMvc.perform(put("/v1/users/me/balance")
                        .header("Authorization", "Bearer good")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\": 10}"))
                .andExpect(status().isOk());
    }

    // ==================== публичные и закрытые адреса ====================

    @Test
    void authEndpoints_arePublic() throws Exception {
        // пустое тело даёт 400 от валидации, то есть запрос прошёл через безопасность без токена
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void swaggerAndApiDocs_arePublic() throws Exception {
        // в срезе WebMvcTest springdoc не загружен, поэтому 404 (не 401) означает, что безопасность пропустила запрос
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }

    @Test
    void actuatorHealthAndInfo_arePublicButOtherActuatorEndpointsAreNot() throws Exception {
        // в срезе actuator не загружен: 404 означает «безопасность пропустила», 401 — «закрыто»
        mockMvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    }

    // ==================== CORS ====================

    @Test
    void cors_preflightFromConfiguredOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/v1/users/me")
                        .header("Origin", ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN));
    }

    @Test
    void cors_patchMethodIsAllowed() throws Exception {
        mockMvc.perform(options("/v1/users/me")
                        .header("Origin", ORIGIN)
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk());
    }

    @Test
    void cors_preflightFromUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/v1/users/me")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    // ==================== заголовки безопасности ====================

    @Test
    void responses_carryNosniffHeader() throws Exception {
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    // ==================== рейт-лимит ====================

    @Test
    void authEndpoints_overTheLimit_return429WithRetryAfter() throws Exception {
        when(ops.increment(anyString())).thenReturn(31L);

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void authEndpoints_exactlyAtTheLimit_areStillAllowed() throws Exception {
        when(ops.increment(anyString())).thenReturn(30L);

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rateLimit_doesNotAffectOtherEndpoints() throws Exception {
        when(ops.increment(anyString())).thenReturn(1000L);
        givenValidTokenOfExistingUser();
        when(userService.getUserById(1L)).thenReturn(new UserDto("Alex", "alex@test.com", BigDecimal.ZERO));

        mockMvc.perform(get("/v1/users/me").header("Authorization", "Bearer good"))
                .andExpect(status().isOk());
    }

    // ==================== прочее ====================

    @Test
    void passwordEncoder_isBcrypt() {
        String hash = passwordEncoder.encode("secret");

        assertTrue(hash.startsWith("$2"), "BCrypt-хэш начинается с $2, а был " + hash);
        assertTrue(passwordEncoder.matches("secret", hash));
        assertFalse(passwordEncoder.matches("other", hash));
    }
}
