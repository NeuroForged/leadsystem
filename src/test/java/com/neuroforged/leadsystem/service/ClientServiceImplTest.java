package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.ClientDto;
import com.neuroforged.leadsystem.entity.Client;
import com.neuroforged.leadsystem.mapper.ClientMapper;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.service.impl.ClientServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock private ClientRepository clientRepository;
    @Mock private ClientMapper clientMapper;

    @InjectMocks
    private ClientServiceImpl clientService;

    @Test
    void createClient_savesAndReturnsDto() {
        ClientDto dto = new ClientDto();
        dto.setName("Acme");
        dto.setPrimaryEmail("acme@example.com");

        Client entity = new Client();
        entity.setId(1L);
        entity.setName("Acme");

        ClientDto resultDto = new ClientDto();
        resultDto.setId(1L);
        resultDto.setName("Acme");

        when(clientMapper.toEntity(dto)).thenReturn(entity);
        when(clientRepository.save(entity)).thenReturn(entity);
        when(clientMapper.toDto(entity)).thenReturn(resultDto);

        ClientDto result = clientService.createClient(dto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Acme");
        verify(clientRepository).save(entity);
    }

    @Test
    void getClientDtoById_existingId_returnsDto() {
        Client entity = new Client();
        entity.setId(1L);
        ClientDto dto = new ClientDto();
        dto.setId(1L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(clientMapper.toDto(entity)).thenReturn(dto);

        assertThat(clientService.getClientDtoById(1L).getId()).isEqualTo(1L);
    }

    @Test
    void getClientDtoById_nonExistentId_throwsRuntimeException() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientDtoById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findClientByEmail_match_returnsClient() {
        Client client = new Client();
        client.setPrimaryEmail("match@example.com");
        when(clientRepository.findByPrimaryEmail("match@example.com")).thenReturn(Optional.of(client));

        assertThat(clientService.findClientByEmail("match@example.com")).isPresent();
    }

    @Test
    void findClientByEmail_noMatch_returnsEmpty() {
        when(clientRepository.findByPrimaryEmail("none@example.com")).thenReturn(Optional.empty());

        assertThat(clientService.findClientByEmail("none@example.com")).isEmpty();
    }
}
