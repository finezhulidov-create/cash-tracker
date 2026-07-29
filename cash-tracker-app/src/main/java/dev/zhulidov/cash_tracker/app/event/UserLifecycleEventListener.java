package dev.zhulidov.cash_tracker.app.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserLifecycleEventListener {
    private final RegistrationVerificationEventProducer producer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserLifecycle(UserLifecycleEvent event){
        producer.sendEvent(event.email(), event.operations());
    }

}
