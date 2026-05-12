package com.neuroforged.leadsystem.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bucket4j-backed rate limit dispatcher.
 *
 * <p>LSB-167: in prod and local-dev (with Redis running) buckets live in Redis
 * via {@link RedisRateLimitConfig}'s Lettuce-based {@link ProxyManager}, so
 * limits survive Coolify rollouts and stay coherent across replicas. The
 * previous in-memory ConcurrentHashMap reset on every restart — a brute-force
 * attempt could time itself around a deploy to get a clean budget.
 *
 * <p>When the {@code neuroforged.redis.url} property is missing — the
 * integration-test profile keeps it blank — the constructor falls back to a
 * ConcurrentHashMap of {@link io.github.bucket4j.local.LocalBucket} so test
 * suites don't need a Redis container just to exercise unrelated controllers.
 *
 * <p>Key namespaces (Redis prefix / map key shape):
 * <ul>
 *   <li>{@code rl:api:<apiKey>} — LSB-37 chatbot lead submission</li>
 *   <li>{@code rl:public:<endpoint>:<ip>} — LSB-161 contact/newsletter</li>
 *   <li>{@code rl:login:<email>} — LSB-160 login per-email</li>
 * </ul>
 */
@Service
public class RateLimitService {

    private static final String KEY_API    = "rl:api:";
    private static final String KEY_PUBLIC = "rl:public:";
    private static final String KEY_LOGIN  = "rl:login:";

    private final Optional<ProxyManager<String>> proxyManager;
    private final BucketConfiguration apiKeyConfig;
    private final BucketConfiguration publicConfig;
    private final BucketConfiguration loginConfig;

    // Fallback used only when proxyManager is empty (test profile, no Redis).
    private final ConcurrentHashMap<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    public RateLimitService(
            Optional<ProxyManager<String>> proxyManager,
            @Value("${rate-limit.requests-per-minute:60}") int requestsPerMinute,
            @Value("${rate-limit.requests-per-hour:500}") int requestsPerHour) {
        this.proxyManager = proxyManager;
        this.apiKeyConfig = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(requestsPerMinute, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(requestsPerHour, Duration.ofHours(1)))
                .build();
        this.publicConfig = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(5, Duration.ofHours(1)))
                .build();
        this.loginConfig = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(5, Duration.ofMinutes(5)))
                .build();
    }

    public boolean tryConsume(String apiKey) {
        return bucket(KEY_API + apiKey, apiKeyConfig).tryConsume(1);
    }

    public long getSecondsUntilRefill(String apiKey) {
        return bucket(KEY_API + apiKey, apiKeyConfig).getAvailableTokens() > 0 ? 0 : 60;
    }

    /**
     * LSB-161: 5 requests per hour per (endpoint, ip) pair.
     * Returns {@code true} if the request is allowed; {@code false} when the bucket is empty.
     */
    public boolean tryConsumePublic(String endpoint, String ip) {
        return bucket(KEY_PUBLIC + endpoint + ":" + ip, publicConfig).tryConsume(1);
    }

    /**
     * LSB-160: 5 login attempts per 5 minutes per email. Successful login should
     * reset the bucket via {@link #resetLogin(String)}.
     */
    public boolean tryConsumeLogin(String email) {
        if (email == null || email.isBlank()) return true;
        return bucket(KEY_LOGIN + normaliseEmail(email), loginConfig).tryConsume(1);
    }

    /**
     * LSB-160: clear the login bucket for an email after a successful authentication.
     * Keeps legitimate users from being throttled if they fat-fingered the password a
     * few times before finally getting in.
     */
    public void resetLogin(String email) {
        if (email == null || email.isBlank()) return;
        String key = KEY_LOGIN + normaliseEmail(email);
        proxyManager.ifPresentOrElse(
                pm -> pm.removeProxy(key),
                () -> localBuckets.remove(key)
        );
    }

    // ── Bucket dispatch ─────────────────────────────────────────────────────

    /**
     * Returns a usable bucket for the given key, either via the Redis
     * {@link ProxyManager} when available, or a JVM-local in-memory bucket
     * (test profile only).
     */
    @SuppressWarnings("unchecked")
    private Bucket bucket(String key, BucketConfiguration config) {
        return proxyManager
                .map(pm -> (Bucket) pm.builder().build(key, () -> config))
                .orElseGet(() -> localBuckets.computeIfAbsent(key, k -> newLocalBucket(config)));
    }

    private static Bucket newLocalBucket(BucketConfiguration config) {
        var builder = Bucket.builder();
        for (Bandwidth bw : config.getBandwidths()) {
            builder.addLimit(bw);
        }
        return builder.build();
    }

    private static String normaliseEmail(String email) {
        return email.toLowerCase().trim();
    }
}
