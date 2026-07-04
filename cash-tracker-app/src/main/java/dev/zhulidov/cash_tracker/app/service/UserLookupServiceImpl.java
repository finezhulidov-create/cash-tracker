package dev.zhulidov.cash_tracker.app.service;

import dev.zhulidov.cash_tracker.app.repository.UserRepository;
import dev.zhulidov.cash_tracker.common.dto.UserSummary;
import dev.zhulidov.cash_tracker.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import dev.zhulidov.cash_tracker.common.service.UserLookUpService;

@Service
@RequiredArgsConstructor
public class UserLookupServiceImpl implements UserLookUpService {
    private final UserRepository userRepository;
    @Override
    public UserSummary getUserInfo(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
        return new UserSummary(user.getId(),user.getName());
    }
}
