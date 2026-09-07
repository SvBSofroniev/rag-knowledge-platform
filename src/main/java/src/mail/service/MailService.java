package src.mail.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import src.common.exception.EmailDeliveryException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    /*
     * ---------------------------------------------------------
     * SIMPLE TEXT EMAIL
     * ---------------------------------------------------------
     *
     * Used for emails that do not require attachments,
     * such as password-reset messages.
     */
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
                    "Text email sent successfully to {}",
                    recipient
            );

        } catch (MailException exception) {

            log.error(
                    "Could not send text email to {}",
                    recipient,
                    exception
            );

            throw new EmailDeliveryException(
                    "Email could not be delivered",
                    exception
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * EMAIL WITH PDF ATTACHMENT
     * ---------------------------------------------------------
     */
    public void sendEmailWithPdfAttachment(
            String recipient,
            String subject,
            String body,
            String attachmentFilename,
            byte[] pdfContent
    ) {
        MimeMessage message =
                mailSender.createMimeMessage();

        try {
            /*
             * multipart = true because the message
             * contains an attachment.
             */
            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            helper.setFrom(
                    fromAddress
            );

            helper.setTo(
                    recipient
            );

            helper.setSubject(
                    subject
            );

            /*
             * false -> body is plain text.
             *
             * The actual rich content is contained
             * in the attached OurVault PDF report.
             */
            helper.setText(
                    body,
                    false
            );

            ByteArrayResource pdfResource =
                    new ByteArrayResource(
                            pdfContent
                    );

            helper.addAttachment(
                    attachmentFilename,
                    pdfResource,
                    "application/pdf"
            );

            mailSender.send(
                    message
            );

            log.info(
                    "Email with PDF attachment '{}' sent successfully to {}",
                    attachmentFilename,
                    recipient
            );

        } catch (
                MessagingException |
                MailException exception
        ) {
            log.error(
                    "Could not send PDF email to {}",
                    recipient,
                    exception
            );

            throw new EmailDeliveryException(
                    "Email with PDF attachment could not be delivered",
                    exception
            );
        }
    }
}