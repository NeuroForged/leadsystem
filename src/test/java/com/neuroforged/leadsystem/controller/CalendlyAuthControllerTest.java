package com.neuroforged.leadsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuroforged.leadsystem.dto.AuthenticationRequest;
import com.neuroforged.leadsystem.dto.CalendlyAuthRequest;
import com.neuroforged.leadsystem.dto.CalendlyAuthResponse;
import com.neuroforged.leadsystem.service.CalendlyAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CalendlyAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CalendlyAuthService calendlyAuthService;

    private String adminJwt;

    @BeforeEach
    void obtainJwt() throws Exception {
        if (adminJwt != null) return;
        AuthenticationRequest login = new AuthenticationRequest();
        login.setEmail("admin@test.com");
        login.setPassword("test-password");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        adminJwt = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();

        when(calendlyAuthService.generateAuthorizationUrl(any()))
                .thenReturn(new CalendlyAuthResponse("https://calendly.com/oauth/authorize?...", "test-state"));
    }

    @Test
    void authorize_withAdminJwt_returns200WithUrl() throws Exception {
        CalendlyAuthRequest request = new CalendlyAuthRequest();
        request.setClientId(1L);

        mockMvc.perform(post("/api/calendly/authorize")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").isNotEmpty())
                .andExpect(jsonPath("$.state").isNotEmpty());
    }

    @Test
    void authorize_withoutJwt_returns403() throws Exception {
        CalendlyAuthRequest request = new CalendlyAuthRequest();
        request.setClientId(1L);

        mockMvc.perform(post("/api/calendly/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
