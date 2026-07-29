package dev.zhulidov.cash_tracker.app.event;

import dev.zhulidov.cash_tracker.common.dto.SendingOperations;

public record UserLifecycleEvent(String email, SendingOperations operations) {
}
