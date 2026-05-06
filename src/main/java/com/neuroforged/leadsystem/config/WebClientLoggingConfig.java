package com.neuroforged.leadsystem.config;

import com.neuroforged.leadsystem.logging.OutboundHttpLoggingFilter;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the outbound HTTP logging filter on every WebClient.Builder instance
 * auto-wired via Spring Boot.  Captures method, URL (sanitised), status code,
 * and latency for all calls to Calendly API, scraper, and any other outbound clients.
 * LSB-137.
 */
@Configuration
public class WebClientLoggingConfig {

    @Bean
    public WebClientCustomizer outboundHttpLoggingCustomizer() {
        return builder -> builder.filter(OutboundHttpLoggingFilter.logWithTiming());
    }
}
