package dev.zhulidov.cash_tracker.notifications.service;

import dev.zhulidov.cash_tracker.common.dto.SendingOperations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
     JavaMailSender mailSender;

    @InjectMocks
    EmailService service;

    @Test
    void sendCreateEmail_shouldCallMailSender(){
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        service.sendByOperation("test@YAndex.ru", SendingOperations.REGISTERED);
        verify(mailSender,times(1)).send(any(SimpleMailMessage.class));
    }
}
