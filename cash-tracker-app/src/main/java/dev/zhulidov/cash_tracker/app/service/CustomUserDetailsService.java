package dev.zhulidov.cash_tracker.app.service;

import dev.zhulidov.cash_tracker.app.model.User;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository repository;

    /**
     * Бросает именно UsernameNotFoundException: Spring Security превращает его при логине в
     * BadCredentialsException (не раскрывая, что такого email нет), а JwtFilter — в «не аутентифицирован».
     */
    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPassword());
    }
}
