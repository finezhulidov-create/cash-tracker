package dev.zhulidov.cash_tracker.model;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@RequiredArgsConstructor

public class UserPrincipal implements UserDetails {
    private Long id;
    private String email;
    private String userName;
    private String password;
    public static UserPrincipal from(User user){
        return new UserPrincipal(user.getId(),user.getEmail(),user.getName(),user.getPassword());
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
    /**
     * Возвращает email пользователя, используемый в качестве имени пользователя для аутентификации.
     *
     * @return email пользователя (уникальный идентификатор при входе)
     */
    @Override
    public String getUsername() {
        return email;
    }
}
