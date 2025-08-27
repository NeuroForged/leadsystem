package com.neuroforged.leadsystem.client;
import java.time.Instant;
import java.util.List;
import com.neuroforged.leadsystem.entity.CalendlyAccount;
import com.neuroforged.leadsystem.dto.ScheduledEventDTO;
import com.neuroforged.leadsystem.dto.InviteeDTO;


import com.neuroforged.leadsystem.dto.CalendlyTokenResponse;

public interface CalendlyApiClient {

    /**
     * Builds an authorization URL for Calendly with the given OAuth state.
     */
    String exchangeAuthCodeForTokens(String state);

    /**
     * Exchanges the authorization code for an access + refresh token.
     */
  
      CalendlyTokenResponse exchangeCodeForToken(String code);

    List<ScheduledEventDTO> listScheduledEvents(CalendlyAccount account, Instant minStartTime, Instant maxStartTime);

    List<InviteeDTO> listInvitees(CalendlyAccount account, String scheduledEventUuid);

