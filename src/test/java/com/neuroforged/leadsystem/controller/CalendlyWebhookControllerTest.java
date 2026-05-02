package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.service.CalendlyWebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CalendlyWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalendlyWebhookService calendlyWebhookService;

    private static final String SIGNING_KEY = "test-signing-key";
    private static final String VALID_BODY = """
            {"event":"invitee.created","payload":{"event_type":{"uuid":"abc","name":"30min","slug":"30min"},"invitee":{"email":"user@example.com","name":"Test User"}}}
            """.strip();

    private String buildSignature(String body) throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String dataToSign = timestamp + "." + body;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SIGNING_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String hmac = HexFormat.of().formatHex(mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + hmac;
    }

    @Test
    void webhook_missingSignatureHeader_returns401() throws Exception {
        mockMvc.perform(post("/api/calendly/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_invalidHmac_returns401() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String badSig = "t=" + timestamp + ",v1=deadbeefdeadbeef";

        mockMvc.perform(post("/api/calendly/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Calendly-Webhook-Signature", badSig)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_staleTimestamp_returns401() throws Exception {
        long staleTimestamp = Instant.now().getEpochSecond() - 400;
        String dataToSign = staleTimestamp + "." + VALID_BODY;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SIGNING_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String hmac = HexFormat.of().formatHex(mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8)));
        String sig = "t=" + staleTimestamp + ",v1=" + hmac;

        mockMvc.perform(post("/api/calendly/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Calendly-Webhook-Signature", sig)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_malformedSignatureHeader_returns401() throws Exception {
        mockMvc.perform(post("/api/calendly/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Calendly-Webhook-Signature", "not-a-valid-signature")
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_validSignature_returns200() throws Exception {
        String sig = buildSignature(VALID_BODY);

        mockMvc.perform(post("/api/calendly/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Calendly-Webhook-Signature", sig)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void webhook_validSignatureButMalformedJson_returns400() throws Exception {
        String badBody = "{not-valid-json}";
        String sig = buildSignature(badBody);

        mockMvc.perform(post("/api/calendly/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Calendly-Webhook-Signature", sig)
                        .content(badBody))
                .andExpect(status().isBadRequest());
    }
}
