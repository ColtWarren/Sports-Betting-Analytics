package com.coltwarren.sports_betting_analytics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient used by external API services
 * (CFBD, Claude AI, Odds API, etc.)
 *
 * IMPORTANT: Basketball API responses can be very large (5000+ games, 350+ teams)
 * Default buffer limit is 256KB which causes DataBufferLimitException
 * We increase the buffer to 10MB to handle large JSON responses
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        // Configure exchange strategies with increased buffer size
        // Default: 256KB (262,144 bytes) - TOO SMALL for basketball data
        // New limit: 10MB (10,485,760 bytes) - Handles even the largest API responses
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024)) // 10MB buffer
            .build();

        return WebClient.builder()
            .exchangeStrategies(strategies);
    }
}
