package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.util.SafeUrl;
import java.time.Duration;
import com.neuroforged.leadsystem.entity.NotificationChannel;
import com.neuroforged.leadsystem.entity.NotificationEventType;
import com.neuroforged.leadsystem.repository.NotificationChannelRepository;
import com.neuroforged.leadsystem.service.EmailService;
import com.neuroforged.leadsystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationChannelRepository channelRepository;
    private final EmailService emailService;
    private final WebClient.Builder webClientBuilder;

    @Async("backgroundTaskExecutor")
    @Override
    public void notify(Long clientId, NotificationEventType event, Map<String, String> context) {
        List<NotificationChannel> channels = channelRepository.findByClientIdAndEvent(clientId, event);
        if (channels.isEmpty()) return;

        String message = buildMessage(event, context);

        for (NotificationChannel channel : channels) {
            try {
                switch (channel.getChannelType()) {
                    case SLACK -> sendSlack(channel.getDestination(), message);
                    case EMAIL -> sendEmail(channel.getDestination(), event, message);
                }
            } catch (Exception e) {
                log.warn("Notification dispatch failed for channel id={} event={}: {}", channel.getId(), event, e.getMessage());
            }
        }
    }

    private void sendSlack(String webhookUrl, String message) {
        // Same guard as outbound webhooks: no internal destinations, and never block a
        // bounded executor thread forever on a host that accepts TCP and goes quiet.
        if (!SafeUrl.isPublicHttps(webhookUrl)) {
            log.warn("Refusing Slack webhook to non-public URL");
            return;
        }
        String body = "{\"text\": " + escapeJson(message) + "}";
        webClientBuilder.build()
                .post()
                .uri(webhookUrl)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .block();
        log.debug("Slack notification sent to {}", webhookUrl);
    }

    private void sendEmail(String address, NotificationEventType event, String message) {
        String subject = switch (event) {
            case NEW_LEAD -> "New Lead Received";
            case MEETING_BOOKED -> "New Meeting Booked";
            case LEAD_SCORED_HIGH -> "High-Score Lead Alert";
        };
        try {
            emailService.sendLeadNotification(address, subject, message);
            log.debug("Email notification sent to {}", address);
        } catch (Exception e) {
            log.warn("Email notification failed to {}: {}", address, e.getMessage());
        }
    }

    private String buildMessage(NotificationEventType event, Map<String, String> context) {
        String header = switch (event) {
            case NEW_LEAD -> "New lead captured";
            case MEETING_BOOKED -> "Meeting booked";
            case LEAD_SCORED_HIGH -> "High-score lead alert";
        };
        String details = context.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
        return header + "\n" + details;
    }

    private String escapeJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
