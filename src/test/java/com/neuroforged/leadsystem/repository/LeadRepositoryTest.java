package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.entity.Lead;
import com.neuroforged.leadsystem.entity.LeadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=false")
class LeadRepositoryTest {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private ClientRepository clientRepository;

    private Client buildClient(String name) {
        Client c = new Client();
        c.setName(name);
        c.setApiKey("key-" + name + "-" + System.nanoTime());
        return clientRepository.saveAndFlush(c);
    }

    private Lead buildLead(String email, String clientIdStr) {
        return Lead.builder()
                .email(email)
                .clientIdStr(clientIdStr)
                .status(LeadStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Lead buildLead(String email, Client client) {
        return Lead.builder()
                .email(email)
                .clientIdStr(String.valueOf(client.getId()))
                .client(client)
                .status(LeadStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void save_uniqueConstraint_duplicateEmailSameClient_throws() {
        leadRepository.saveAndFlush(buildLead("dup@example.com", "42"));

        assertThatThrownBy(() -> leadRepository.saveAndFlush(buildLead("dup@example.com", "42")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_sameEmail_differentClient_succeeds() {
        Lead a = leadRepository.saveAndFlush(buildLead("shared@example.com", "1"));
        Lead b = leadRepository.saveAndFlush(buildLead("shared@example.com", "2"));

        assertThat(a.getId()).isNotNull();
        assertThat(b.getId()).isNotNull();
        assertThat(a.getId()).isNotEqualTo(b.getId());
    }

    @Test
    void existsByEmailAndClient_Id_returnsTrue_whenExists() {
        Client client = buildClient("Acme");
        leadRepository.saveAndFlush(buildLead("exists@example.com", client));

        assertThat(leadRepository.existsByEmailAndClient_Id("exists@example.com", client.getId())).isTrue();
    }

    @Test
    void existsByEmailAndClient_Id_returnsFalse_whenNotExists() {
        Client client = buildClient("Other");
        assertThat(leadRepository.existsByEmailAndClient_Id("nobody@example.com", client.getId())).isFalse();
    }
}
