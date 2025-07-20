package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.exception.EmailSendException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${neuroforged.mail.from}")
    private String from;

    private final JavaMailSender mailSender;

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

    // Optional: bulk sending for multiple team members
    public void sendLeadToMultiple(String[] recipients, String subject, String body) {
        for (String recipient : recipients) {
            try {
                sendLeadNotification(recipient, subject, body);
            } catch (MessagingException e) {
                log.warn("⚠️ Skipped sending to {} due to error: {}", recipient, e.getMessage());
            }
        }
    }
}
