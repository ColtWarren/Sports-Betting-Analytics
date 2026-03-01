package com.coltwarren.sports_betting_analytics.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Centralized WebClient configuration.
 *
 * Provides a prototype-scoped builder with shared buffer settings,
 * plus pre-built WebClient beans for common external APIs.
 */
@Configuration
public class WebClientConfig {

    private static final int MAX_BUFFER_SIZE = 16 * 1024 * 1024; // 16MB

    /**
     * Prototype-scoped builder — each injection gets a fresh instance
     * with the shared 16MB buffer. Services can set their own baseUrl.
     */
    @Bean
    @Scope("prototype")
    public WebClient.Builder webClientBuilder() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(MAX_BUFFER_SIZE))
            .build();

        return WebClient.builder()
            .exchangeStrategies(strategies);
    }

    /**
     * Pre-built ESPN WebClient (site API — scoreboard, stats, etc.)
     */
    @Bean("espnWebClient")
    public WebClient espnWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
            .baseUrl("https://site.api.espn.com/apis/site/v2/sports")
            .build();
    }

    /**
     * Pre-built Odds API WebClient — base URL from OddsApiProperties.
     */
    @Bean("oddsApiWebClient")
    public WebClient oddsApiWebClient(WebClient.Builder webClientBuilder,
                                      OddsApiProperties oddsApiProperties) {
        return webClientBuilder
            .baseUrl(oddsApiProperties.getBaseUrl())
            .build();
    }
}
