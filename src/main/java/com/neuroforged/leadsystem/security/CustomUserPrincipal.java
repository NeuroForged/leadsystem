package com.neuroforged.leadsystem.security;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

public class CustomUserPrincipal extends User {

    private final String role;
    private final Long clientId;

    public CustomUserPrincipal(String email, String password, String role, Long clientId) {
        super(email, password, AuthorityUtils.createAuthorityList("ROLE_" + role));
        this.role = role;
        this.clientId = clientId;
    }

    public String getRole() {
        return role;
    }

    public Long getClientId() {
        return clientId;
    }
}
