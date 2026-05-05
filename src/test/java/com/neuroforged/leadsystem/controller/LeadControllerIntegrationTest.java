package com.neuroforged.leadsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuroforged.leadsystem.dto.LeadRequestDTO;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.service.EmailService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LeadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @MockBean
    private EmailService emailService;

    private String clientApiKey;
    private String clientId;

    @BeforeEach
    void setUp() {
        Client client = new Client();
        client.setName("Test Client");
        client.setPrimaryEmail("client@test.com");
        Client saved = clientRepository.save(client);
        clientApiKey = saved.getApiKey();
        clientId = String.valueOf(saved.getId());
    }

    private LeadRequestDTO validLead(String email) {
        LeadRequestDTO dto = new LeadRequestDTO();
        dto.setEmail(email);
        dto.setClientId(clientId);
        dto.setBusinessName("Acme Corp");
        dto.setBusinessType("SaaS");
        dto.setCustomerType("B2B");
        dto.setTrafficSource("Google");
        dto.setMonthlyLeads(100);
        dto.setConversionRate(2.5);
        dto.setCostPerLead(50.0);
        dto.setClientValue(5000.0);
        dto.setLeadScore(80);
        dto.setLeadChallenge("Growing pipeline");
        return dto;
    }

    @Test
    void postLead_validApiKey_returns200() throws Exception {
        mockMvc.perform(post("/api/leads")
                        .header("X-Api-Key", clientApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLead("new@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void postLead_invalidApiKey_returns403() throws Exception {
        mockMvc.perform(post("/api/leads")
                        .header("X-Api-Key", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLead("new2@example.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void postLead_missingApiKey_returns403() throws Exception {
        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLead("new3@example.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void postLead_duplicateEmail_returns409() throws Exception {
        String body = objectMapper.writeValueAsString(validLead("dup@example.com"));

        mockMvc.perform(post("/api/leads")
                        .header("X-Api-Key", clientApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/leads")
                        .header("X-Api-Key", clientApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void postLead_invalidEmail_returns400() throws Exception {
        LeadRequestDTO dto = validLead("not-an-email");
        mockMvc.perform(post("/api/leads")
                        .header("X-Api-Key", clientApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
