package com.neuroforged.leadsystem.service;

import jakarta.mail.MessagingException;

public interface EmailService {
    void sendLeadNotification(String to, String subject, String body) throws MessagingException;
    void sendLeadToMultiple(String[] recipients, String subject, String body);
    void notifyAdminOfWebhookFailure(String message);
    }
