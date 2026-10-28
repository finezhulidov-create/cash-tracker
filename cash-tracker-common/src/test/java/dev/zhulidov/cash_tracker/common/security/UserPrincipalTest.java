package dev.zhulidov.cash_tracker.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserPrincipalTest {

    @Test
    void threeArgConstructor_setsIdEmailAndPassword() {
        var principal = new UserPrincipal(1L, "user@test.com", "hash");

        assertEquals(1L, principal.getId());
        assertEquals("user@test.com", principal.getEmail());
        assertEquals("hash", principal.getPassword());
    }

    @Test
    void getUsername_returnsEmail() {
        var principal = new UserPrincipal(1L, "user@test.com", "hash");

        assertEquals("user@test.com", principal.getUsername());
    }

    @Test
    void getAuthorities_isEmpty() {
        assertTrue(new UserPrincipal(1L, "user@test.com", "hash").getAuthorities().isEmpty());
    }

    @Test
    void accountStatusFlags_areAllTrue() {
        var principal = new UserPrincipal(1L, "user@test.com", "hash");

        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isCredentialsNonExpired());
        assertTrue(principal.isEnabled());
    }

    @Test
    void builder_setsAllFields() {
        var principal = UserPrincipal.builder()
                .id(2L).email("a@b.c").userName("name").password("p").build();

        assertEquals(2L, principal.getId());
        assertEquals("a@b.c", principal.getEmail());
        assertEquals("p", principal.getPassword());
    }
}
