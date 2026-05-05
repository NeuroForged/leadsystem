package com.neuroforged.leadsystem.security;

import com.neuroforged.leadsystem.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@RequiredArgsConstructor
public class CustomUserDetailsConfig {

    private final UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepository.findByEmail(email)
                .map(user -> new CustomUserPrincipal(
                        user.getEmail(),
                        user.getPassword(),
                        user.getRole(),
                        user.getClientId()
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @PostConstruct
    public void init() {
        System.out.println("✅ UserDetailsService initialized");
    }
}
