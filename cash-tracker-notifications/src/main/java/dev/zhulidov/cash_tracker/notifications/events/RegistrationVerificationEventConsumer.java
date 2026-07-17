package dev.zhulidov.cash_tracker.notifications.events;

import dev.zhulidov.cash_tracker.notifications.dto.RegistrationVerificationDto;
import dev.zhulidov.cash_tracker.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegistrationVerificationEventConsumer {

        private final NotificationService notificationService;
        @KafkaListener(topics = "register-events", groupId = "notification-group")
        public void consume(RegistrationVerificationDto dto){
            notificationService.sendByOperation(dto.email(), dto.operations());
        }
}
