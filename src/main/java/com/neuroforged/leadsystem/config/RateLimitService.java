package com.neuroforged.leadsystem.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    @Value("${rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${rate-limit.requests-per-hour:500}")
    private int requestsPerHour;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    // LSB-161: separate bucket map for low-rate public endpoints (contact, newsletter).
    // Key shape: "<endpoint>:<ip>". Stricter limits to deter spam-bots without
    // affecting legitimate burst usage of /api/leads.
    private final ConcurrentHashMap<String, Bucket> publicBuckets = new ConcurrentHashMap<>();

    // LSB-160: per-email login bucket. Slows credential-stuffing / password-spray.
    // Key shape: lowercased email. 5 attempts per 5 minutes per email.
    private final ConcurrentHashMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String apiKey) {
        return buckets.computeIfAbsent(apiKey, this::newBucket).tryConsume(1);
    }

    public long getSecondsUntilRefill(String apiKey) {
        return buckets.computeIfAbsent(apiKey, this::newBucket)
                .getAvailableTokens() > 0 ? 0 : 60;
    }

    /**
     * LSB-161: 5 requests per hour per (endpoint, ip) pair.
     * Returns {@code true} if the request is allowed; {@code false} when the bucket is empty.
     */
    public boolean tryConsumePublic(String endpoint, String ip) {
        String key = endpoint + ":" + ip;
        return publicBuckets.computeIfAbsent(key, k -> newPublicBucket()).tryConsume(1);
    }

    /**
     * LSB-160: 5 login attempts per 5 minutes per email. Successful login should
     * reset the bucket via {@link #resetLogin(String)}.
     */
    public boolean tryConsumeLogin(String email) {
        if (email == null || email.isBlank()) return true; // bad request will fail upstream
        String key = email.toLowerCase().trim();
        return loginBuckets.computeIfAbsent(key, k -> newLoginBucket()).tryConsume(1);
    }

    /**
     * LSB-160: clear the login bucket for an email after a successful authentication.
     * Keeps legitimate users from being throttled if they fat-fingered the password a
     * few times before finally getting in.
     */
    public void resetLogin(String email) {
        if (email == null || email.isBlank()) return;
        loginBuckets.remove(email.toLowerCase().trim());
    }

    private Bucket newBucket(String key) {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(requestsPerMinute, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(requestsPerHour, Duration.ofHours(1)))
                .build();
    }

    private Bucket newPublicBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(5, Duration.ofHours(1)))
                .build();
    }

    private Bucket newLoginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(5, Duration.ofMinutes(5)))
                .build();
    }
}
