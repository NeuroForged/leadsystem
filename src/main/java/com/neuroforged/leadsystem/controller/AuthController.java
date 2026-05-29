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
    private final com.neuroforged.leadsystem.config.RateLimitService rateLimitService;
    private final com.neuroforged.leadsystem.service.PasswordResetService passwordResetService;

    // LSB-159: cookie Domain is per-environment. Blank in local (host-only cookies);
    // set to "alchemizeiq.com" in prod/dev so subdomains (app/api/...) share the auth cookie.
    @org.springframework.beans.factory.annotation.Value("${neuroforged.cookie.domain:}")
    private String cookieDomain;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest request,
                                   HttpServletResponse response) {
        // LSB-160: per-email rate limit before any DB / password-encoder work.
        // Slows credential-stuffing and password-spray attacks. Successful login
        // clears the bucket so legitimate users aren't locked out after a typo.
        if (!rateLimitService.tryConsumeLogin(request.getEmail())) {
            metrics.recordAuthFailure("rate_limited");
            eventLogger.authFailed("rate_limited", request.getEmail(), null);
            return ResponseEntity.status(429)
                    .header("Retry-After", "300")
                    .body("Too many login attempts. Try again in a few minutes.");
        }
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

            rateLimitService.resetLogin(request.getEmail());
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        authService.changePassword(principal.getName(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    /**
     * LSB-164: Trigger password-reset email. Always returns 200 to avoid email
     * enumeration. Rate-limited per email by {@link RateLimitService#tryConsumeLogin}.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody java.util.Map<String, String> body) {
        String email = body.getOrDefault("email", "");
        if (!email.isBlank()) {
            // LSB-164: per-email rate limit. Reuses the login bucket (5/5min/email)
            // because the cost model is the same — both endpoints accept an email
            // and trigger expensive server work. Returning 200 either way preserves
            // the anti-enumeration property.
            if (rateLimitService.tryConsumeLogin(email)) {
                passwordResetService.requestReset(email);
            } else {
                log.warn("Password-reset rate-limited for email={}", email);
            }
        }
        return ResponseEntity.ok().build();
    }

    /**
     * LSB-164: Reset password using emailed token.
     * Returns 400 on bad / expired / re-used token (no further detail to avoid
     * confirming the validity of a leaked token to an attacker).
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody java.util.Map<String, String> body) {
        String token = body.getOrDefault("token", "");
        String newPassword = body.getOrDefault("newPassword", "");
        if (token.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body("Token and newPassword are required.");
        }
        try {
            passwordResetService.completeReset(token, newPassword);
            return ResponseEntity.ok().build();
        } catch (com.neuroforged.leadsystem.service.PasswordResetService.InvalidResetTokenException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
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
                .append("; Max-Age=").append(maxAge);
        // LSB-159: only set the Domain attribute when configured. Local dev gets a
        // host-only cookie (omitting Domain altogether) which the browser keeps for
        // 127.0.0.1 / localhost / a custom dev host.
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            sb.append("; Domain=").append(cookieDomain);
        }
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
