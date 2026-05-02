package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.AuthenticationRequest;
import com.neuroforged.leadsystem.dto.AuthenticationResponse;
import com.neuroforged.leadsystem.dto.ChangePasswordRequest;
import com.neuroforged.leadsystem.entity.User;
import com.neuroforged.leadsystem.repository.UserRepository;
import com.neuroforged.leadsystem.security.JwtUtil;
import com.neuroforged.leadsystem.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest request) {
        try {
            log.info("🔐 Attempting login for email: {}", request.getEmail());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            log.info("✅ Authentication successful for: {}", request.getEmail());

            User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
            String token = jwtUtil.generateToken(user);

            return ResponseEntity.ok(new AuthenticationResponse(token));
        } catch (AuthenticationException e) {
            log.warn("❌ Authentication failed for {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(401).body("Invalid email or password");
        }
    }


    @PatchMapping("/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        authService.changePassword(principal.getName(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthenticationRequest request) {
        log.info("Attempting to Register new user: {}", request.getEmail());
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.info("User already exists");
            return ResponseEntity.badRequest().body("User already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .build();

        userRepository.save(user);
        log.info("User registered successfully");
        return ResponseEntity.ok("User registered successfully");
    }
}
