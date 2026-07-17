package dev.zhulidov.cash_tracker.notifications.service;

import dev.zhulidov.cash_tracker.app.event.SendingOperations;
import dev.zhulidov.cash_tracker.notifications.util.VerificationCodeRandomiser;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService implements NotificationService{
    private final JavaMailSender sender;

    @Override
    public void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(subject);
        message.setTo(to);
        message.setText(text);
        sender.send(message);
    }

    @Override
    public void sendByOperation(String email, @NotNull SendingOperations operation) {
        if (operation == SendingOperations.REGISTERED) {
            send(email, "You`re welcome!", "Your account successfully registered");
        } else if (operation == SendingOperations.DELETED) {
            send(email, "Account deleted", "Your account successfully deleted");
        } else if (operation == SendingOperations.VERIFY_MAIL){
            String code = VerificationCodeRandomiser.randomeCode().toString();
            send(email,"Verification code", "Your verification code: " + code);
        }
    }
}
