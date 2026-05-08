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
        String subject = "New Lead Received - " + lead.getEmail() + " - " + lead.getLeadScore() + "/100";
        String body = String.format("""
            Email: %s
            Customer Type: %s
            Business Name: %s
            Business Type: %s
            Monthly Leads: %s
            Traffic Source: %s
            Conversion Rate: %s
            Cost Per Lead: %s
            Client Value: %s
            Lead Challenge: %s
            Client ID: %s
            Created At: %s
            """,
            lead.getEmail(), lead.getCustomerType(), lead.getBusinessName(),
            lead.getBusinessType(), lead.getMonthlyLeads(), lead.getTrafficSource(),
            lead.getConversionRate(), lead.getCostPerLead(), lead.getClientValue(),
            lead.getLeadChallenge(), lead.getClientIdStr(), lead.getCreatedAt());

        String[] recipients = resolveRecipients(lead.getClientIdStr());

        try {
            emailService.sendLeadToMultiple(recipients, subject, body);
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
}
