package com.neuroforged.leadsystem.service.impl;

import com.neuroforged.leadsystem.service.ClientOAuthStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ClientOAuthStateServiceImpl implements ClientOAuthStateService {

    private final Map<String, Long> stateStore = new ConcurrentHashMap<>();

    @Override
    public Optional<Long> findClientIdByOAuthState(String state) {
        return Optional.ofNullable(stateStore.get(state));
    }

    @Override
    public void saveOAuthState(String state, Long clientId) {
        stateStore.put(state, clientId);
    }
}
