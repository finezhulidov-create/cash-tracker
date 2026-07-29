package dev.zhulidov.cash_tracker.app.event;

import dev.zhulidov.cash_tracker.common.dto.RegistrationVerificationDto;
import dev.zhulidov.cash_tracker.common.dto.SendingOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationVerificationEventProducer {

    private static final String TOPIC = "register-events";
    private final KafkaTemplate<String, RegistrationVerificationDto> kafkaTemplate;

    public void sendEvent(String email, SendingOperations operation ){
        kafkaTemplate.send(TOPIC, new RegistrationVerificationDto(email,operation))
                .whenComplete((result, ex)->{
                    if (ex != null){
                        log.error("Не удалось опубликовать событие {} для {}", operation, email, ex);
                    }
                });
    }


}
