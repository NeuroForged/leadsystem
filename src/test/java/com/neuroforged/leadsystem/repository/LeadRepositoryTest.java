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

    private Lead buildLead(String email, Client client) {
        return Lead.builder()
                .email(email)
                .client(client)
                .status(LeadStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void save_uniqueConstraint_duplicateEmailSameClient_throws() {
        // LSB-153: unique constraint is now on (email, client_id) FK.
        Client client = buildClient("dup-client");
        leadRepository.saveAndFlush(buildLead("dup@example.com", client));

        assertThatThrownBy(() -> leadRepository.saveAndFlush(buildLead("dup@example.com", client)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_sameEmail_differentClient_succeeds() {
        Client clientA = buildClient("client-A");
        Client clientB = buildClient("client-B");
        Lead a = leadRepository.saveAndFlush(buildLead("shared@example.com", clientA));
        Lead b = leadRepository.saveAndFlush(buildLead("shared@example.com", clientB));

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
