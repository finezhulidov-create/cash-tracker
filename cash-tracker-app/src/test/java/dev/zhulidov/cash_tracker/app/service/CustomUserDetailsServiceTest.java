package dev.zhulidov.cash_tracker.app.service;

import dev.zhulidov.cash_tracker.app.model.User;
import dev.zhulidov.cash_tracker.app.repository.UserRepository;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_whenUserExists_returnsPrincipalWithIdEmailAndPasswordHash() {
        var user = User.builder().id(7L).name("Alex").email("alex@test.com").password("hash").build();
        when(repository.findByEmailIgnoreCase("alex@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alex@test.com");

        var principal = assertInstanceOf(UserPrincipal.class, details);
        assertEquals(7L, principal.getId());
        assertEquals("alex@test.com", principal.getUsername());
        assertEquals("hash", principal.getPassword());
    }

    @Test
    void loadUserByUsername_whenUserDoesNotExist_throwsUsernameNotFoundExceptionWithoutRevealingEmail() {
        when(repository.findByEmailIgnoreCase("ghost@test.com")).thenReturn(Optional.empty());

        var ex = assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ghost@test.com"));

        assertFalse(ex.getMessage().contains("ghost@test.com"));
    }
}
