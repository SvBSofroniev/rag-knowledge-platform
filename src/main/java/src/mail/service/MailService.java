package src.mail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import src.common.exception.EmailDeliveryException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public void sendTextEmail(
            String recipient,
            String subject,
            String body
    ) {
        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(
                fromAddress
        );

        message.setTo(
                recipient
        );

        message.setSubject(
                subject
        );

        message.setText(
                body
        );

        try {
            mailSender.send(
                    message
            );

            log.info(
                    "Email sent successfully to {}",
                    recipient
            );

        } catch (MailException exception) {

            log.error(
                    "Could not send email to {}",
                    recipient,
                    exception
            );

            throw new EmailDeliveryException(
                    "Email could not be delivered",
                    exception
            );
        }
    }
}