package dev.zhulidov.cash_tracker.service;

import dev.zhulidov.cash_tracker.dto.RegisterUserRequest;
import dev.zhulidov.cash_tracker.dto.RegisterUserResponse;
import dev.zhulidov.cash_tracker.model.User;
import dev.zhulidov.cash_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public RegisterUserResponse createUser(RegisterUserRequest request){
        User user =  User.builder().email(request.email())
                .userName(request.username())
                .password(encoder.encode(request.password()))
                .build();
        var savedUser =  repository.save(user);
        return new RegisterUserResponse(savedUser.getUserName(), savedUser.getEmail());
    }

    public void deleteUser(Long id){
        repository.deleteById(id);
    }

    public User getUserById(Long id){
        return repository.findById(id).orElseThrow(()-> new UsernameNotFoundException("User not found"));
    }
}
