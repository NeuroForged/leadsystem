package com.neuroforged.leadsystem.service;

import java.util.Map;

/**
 * Connects a client's Calendly with a personal access token — for clients (and demo
 * environments) without a registered Calendly OAuth app. Bookings then arrive by polling.
 */
public interface CalendlyTokenConnectService {
    /** Validates the token with Calendly and stores it. Returns name + scheduling URL of the account. */
    Map<String, String> connect(Long clientId, String token);
}
