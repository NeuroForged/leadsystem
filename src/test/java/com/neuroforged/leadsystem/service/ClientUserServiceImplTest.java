package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.ClientUserDto;
import com.neuroforged.leadsystem.dto.CreateClientUserRequest;
import com.neuroforged.leadsystem.entity.User;
import com.neuroforged.leadsystem.exception.DuplicateResourceException;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.UserRepository;
import com.neuroforged.leadsystem.service.impl.ClientUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientUserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;

    private PasswordEncoder passwordEncoder;
    private ClientUserServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new ClientUserServiceImpl(userRepository, clientRepository, passwordEncoder);
    }

    private CreateClientUserRequest request(String email, String password) {
        CreateClientUserRequest r = new CreateClientUserRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    // ----- createClientUser -----

    @Test
    void createClientUser_happyPath_savesUserWithRoleAndHashedPassword() {
        Long clientId = 5L;
        CreateClientUserRequest req = request("portal@acme.com", "supersecret");

        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(userRepository.existsByEmail("portal@acme.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(101L);
            return u;
        });

        ClientUserDto dto = service.createClientUser(clientId, req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("portal@acme.com");
        assertThat(saved.getRole()).isEqualTo("CLIENT");
        assertThat(saved.getClientId()).isEqualTo(clientId);
        assertThat(saved.getPassword()).isNotEqualTo("supersecret");
        assertThat(passwordEncoder.matches("supersecret", saved.getPassword())).isTrue();

        assertThat(dto.getId()).isEqualTo(101L);
        assertThat(dto.getEmail()).isEqualTo("portal@acme.com");
        assertThat(dto.getRole()).isEqualTo("CLIENT");
        assertThat(dto.getClientId()).isEqualTo(clientId);
    }

    @Test
    void createClientUser_clientNotFound_throwsAndDoesNotSave() {
        when(clientRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.createClientUser(99L, request("a@x.com", "longenough")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createClientUser_duplicateEmail_throwsAndDoesNotSave() {
        when(clientRepository.existsById(5L)).thenReturn(true);
        when(userRepository.existsByEmail("dup@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createClientUser(5L, request("dup@x.com", "longenough")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("dup@x.com");

        verify(userRepository, never()).save(any());
    }

    // ----- listClientUsers -----

    @Test
    void listClientUsers_returnsMappedDtosOrderedAsRepoReturned() {
        Long clientId = 5L;
        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(userRepository.findAllByClientIdOrderByEmailAsc(clientId)).thenReturn(List.of(
                User.builder().id(1L).email("a@acme.com").role("CLIENT").clientId(clientId).build(),
                User.builder().id(2L).email("b@acme.com").role("CLIENT").clientId(clientId).build()
        ));

        List<ClientUserDto> dtos = service.listClientUsers(clientId);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getEmail()).isEqualTo("a@acme.com");
        assertThat(dtos.get(0).getClientId()).isEqualTo(clientId);
        assertThat(dtos.get(1).getEmail()).isEqualTo("b@acme.com");
    }

    @Test
    void listClientUsers_clientNotFound_throws() {
        when(clientRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.listClientUsers(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).findAllByClientIdOrderByEmailAsc(any());
    }

    // ----- deleteClientUser -----

    @Test
    void deleteClientUser_happyPath_deletes() {
        Long clientId = 5L;
        Long userId = 10L;
        User existing = User.builder().id(userId).email("u@x.com").role("CLIENT").clientId(clientId).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));

        service.deleteClientUser(clientId, userId);

        verify(userRepository).delete(existing);
    }

    @Test
    void deleteClientUser_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteClientUser(5L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteClientUser_userBelongsToDifferentClient_throws() {
        User other = User.builder().id(10L).email("u@x.com").role("CLIENT").clientId(7L).build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.deleteClientUser(5L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteClientUser_userHasNullClientId_throws() {
        User adminLike = User.builder().id(10L).email("u@x.com").role("ADMIN").clientId(null).build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(adminLike));

        assertThatThrownBy(() -> service.deleteClientUser(5L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).delete(any(User.class));
    }
}
