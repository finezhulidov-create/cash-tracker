package dev.zhulidov.cash_tracker.app.service;

import dev.zhulidov.cash_tracker.app.dto.RegisterUserRequest;
import dev.zhulidov.cash_tracker.app.dto.RegisterUserResponse;
import dev.zhulidov.cash_tracker.app.dto.UserDto;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.app.model.User;
import dev.zhulidov.cash_tracker.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public RegisterUserResponse createUser(RegisterUserRequest request){
        User user =  User.builder().email(request.email())
                .name(request.username())
                .password(encoder.encode(request.password()))
                .build();
        var savedUser =  repository.save(user);
        return new RegisterUserResponse(savedUser.getName(), savedUser.getEmail());
    }

    public void deleteUser(Long id){
        repository.deleteById(id);
    }

    public UserDto getUserById(Long id){
        var user = repository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        return new UserDto(user.getName(),user.getEmail());
    }




}
