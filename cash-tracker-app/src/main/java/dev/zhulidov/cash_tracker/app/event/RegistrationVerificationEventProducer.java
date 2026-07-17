package dev.zhulidov.cash_tracker.app.event;

import dev.zhulidov.cash_tracker.app.dto.RegistrationVerificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

@Component
@RequiredArgsConstructor
public class RegistrationVerificationEventProducer {

    private static final String TOPIC = "register-events";
    private final KafkaTemplate<String, RegistrationVerificationDto> kafkaTemplate;

    public void sendEvent(String email, SendingOperations operation ){
        kafkaTemplate.send(TOPIC, new RegistrationVerificationDto(email,operation));
    }


}
