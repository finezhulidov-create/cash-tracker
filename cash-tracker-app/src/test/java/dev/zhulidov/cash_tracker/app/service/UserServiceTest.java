package dev.zhulidov.cash_tracker.app.service;

import dev.zhulidov.cash_tracker.app.dto.RegisterUserRequest;
import dev.zhulidov.cash_tracker.app.model.User;
import dev.zhulidov.cash_tracker.app.repository.UserRepository;
import dev.zhulidov.cash_tracker.common.exception.ResourceAlreadyExistsException;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;
    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserService userService;

    private static final Long USER_ID = 1L;

    private User user() {
        return User.builder().id(USER_ID).name("Alex").email("alex@test.com").password("hash").build();
    }

    // ==================== createUser ====================

    @Test
    void createUser_encodesPasswordSavesUserWithZeroBalanceAndReturnsIdNameEmail() {
        var request = new RegisterUserRequest("alex@test.com", "Alex", "plain-password");
        when(repository.existsByEmailIgnoreCase("alex@test.com")).thenReturn(false);
        when(encoder.encode("plain-password")).thenReturn("encoded");
        when(repository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        var response = userService.createUser(request);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        var saved = captor.getValue();
        assertEquals("alex@test.com", saved.getEmail());
        assertEquals("Alex", saved.getName());
        assertEquals("encoded", saved.getPassword());
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getBalance()));
        assertEquals(7L, response.id());
        assertEquals("Alex", response.name());
        assertEquals("alex@test.com", response.email());
    }

    @Test
    void createUser_storesEmailInLowerCaseAndTrimsName() {
        var request = new RegisterUserRequest("  Alex@Test.COM ", " Alex ", "plain-password");
        when(repository.existsByEmailIgnoreCase("alex@test.com")).thenReturn(false);
        when(encoder.encode(any())).thenReturn("encoded");
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.createUser(request);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertEquals("alex@test.com", captor.getValue().getEmail());
        assertEquals("Alex", captor.getValue().getName());
    }

    @Test
    void createUser_whenEmailAlreadyRegistered_throwsConflictWithEmailCodeAndSavesNothing() {
        var request = new RegisterUserRequest("Alex@Test.com", "Alex", "plain-password");
        when(repository.existsByEmailIgnoreCase("alex@test.com")).thenReturn(true);

        var ex = assertThrows(ResourceAlreadyExistsException.class, () -> userService.createUser(request));

        assertEquals("EMAIL_ALREADY_EXISTS", ex.getErrorCode());
        verify(repository, never()).save(any());
        verifyNoInteractions(encoder);
    }

    // ==================== deleteUser ====================

    @Test
    void deleteUser_whenExists_deletesUser() {
        var existing = user();
        when(repository.findById(USER_ID)).thenReturn(Optional.of(existing));

        userService.deleteUser(USER_ID);

        verify(repository).delete(existing);
    }

    @Test
    void deleteUser_whenNotFound_throwsResourceNotFoundException() {
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(USER_ID));

        verify(repository, never()).delete(any());
    }

    // ==================== getUserById ====================

    @Test
    void getUserById_returnsNameEmailAndBalance() {
        var existing = user();
        existing.setBalance(new BigDecimal("250.75"));
        when(repository.findById(USER_ID)).thenReturn(Optional.of(existing));

        var dto = userService.getUserById(USER_ID);

        assertEquals("Alex", dto.name());
        assertEquals("alex@test.com", dto.email());
        assertEquals(new BigDecimal("250.75"), dto.balance());
    }

    @Test
    void getUserById_whenNotFound_throwsResourceNotFoundException() {
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(USER_ID));
    }

    // ==================== updateUserBalance ====================

    @Test
    void updateUserBalance_setsNewBalanceSavesAndReturnsIt() {
        var existing = user();
        when(repository.findById(USER_ID)).thenReturn(Optional.of(existing));

        var result = userService.updateUserBalance(USER_ID, new BigDecimal("1500.50"));

        assertEquals(new BigDecimal("1500.50"), result);
        assertEquals(new BigDecimal("1500.50"), existing.getBalance());
        verify(repository).save(existing);
    }

    @Test
    void updateUserBalance_whenNotFound_throwsAndDoesNotSave() {
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUserBalance(USER_ID, BigDecimal.TEN));

        verify(repository, never()).save(any());
    }
}
