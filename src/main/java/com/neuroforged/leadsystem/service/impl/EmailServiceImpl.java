package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.exception.EmailSendException;
import com.neuroforged.leadsystem.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${neuroforged.admin.email}")
    private String adminEmail;

    @Value("${neuroforged.mail.from}")
    private String from;

    @Override
    public void sendLeadNotification(String to, String subject, String body) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false); // plain text email
            helper.setFrom(from);

            mailSender.send(message);

            log.info("✅ Email sent to {}\nSubject: {}\nBody:\n{}", to, subject, body);
        } catch (MessagingException e) {
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new EmailSendException("Failed to send email to " + to, e);
        }
    }

    @Override
    public void sendLeadToMultiple(String[] recipients, String subject, String body) {
        for (String recipient : recipients) {
            try {
                sendLeadNotification(recipient, subject, body);
            } catch (MessagingException e) {
                log.warn("⚠️ Skipped sending to {} due to error: {}", recipient, e.getMessage());
            }
        }
    }



    @Override
    public void notifyAdminOfWebhookFailure(String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(adminEmail);
            helper.setFrom(from);
            helper.setSubject("Calendly Webhook Notification");
            helper.setText(message, false);
            mailSender.send(mimeMessage);
        } catch (MessagingException ex) {
            log.error("Failed to send notification email to admin", ex);
        }
    }
}
