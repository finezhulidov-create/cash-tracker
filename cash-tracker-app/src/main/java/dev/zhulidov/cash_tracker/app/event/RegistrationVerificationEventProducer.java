package dev.zhulidov.cash_tracker.app.event;

import dev.zhulidov.cash_tracker.common.dto.RegistrationVerificationDto;
import dev.zhulidov.cash_tracker.common.dto.SendingOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegistrationVerificationEventProducer {

    private static final String TOPIC = "register-events";
    private final KafkaTemplate<String, RegistrationVerificationDto> kafkaTemplate;

    public void sendEvent(String email, SendingOperations operation ){
        kafkaTemplate.send(TOPIC, new RegistrationVerificationDto(email,operation));
    }


}
