package com.neuroforged.leadsystem.repository;

import com.neuroforged.leadsystem.entity.CalendlyMeeting;
import com.neuroforged.leadsystem.entity.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=false")
class CalendlyMeetingRepositoryTest {

    private static final ZonedDateTime ALL_FROM = ZonedDateTime.now().minusYears(1);
    private static final ZonedDateTime ALL_TO = ZonedDateTime.now().plusYears(1);

    @Autowired
    private CalendlyMeetingRepository meetingRepository;

    @Autowired
    private ClientRepository clientRepository;

    private void meeting(Client client, String email) {
        meetingRepository.saveAndFlush(CalendlyMeeting.builder()
                .calendlyUri("uri-" + email)
                .inviteeEmail(email)
                .startTime(ZonedDateTime.now().plusDays(1))
                .client(client)
                .build());
    }

    @Test
    void search_withNoFilters_returnsEveryMeeting() {
        // The Meetings page calls this with every filter empty. With null filters the query
        // failed on Postgres (untyped null parameters), so the page showed "No meetings yet" forever.
        Client client = new Client();
        client.setName("meetings-client");
        client.setApiKey("key-meetings-" + System.nanoTime());
        client = clientRepository.saveAndFlush(client);
        meeting(client, "Ann@Example.com");
        meeting(client, "bob@example.com");

        assertThat(meetingRepository.search(null, ALL_FROM, ALL_TO, "%", PageRequest.of(0, 20)).getTotalElements())
                .isEqualTo(2);
        assertThat(meetingRepository.search(client.getId(), ALL_FROM, ALL_TO, "%ann@%", PageRequest.of(0, 20)).getContent())
                .extracting(CalendlyMeeting::getInviteeEmail).containsExactly("Ann@Example.com");
    }
}
