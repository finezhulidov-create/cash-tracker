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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RegisterUserResponse createUser(RegisterUserRequest request){
        User user =  User.builder().email(request.email())
                .name(request.username())
                .password(encoder.encode(request.password()))
                .build();
        var savedUser =  repository.save(user);
        return new RegisterUserResponse(savedUser.getName(), savedUser.getEmail());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteUser(Long id){
        var user = repository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));

        String email = user.getEmail();
        repository.delete(user);

    }

    public UserDto getUserById(Long id){
        var user = repository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        return new UserDto(user.getName(),user.getEmail(),user.getBalance());
    }

    public BigDecimal updateUserBalance(Long id, BigDecimal amount){
        User user = repository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        user.setBalance(amount);
        repository.save(user);
        return user.getBalance();
    }






}
