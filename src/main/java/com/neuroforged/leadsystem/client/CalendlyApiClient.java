package com.neuroforged.leadsystem.client;

import com.neuroforged.leadsystem.dto.CalendlyTokenResponse;
import com.neuroforged.leadsystem.dto.CalendlyScheduledEventsResponse;
import com.neuroforged.leadsystem.dto.CalendlyEventInviteesResponse;

import java.time.ZonedDateTime;
import java.util.Map;

public interface CalendlyApiClient {

    String exchangeAuthCodeForTokens(String state);

    CalendlyTokenResponse exchangeCodeForToken(String code);

    CalendlyTokenResponse refreshAccessToken(String refreshToken);

    CalendlyScheduledEventsResponse fetchScheduledEvents(String accessToken, String organizationUri, ZonedDateTime minStartTime, String status);

    CalendlyEventInviteesResponse fetchEventInvitees(String accessToken, String eventUuid);

    /** GET /users/me — the `resource` object (uri, name, scheduling_url, current_organization). */
    Map<String, Object> fetchCurrentUser(String accessToken);
}
