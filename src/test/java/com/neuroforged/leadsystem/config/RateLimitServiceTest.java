package com.neuroforged.leadsystem.config;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LSB-167: exercises the local-bucket fallback path (no Redis ProxyManager
 * injected). Covers the three bucket pools — login, api, public — to pin the
 * key namespacing so unrelated callers can't cross-pollute each other's
 * budgets. Redis behaviour is covered manually + by RateLimitService running
 * against the live dev/prod Redis after deploy.
 */
class RateLimitServiceTest {

    private RateLimitService newService() {
        // requestsPerMinute and requestsPerHour are wide enough not to trip in tests.
        return new RateLimitService(Optional.empty(), 1000, 100_000);
    }

    @Test
    void loginBucket_allowsFiveAttemptsThenThrottles() {
        RateLimitService svc = newService();

        for (int i = 0; i < 5; i++) {
            assertTrue(svc.tryConsumeLogin("user@example.com"), "attempt " + (i + 1) + " should be allowed");
        }
        assertFalse(svc.tryConsumeLogin("user@example.com"), "6th attempt must be throttled");
    }

    @Test
    void loginBucket_isPerEmail_andCaseInsensitive() {
        RateLimitService svc = newService();

        for (int i = 0; i < 5; i++) svc.tryConsumeLogin("Alice@Example.com");

        assertFalse(svc.tryConsumeLogin("alice@example.com"),
                "different casing must hit the same bucket");
        assertTrue(svc.tryConsumeLogin("bob@example.com"),
                "a different email must have its own fresh budget");
    }

    @Test
    void resetLogin_clearsTheBucket() {
        RateLimitService svc = newService();

        for (int i = 0; i < 5; i++) svc.tryConsumeLogin("user@example.com");
        assertFalse(svc.tryConsumeLogin("user@example.com"));

        svc.resetLogin("user@example.com");
        assertTrue(svc.tryConsumeLogin("user@example.com"),
                "post-reset, the bucket should refill to its full budget");
    }

    @Test
    void publicBucket_allowsFivePerEndpointIpPair() {
        RateLimitService svc = newService();

        for (int i = 0; i < 5; i++) {
            assertTrue(svc.tryConsumePublic("/api/contact", "1.2.3.4"));
        }
        assertFalse(svc.tryConsumePublic("/api/contact", "1.2.3.4"));

        // Different endpoint, same IP: separate bucket
        assertTrue(svc.tryConsumePublic("/api/newsletter", "1.2.3.4"));
        // Different IP, same endpoint: separate bucket
        assertTrue(svc.tryConsumePublic("/api/contact", "9.8.7.6"));
    }

    @Test
    void apiKeyBucket_isPerKey() {
        // Tight limits so the test doesn't have to consume thousands.
        RateLimitService svc = new RateLimitService(Optional.empty(), 3, 1000);

        for (int i = 0; i < 3; i++) assertTrue(svc.tryConsume("client-1"));
        assertFalse(svc.tryConsume("client-1"));

        assertTrue(svc.tryConsume("client-2"), "a different API key must have its own fresh budget");
    }

    @Test
    void tryConsumeLogin_isANoopForBlankEmail() {
        RateLimitService svc = newService();

        // Both should be no-ops returning true (request validation will reject them upstream).
        assertTrue(svc.tryConsumeLogin(null));
        assertTrue(svc.tryConsumeLogin(""));
        assertTrue(svc.tryConsumeLogin("   "));
    }
}
