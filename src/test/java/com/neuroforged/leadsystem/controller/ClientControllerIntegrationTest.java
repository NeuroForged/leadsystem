package com.neuroforged.leadsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuroforged.leadsystem.dto.AuthenticationRequest;
import com.neuroforged.leadsystem.dto.ClientDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ClientControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

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
    }

    @Test
    void createClient_withAdminJwt_returns200() throws Exception {
        ClientDto dto = new ClientDto();
        dto.setName("Test Client");
        dto.setPrimaryEmail("client@test.com");

        mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Client"));
    }

    @Test
    void createClient_withoutJwt_returns403() throws Exception {
        ClientDto dto = new ClientDto();
        dto.setName("Unauthorized Client");
        dto.setPrimaryEmail("unauth@test.com");

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getClient_withAdminJwt_returns200() throws Exception {
        ClientDto dto = new ClientDto();
        dto.setName("Get Test Client");
        dto.setPrimaryEmail("get@test.com");

        MvcResult created = mockMvc.perform(post("/api/clients")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        Long id = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/clients/" + id)
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Get Test Client"));
    }

    @Test
    void getClient_nonExistentId_returns500() throws Exception {
        mockMvc.perform(get("/api/clients/99999")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().is5xxServerError());
    }
}
