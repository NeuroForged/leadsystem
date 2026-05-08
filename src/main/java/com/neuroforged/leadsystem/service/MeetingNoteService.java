package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.FirefliesWebhookPayload;

public interface MeetingNoteService {
    /**
     * Match the Fireflies payload to a CalendlyMeeting, persist a MeetingNote,
     * and notify the client's configured recipients.
     */
    void processWebhook(Long clientId, FirefliesWebhookPayload payload);
}
