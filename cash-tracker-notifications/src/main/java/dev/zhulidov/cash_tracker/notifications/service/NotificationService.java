package dev.zhulidov.cash_tracker.notifications.service;

import dev.zhulidov.cash_tracker.app.event.SendingOperations;
import jakarta.validation.constraints.NotNull;

public interface NotificationService {
    void send(String to, String subject, String text);
    void sendByOperation(String email, @NotNull SendingOperations operation);
}
