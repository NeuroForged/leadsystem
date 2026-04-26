package com.neuroforged.leadsystem.config;

import com.neuroforged.leadsystem.entity.User;
import com.neuroforged.leadsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StartupConfig {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Value("${neuroforged.admin.email}")
    private String adminEmail;

    @Value("${neuroforged.admin.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner seedAdminUser() {
        return args -> {
            var existing = userRepository.findByEmail(adminEmail);
            if (existing.isEmpty()) {
                User admin = User.builder()
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .role("ADMIN")
                        .build();

                userRepository.save(admin);
                log.info("✅ Admin user created: {}", adminEmail);
            } else {
                User admin = existing.get();
                if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    userRepository.save(admin);
                    log.warn("⚠️ Admin password was not BCrypt-encoded — re-seeded with correct hash");
                } else {
                    log.info("ℹ️ Admin user already exists: {}", adminEmail);
                }
            }
        };
    }
}
