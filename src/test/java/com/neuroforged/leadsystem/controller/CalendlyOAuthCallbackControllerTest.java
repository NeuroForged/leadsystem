package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.service.CalendlyAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CalendlyOAuthCallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalendlyAuthService calendlyAuthService;

    @Test
    void callback_validCodeAndState_returns200() throws Exception {
        doNothing().when(calendlyAuthService).handleOAuthCallback(any());

        mockMvc.perform(get("/api/calendly/oauth/callback")
                        .param("code", "test-code")
                        .param("state", "test-state"))
                .andExpect(status().isOk());
    }

    @Test
    void callback_missingCode_returns400() throws Exception {
        mockMvc.perform(get("/api/calendly/oauth/callback")
                        .param("state", "test-state"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void callback_missingState_returns400() throws Exception {
        mockMvc.perform(get("/api/calendly/oauth/callback")
                        .param("code", "test-code"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void callback_serviceThrowsException_returns500() throws Exception {
        doThrow(new RuntimeException("OAuth failed")).when(calendlyAuthService).handleOAuthCallback(any());

        mockMvc.perform(get("/api/calendly/oauth/callback")
                        .param("code", "test-code")
                        .param("state", "bad-state"))
                .andExpect(status().isInternalServerError());
    }
}
