package com.neuroforged.leadsystem.client;

import com.neuroforged.leadsystem.dto.CalendlyTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalendlyApiClientImpl implements CalendlyApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${calendly.client-id}")
    private String clientId;

    @Value("${calendly.client-secret}")
    private String clientSecret;

    @Value("${calendly.redirect-uri}")
    private String redirectUri;

    private static final String TOKEN_URL = "https://auth.calendly.com/oauth/token";
    private static final String AUTH_URL = "https://auth.calendly.com/oauth/authorize";

    @Override
    public String exchangeAuthCodeForTokens(String state) {
        log.info("CalendlyApiClient.exchangeAuthCodeForTokens");
        // Generate authorization URL to redirect user to Calendly
        log.info("Auth Url: AUTH_URL + \"?response_type=code\"\n" +
                "                + \"&client_id=\" + clientId\n" +
                "                + \"&redirect_uri=\" + redirectUri\n" +
                "                + \"&state=\" + state;");
        return AUTH_URL + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&state=" + state;
    }

    @Override
    public CalendlyTokenResponse exchangeCodeForToken(String code) {
        log.info("CalendlyApiClient.exchangeCodeForToken");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("redirect_uri", redirectUri);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        log.info("Body before sending request: {}", request.getBody());
        ResponseEntity<CalendlyTokenResponse> response = restTemplate.exchange(
                TOKEN_URL,
                HttpMethod.POST,
                request,
                CalendlyTokenResponse.class
        );
        log.info("Response body from Calendly: {}", response.getBody());
        log.info("Response headers from Calendly: {}", response.getHeaders());
        return response.getBody();
 
        @Override
    public java.util.List<com.neuroforged.leadsystem.dto.ScheduledEventDTO> listScheduledEvents(
            com.neuroforged.leadsystem.entity.CalendlyAccount account,
            java.time.Instant minStartTime,
            java.time.Instant maxStartTime
    ) {
        // TODO: implement Calendly scheduled events API call for polling
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<com.neuroforged.leadsystem.dto.InviteeDTO> listInvitees(
            com.neuroforged.leadsystem.entity.CalendlyAccount account,
            String scheduledEventUuid
    ) {
        // TODO: implement Calendly invitees API call for polling
        return java.util.Collections.emptyList();
    }
}
}
