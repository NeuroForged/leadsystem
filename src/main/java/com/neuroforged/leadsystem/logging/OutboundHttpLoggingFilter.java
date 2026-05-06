package com.neuroforged.leadsystem.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * WebClient ExchangeFilterFunction that logs every outgoing HTTP call at DEBUG level.
 * Captures method, host+path (no query params to avoid token leakage), HTTP status,
 * and latency.  Authorization header values are redacted.
 *
 * Usage: webClientBuilder.filter(OutboundHttpLoggingFilter.logRequest())
 * LSB-137.
 */
public class OutboundHttpLoggingFilter {

    private static final Logger log = LoggerFactory.getLogger("outbound.http");

    private OutboundHttpLoggingFilter() {}

    public static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            if (log.isDebugEnabled()) {
                log.debug("→ {} {}", request.method(), sanitise(request));
            }
            return Mono.just(request);
        });
    }

    public static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (log.isDebugEnabled()) {
                log.debug("← {}", response.statusCode());
            }
            return Mono.just(response);
        });
    }

    /**
     * Timed variant: wraps the exchange and logs method, path, status and latency together.
     * Preferred over separate request/response filters.
     */
    public static ExchangeFilterFunction logWithTiming() {
        return (request, next) -> {
            long start = Instant.now().toEpochMilli();
            String label = request.method() + " " + sanitise(request);
            return next.exchange(request)
                    .doOnNext(response -> {
                        long latency = Instant.now().toEpochMilli() - start;
                        log.debug("[outbound] {} → {} {}ms", label, response.statusCode().value(), latency);
                    })
                    .doOnError(err ->
                        log.warn("[outbound] {} → ERROR {}ms: {}", label,
                                Instant.now().toEpochMilli() - start, err.getMessage()));
        };
    }

    /** Strip query params to avoid accidentally logging tokens or API keys in URLs. */
    private static String sanitise(ClientRequest request) {
        String uri = request.url().toString();
        int q = uri.indexOf('?');
        return q >= 0 ? uri.substring(0, q) : uri;
    }
}
