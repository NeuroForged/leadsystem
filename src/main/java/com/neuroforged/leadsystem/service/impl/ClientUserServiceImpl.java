package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.dto.ClientUserDto;
import com.neuroforged.leadsystem.dto.CreateClientUserRequest;
import com.neuroforged.leadsystem.entity.User;
import com.neuroforged.leadsystem.exception.DuplicateResourceException;
import com.neuroforged.leadsystem.exception.ResourceNotFoundException;
import com.neuroforged.leadsystem.repository.ClientRepository;
import com.neuroforged.leadsystem.repository.UserRepository;
import com.neuroforged.leadsystem.security.UserRole;
import com.neuroforged.leadsystem.service.ClientUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientUserServiceImpl implements ClientUserService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ClientUserDto createClientUser(Long clientId, CreateClientUserRequest request) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client not found: " + clientId);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User already exists: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CLIENT.name())
                .clientId(clientId)
                .build();

        return toDto(userRepository.save(user));
    }

    @Override
    public List<ClientUserDto> listClientUsers(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client not found: " + clientId);
        }
        return userRepository.findAllByClientIdOrderByEmailAsc(clientId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteClientUser(Long clientId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (user.getClientId() == null || !user.getClientId().equals(clientId)) {
            throw new ResourceNotFoundException("User " + userId + " is not associated with client " + clientId);
        }
        userRepository.delete(user);
    }

    private ClientUserDto toDto(User user) {
        return ClientUserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .clientId(user.getClientId())
                .build();
    }
}
