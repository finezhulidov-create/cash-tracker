package dev.zhulidov.cash_tracker.app.service;

import dev.zhulidov.cash_tracker.app.dto.RegisterUserRequest;
import dev.zhulidov.cash_tracker.app.dto.RegisterUserResponse;
import dev.zhulidov.cash_tracker.app.dto.UserDto;
import dev.zhulidov.cash_tracker.app.event.UserLifecycleEvent;
import dev.zhulidov.cash_tracker.common.dto.SendingOperations;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.app.model.User;
import dev.zhulidov.cash_tracker.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher publisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RegisterUserResponse createUser(RegisterUserRequest request){
        User user =  User.builder().email(request.email())
                .name(request.username())
                .password(encoder.encode(request.password()))
                .build();
        var savedUser =  repository.save(user);
        publisher.publishEvent(new UserLifecycleEvent(savedUser.getEmail(),SendingOperations.REGISTERED));
        return new RegisterUserResponse(savedUser.getName(), savedUser.getEmail());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteUser(Long id){
        var user = repository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));

        String email = user.getEmail();
        repository.delete(user);
        publisher.publishEvent(new UserLifecycleEvent(email,SendingOperations.DELETED));

    }

    public UserDto getUserById(Long id){
        var user = repository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        return new UserDto(user.getName(),user.getEmail());
    }


//    public void confirmEmail(Long id, String code){
//        var user = repository.findById(id)
//                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
//        SecurityUtils.assertOwner(user.getId(), id);
//
//    }

}
