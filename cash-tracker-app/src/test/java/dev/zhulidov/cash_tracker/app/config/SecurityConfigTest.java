package dev.zhulidov.cash_tracker.app.config;

import dev.zhulidov.cash_tracker.app.controller.AuthController;
import dev.zhulidov.cash_tracker.app.controller.UserController;
import dev.zhulidov.cash_tracker.app.dto.UserDto;
import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.app.service.UserService;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Проверяет настоящий SecurityConfig вместе с настоящим JwtFilter (в отличие от остальных
 * контроллерных тестов, где конфигурация безопасности подменяется стандартной). Docker не нужен.
 */
@WebMvcTest({UserController.class, AuthController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

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

    private static final String ORIGIN = "http://localhost:3000";

    @Test
    void protectedEndpoint_withoutToken_isForbidden() throws Exception {
        // Фиксируем фактическое поведение: в цепочке не настроен AuthenticationEntryPoint,
        // поэтому анонимный запрос получает 403, а не 401. Фронту это нужно учитывать
        // (или добавить entry point, возвращающий 401).
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_withInvalidToken_isForbidden() throws Exception {
        when(jwtService.isTokenValid("bad")).thenReturn(false);

        mockMvc.perform(get("/v1/users/me").header("Authorization", "Bearer bad"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_withValidToken_isAllowed() throws Exception {
        when(jwtService.isTokenValid("good")).thenReturn(true);
        when(jwtService.extractEmail("good")).thenReturn("alex@test.com");
        when(customUserDetailsService.loadUserByUsername("alex@test.com"))
                .thenReturn(new UserPrincipal(1L, "alex@test.com", "hash"));
        when(userService.getUserById(1L)).thenReturn(new UserDto("Alex", "alex@test.com", BigDecimal.ZERO));

        mockMvc.perform(get("/v1/users/me").header("Authorization", "Bearer good"))
                .andExpect(status().isOk());
    }

    @Test
    void stateChangingRequest_doesNotRequireCsrfToken() throws Exception {
        when(jwtService.isTokenValid("good")).thenReturn(true);
        when(jwtService.extractEmail("good")).thenReturn("alex@test.com");
        when(customUserDetailsService.loadUserByUsername("alex@test.com"))
                .thenReturn(new UserPrincipal(1L, "alex@test.com", "hash"));
        when(userService.updateUserBalance(1L, BigDecimal.TEN)).thenReturn(BigDecimal.TEN);

        mockMvc.perform(post("/v1/users/balance")
                        .header("Authorization", "Bearer good")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("10"))
                .andExpect(status().isOk());
    }

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
        // в срезе WebMvcTest springdoc не загружен, поэтому 404 (не 403) означает, что безопасность пропустила запрос
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }

    @Test
    void cors_preflightFromAnyOriginIsAllowedForConfiguredMethods() throws Exception {
        mockMvc.perform(options("/v1/users/me")
                        .header("Origin", ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    @Test
    void cors_preflightForMethodThatIsNotConfiguredIsRejected() throws Exception {
        mockMvc.perform(options("/v1/users/me")
                        .header("Origin", ORIGIN)
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isForbidden());
    }

    @Test
    void passwordEncoder_isBcrypt() {
        String hash = passwordEncoder.encode("secret");

        assertTrue(hash.startsWith("$2"), "BCrypt-хэш начинается с $2, а был " + hash);
        assertTrue(passwordEncoder.matches("secret", hash));
        assertFalse(passwordEncoder.matches("other", hash));
    }
}
