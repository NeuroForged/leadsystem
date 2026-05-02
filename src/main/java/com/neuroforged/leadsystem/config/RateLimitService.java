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

    public boolean tryConsume(String apiKey) {
        return buckets.computeIfAbsent(apiKey, this::newBucket).tryConsume(1);
    }

    public long getSecondsUntilRefill(String apiKey) {
        return buckets.computeIfAbsent(apiKey, this::newBucket)
                .getAvailableTokens() > 0 ? 0 : 60;
    }

    private Bucket newBucket(String key) {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(requestsPerMinute, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(requestsPerHour, Duration.ofHours(1)))
                .build();
    }
}
