package com.neuroforged.leadsystem.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LSB-167: Wires a single Bucket4j {@link ProxyManager} backed by Redis (via
 * Lettuce) so every {@link RateLimitService} bucket is shared across restarts
 * and across replicas. The previous in-memory {@code ConcurrentHashMap} reset
 * on every Coolify rollout, which gave brute-force attempts a free fresh
 * budget mid-deploy.
 *
 * <p>Config:
 * <pre>
 *   neuroforged.redis.url=redis://localhost:6379  (default for local dev)
 *   REDIS_URL                                     (Coolify env override)
 * </pre>
 *
 * <p>Keys live under {@code bucket4j:&lt;name&gt;:&lt;id&gt;} with a TTL set by the
 * expiration strategy below (longest configured refill window + a small margin),
 * so abandoned bucket entries garbage-collect themselves rather than growing
 * unbounded.
 */
@Slf4j
@Configuration
// LSB-167: only wire the Redis ProxyManager when a URL is configured. The
// integration tests under `application-test.yml` leave it blank so they fall
// back to local buckets via {@link RateLimitService}'s constructor.
@ConditionalOnProperty(name = "neuroforged.redis.url", matchIfMissing = false)
public class RedisRateLimitConfig {

    @Value("${neuroforged.redis.url:redis://localhost:6379}")
    private String redisUrl;

    private RedisClient redisClient;
    private StatefulRedisConnection<String, byte[]> connection;

    @Bean
    public ProxyManager<String> rateLimitProxyManager() {
        log.info("LSB-167: connecting Bucket4j to Redis at {}", redactPassword(redisUrl));
        redisClient = RedisClient.create(redisUrl);
        connection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        return LettuceBasedProxyManager.builderFor(connection)
                // 1-hour idle TTL: longer than any bucket's refill window (hour-based
                // public bucket is the widest) so live buckets stay warm, but abandoned
                // keys (one-off IPs / emails) drop after 1h of inactivity.
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.fixedTimeToLive(Duration.ofHours(1)))
                .build();
    }

    @PreDestroy
    public void shutdown() {
        if (connection != null) connection.close();
        if (redisClient != null) redisClient.shutdown();
    }

    /** Strip the password (everything between {@code :} and {@code @}) for log lines. */
    private static String redactPassword(String url) {
        return url == null ? "null" : url.replaceAll("://[^@]+@", "://***@");
    }
}
