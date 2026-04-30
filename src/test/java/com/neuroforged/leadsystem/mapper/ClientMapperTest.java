package com.neuroforged.leadsystem.mapper;

import com.neuroforged.leadsystem.dto.ClientDto;
import com.neuroforged.leadsystem.entity.Client;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientMapperTest {

    private final ClientMapper mapper = new ClientMapper();

    @Test
    void toDto_mapsAllFields() {
        Client client = new Client();
        client.setId(1L);
        client.setName("Acme");
        client.setPrimaryEmail("contact@acme.com");
        client.setNotificationEmails("a@acme.com,b@acme.com");
        client.setWebsiteUrl("https://acme.com");

        ClientDto dto = mapper.toDto(client);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Acme");
        assertThat(dto.getPrimaryEmail()).isEqualTo("contact@acme.com");
        assertThat(dto.getNotificationEmails()).isEqualTo("a@acme.com,b@acme.com");
        assertThat(dto.getWebsiteUrl()).isEqualTo("https://acme.com");
    }

    @Test
    void toEntity_mapsAllFields() {
        ClientDto dto = new ClientDto();
        dto.setId(2L);
        dto.setName("Beta Corp");
        dto.setPrimaryEmail("hello@beta.com");
        dto.setNotificationEmails("notify@beta.com");
        dto.setWebsiteUrl("https://beta.com");

        Client entity = mapper.toEntity(dto);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getName()).isEqualTo("Beta Corp");
        assertThat(entity.getPrimaryEmail()).isEqualTo("hello@beta.com");
        assertThat(entity.getNotificationEmails()).isEqualTo("notify@beta.com");
        assertThat(entity.getWebsiteUrl()).isEqualTo("https://beta.com");
    }

    @Test
    void toDto_nullInput_returnsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void toEntity_nullInput_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
