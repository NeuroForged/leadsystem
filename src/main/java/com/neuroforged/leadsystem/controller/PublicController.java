package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.ContactRequest;
import com.neuroforged.leadsystem.dto.NewsletterRequest;
import com.neuroforged.leadsystem.entity.NewsletterSubscriber;
import com.neuroforged.leadsystem.repository.NewsletterSubscriberRepository;
import com.neuroforged.leadsystem.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * LSB-152: Public endpoints for the marketing website — contact form + newsletter signup.
 * No auth required. Rate-limited via SecurityConfig permit list (no X-Api-Key needed).
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicController {

    private final EmailService emailService;
    private final NewsletterSubscriberRepository newsletterRepo;

    @Value("${neuroforged.admin.email}")
    private String adminEmail;

    /**
     * POST /api/contact — handles website contact form submissions.
     * Sends an email notification to the admin inbox.
     */
    @PostMapping("/contact")
    public ResponseEntity<Map<String, Boolean>> contact(@Valid @RequestBody ContactRequest req) {
        log.info("Contact form submission from name={} email={} company={} source={}",
                req.getName(), req.getEmail(), req.getCompany(), req.getSource());

        String subject = "[Alchemize Contact] " + req.getSubject();
        String body = buildContactEmailBody(req);

        try {
            emailService.sendLeadNotification(adminEmail, subject, body);
        } catch (Exception e) {
            // Log but don't fail the request — the submission was received
            log.error("Failed to send contact notification email: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of("ok", true));
    }

    /**
     * POST /api/newsletter — idempotent newsletter signup.
     * Duplicate emails return 200 without creating a new record.
     */
    @PostMapping("/newsletter")
    public ResponseEntity<Map<String, Boolean>> newsletter(@Valid @RequestBody NewsletterRequest req) {
        String email = req.getEmail().toLowerCase().strip();
        log.info("Newsletter signup email={} source={}", email, req.getSource());

        if (newsletterRepo.findByEmail(email).isEmpty()) {
            NewsletterSubscriber subscriber = NewsletterSubscriber.builder()
                    .email(email)
                    .source(req.getSource())
                    .subscribedAt(LocalDateTime.now())
                    .build();
            newsletterRepo.save(subscriber);
            log.info("New newsletter subscriber: {}", email);
        } else {
            log.debug("Newsletter subscriber already exists: {}", email);
        }

        return ResponseEntity.ok(Map.of("ok", true));
    }

    private String buildContactEmailBody(ContactRequest req) {
        return String.format(
                """
                New contact form submission from alchemizeiq.com

                Name:    %s
                Email:   %s
                Company: %s
                Source:  %s
                Subject: %s

                --- Message ---
                %s
                """,
                req.getName(),
                req.getEmail(),
                req.getCompany() != null ? req.getCompany() : "(not provided)",
                req.getSource() != null ? req.getSource() : "(direct)",
                req.getSubject(),
                req.getMessage()
        );
    }
}
