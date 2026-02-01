package com.coltwarren.sports_betting_analytics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger Configuration
 *
 * Provides interactive API documentation at /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sportsBettingOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Sports Betting Analytics API")
                .description("AI-powered sports betting analytics platform with multi-factor recommendations, " +
                    "xG soccer analysis, pattern recognition, and backtesting capabilities.")
                .version("2.7.0")
                .contact(new Contact()
                    .name("Sports Betting Analytics")
                    .email("support@sportsbettinganalytics.com"))
                .license(new License()
                    .name("Private")
                    .url("https://sportsbettinganalytics.com")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local Development"),
                new Server().url("https://api.sportsbettinganalytics.com").description("Production")
            ));
    }
}
