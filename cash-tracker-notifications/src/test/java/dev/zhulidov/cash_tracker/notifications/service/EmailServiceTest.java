package dev.zhulidov.cash_tracker.notifications.service;

import dev.zhulidov.cash_tracker.common.dto.SendingOperations;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.*;

@SpringBootTest
public class EmailServiceTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    NotificationService service;

    @Test
    void sendCreateEmail_shouldCallMailSender(){
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        service.sendByOperation("test@YAndex.ru", SendingOperations.REGISTERED);
        verify(mailSender,times(1)).send(any(SimpleMailMessage.class));
    }
}
