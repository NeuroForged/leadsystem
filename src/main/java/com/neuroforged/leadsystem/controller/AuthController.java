package com.neuroforged.leadsystem.controller;

import com.neuroforged.leadsystem.dto.AuthenticationRequest;
import com.neuroforged.leadsystem.dto.AuthenticationResponse;
import com.neuroforged.leadsystem.dto.ChangePasswordRequest;
import com.neuroforged.leadsystem.dto.UserInfoResponse;
import com.neuroforged.leadsystem.entity.User;
import com.neuroforged.leadsystem.logging.BusinessEventLogger;
import com.neuroforged.leadsystem.metrics.LeadSystemMetrics;
import com.neuroforged.leadsystem.repository.UserRepository;
import com.neuroforged.leadsystem.security.CustomUserPrincipal;
import com.neuroforged.leadsystem.security.JwtUtil;
import com.neuroforged.leadsystem.security.AuthPrincipalUtil;
import com.neuroforged.leadsystem.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Arrays;

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
    private final Environment environment;
    private final BusinessEventLogger eventLogger;
    private final LeadSystemMetrics metrics;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest request,
                                   HttpServletResponse response) {
        try {
            log.debug("Login attempt for email={}", request.getEmail());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
            String accessToken = jwtUtil.generateToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user);

            boolean secure = isProdProfile();
            addCookie(response, "alchemize_at", accessToken, (int) jwtUtil.getAccessTokenMaxAge(), secure);
            addCookie(response, "alchemize_rt", refreshToken, (int) jwtUtil.getRefreshTokenMaxAge(), secure);

            eventLogger.authSuccess(request.getEmail());
            return ResponseEntity.ok(new AuthenticationResponse(accessToken));
        } catch (AuthenticationException e) {
            metrics.recordAuthFailure("bad_credentials");
            eventLogger.authFailed("bad_credentials", request.getEmail(), null);
            log.debug("Login failed for email={}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(401).body("Invalid email or password");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "alchemize_rt");
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            metrics.recordAuthFailure("invalid_token");
            return ResponseEntity.status(401).body("Missing or invalid refresh token");
        }

        String tokenType;
        String email;
        try {
            tokenType = jwtUtil.extractTokenType(refreshToken);
            email = jwtUtil.extractUsername(refreshToken);
        } catch (Exception e) {
            metrics.recordAuthFailure("invalid_token");
            return ResponseEntity.status(401).body("Invalid refresh token");
        }

        if (!"refresh".equals(tokenType)) {
            metrics.recordAuthFailure("invalid_token_type");
            return ResponseEntity.status(401).body("Invalid token type");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            metrics.recordAuthFailure("user_not_found");
            return ResponseEntity.status(401).body("User not found");
        }

        String newAccessToken = jwtUtil.generateToken(user);
        boolean secure = isProdProfile();
        addCookie(response, "alchemize_at", newAccessToken, (int) jwtUtil.getAccessTokenMaxAge(), secure);

        return ResponseEntity.ok(new UserInfoResponse(user.getEmail(), user.getRole(), user.getClientId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        boolean secure = isProdProfile();
        clearCookie(response, "alchemize_at", secure);
        clearCookie(response, "alchemize_rt", secure);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ResponseEntity.ok(new UserInfoResponse(
                principal.getUsername(),
                principal.getRole(),
                principal.getClientId()
        ));
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

    private boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge, boolean secure) {
        response.addHeader("Set-Cookie", buildSetCookieHeader(name, value, maxAge, secure));
    }

    private void clearCookie(HttpServletResponse response, String name, boolean secure) {
        response.addHeader("Set-Cookie", buildSetCookieHeader(name, "", 0, secure));
    }

    private String buildSetCookieHeader(String name, String value, int maxAge, boolean secure) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value)
                .append("; Path=/")
                .append("; HttpOnly")
                .append("; SameSite=Lax")
                .append("; Max-Age=").append(maxAge)
                .append("; Domain=alchemizeiq.com");
        if (secure) {
            sb.append("; Secure");
        }
        return sb.toString();
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
