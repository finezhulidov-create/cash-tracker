package dev.zhulidov.cash_tracker.app.security;

import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private FilterChain chain;

    private JwtFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_whenNoAuthorizationHeader_continuesChainWithoutAuthentication() throws Exception {
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void doFilter_whenHeaderIsNotBearer_continuesChainWithoutAuthentication() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void doFilter_whenTokenIsValid_setsAuthenticationWithUserPrincipal() throws Exception {
        request.addHeader("Authorization", "Bearer good-token");
        var principal = new UserPrincipal(7L, "alex@test.com", "hash");
        when(jwtService.isTokenValid("good-token")).thenReturn(true);
        when(jwtService.extractEmail("good-token")).thenReturn("alex@test.com");
        when(userDetailsService.loadUserByUsername("alex@test.com")).thenReturn(principal);

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertSame(principal, authentication.getPrincipal());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_whenUserOfValidTokenWasDeleted_continuesChainWithoutAuthenticationInsteadOfFailing() throws Exception {
        // регрессия: раньше здесь вылетало ResourceNotFoundException и клиент получал 500
        request.addHeader("Authorization", "Bearer good-token");
        when(jwtService.isTokenValid("good-token")).thenReturn(true);
        when(jwtService.extractEmail("good-token")).thenReturn("ghost@test.com");
        when(userDetailsService.loadUserByUsername("ghost@test.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_whenTokenIsInvalid_continuesChainWithoutAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer bad-token");
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
    }
}
