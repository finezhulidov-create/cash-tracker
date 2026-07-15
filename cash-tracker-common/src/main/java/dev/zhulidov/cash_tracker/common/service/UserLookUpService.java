package dev.zhulidov.cash_tracker.common.service;

import dev.zhulidov.cash_tracker.common.dto.UserSummary;

public interface UserLookUpService {
    UserSummary getUserInfo(Long userId);
}
