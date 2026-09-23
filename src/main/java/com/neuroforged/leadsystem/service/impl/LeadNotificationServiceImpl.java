package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.exception.EmailSendException;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.service.EmailService;
import com.neuroforged.leadsystem.service.LeadNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadNotificationServiceImpl implements LeadNotificationService {

    private final ClientRepository clientRepository;
    private final EmailService emailService;

    @Value("${neuroforged.admin.email}")
    private String fallbackEmail;

    @Override
    public void notifyNewLead(Lead lead) {
        String score = lead.getLeadScore() == null ? "unscored" : lead.getLeadScore() + "/100";
        String subject = "New Lead Received - " + lead.getEmail() + " - " + score;
        StringBuilder body = new StringBuilder();
        line(body, "Email", lead.getEmail());
        line(body, "Name", lead.getFirstName());
        line(body, "Customer Type", lead.getCustomerType());
        line(body, "Business Name", lead.getBusinessName());
        line(body, "Business Type", lead.getBusinessType());
        line(body, "Traffic Source", lead.getTrafficSource());
        line(body, "Monthly Leads", lead.getMonthlyLeads());
        line(body, "Conversion Rate", lead.getConversionRate());
        line(body, "Cost Per Lead", lead.getCostPerLead());
        line(body, "Client Value", lead.getClientValue());
        line(body, "Lead Challenge", lead.getLeadChallenge());
        if (lead.getCapturedFields() != null && !lead.getCapturedFields().isEmpty()) {
            body.append("\nCaptured by the bot:\n");
            lead.getCapturedFields().forEach((k, v) -> body.append("  ").append(k.replace('_', ' ')).append(": ").append(v).append('\n'));
        }
        body.append('\n');
        line(body, "Client ID", lead.getClientIdStr());
        line(body, "Created At", lead.getCreatedAt());

        String[] recipients = resolveRecipients(lead.getClientIdStr());

        try {
            emailService.sendLeadToMultiple(recipients, subject, body.toString());
        } catch (EmailSendException e) {
            log.warn("Failed to send notification email: {}", e.getMessage(), e);
        }
    }

    private String[] resolveRecipients(String clientId) {
        try {
            Long id = Long.parseLong(clientId);
            return clientRepository.findById(id)
                    .map(Client::getNotificationEmails)
                    .filter(emails -> emails != null && !emails.isBlank())
                    .map(emails -> Arrays.stream(emails.split(","))
                            .map(String::trim)
                            .filter(e -> !e.isBlank())
                            .toArray(String[]::new))
                    .orElse(new String[]{fallbackEmail});
        } catch (NumberFormatException e) {
            log.warn("Could not parse clientId '{}' as Long, falling back to admin email", clientId);
            return new String[]{fallbackEmail};
        }
    }

    /** Skips absent values so a patient lead does not arrive full of "Cost Per Lead: null". */
    private static void line(StringBuilder sb, String label, Object value) {
        if (value == null || value.toString().isBlank()) return;
        sb.append(label).append(": ").append(value).append('\n');
    }
}
