package dev.zhulidov.cash_tracker.app.service;

import dev.zhulidov.cash_tracker.app.dto.RegisterUserRequest;
import dev.zhulidov.cash_tracker.app.dto.RegisterUserResponse;
import dev.zhulidov.cash_tracker.app.dto.UserDto;
import dev.zhulidov.cash_tracker.app.event.RegistrationVerificationEventProducer;
import dev.zhulidov.cash_tracker.app.event.SendingOperations;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import dev.zhulidov.cash_tracker.app.model.User;
import dev.zhulidov.cash_tracker.app.repository.UserRepository;
import dev.zhulidov.cash_tracker.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final RegistrationVerificationEventProducer producer;

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public RegisterUserResponse createUser(RegisterUserRequest request){
        User user =  User.builder().email(request.email())
                .name(request.username())
                .password(encoder.encode(request.password()))
                .build();
        var savedUser =  repository.save(user);
        producer.sendEvent(savedUser.getEmail(), SendingOperations.REGISTERED);
//        producer.sendEvent(savedUser.getEmail(),SendingOperations.VERIFY_MAIL);
        return new RegisterUserResponse(savedUser.getName(), savedUser.getEmail());
    }

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deleteUser(Long id){
        var user = repository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
        SecurityUtils.assertOwner(user.getId(), id);
        String email = user.getEmail();
        repository.delete(user);
        producer.sendEvent(email, SendingOperations.DELETED);

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
