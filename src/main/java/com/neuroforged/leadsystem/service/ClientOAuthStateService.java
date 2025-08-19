package com.neuroforged.leadsystem.service;

import java.util.Optional;

public interface ClientOAuthStateService {
    Optional<Long> findClientIdByOAuthState(String state);
    void saveOAuthState(String state, Long clientId);
}
