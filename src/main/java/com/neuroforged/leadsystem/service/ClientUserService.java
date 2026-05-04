package com.neuroforged.leadsystem.service;

import com.neuroforged.leadsystem.dto.ClientUserDto;
import com.neuroforged.leadsystem.dto.CreateClientUserRequest;

import java.util.List;

public interface ClientUserService {

    ClientUserDto createClientUser(Long clientId, CreateClientUserRequest request);

    List<ClientUserDto> listClientUsers(Long clientId);

    void deleteClientUser(Long clientId, Long userId);
}
