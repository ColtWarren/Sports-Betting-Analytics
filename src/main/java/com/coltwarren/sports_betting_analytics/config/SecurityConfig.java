package com.coltwarren.sports_betting_analytics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService oAuth2SuccessHandler;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    public SecurityConfig(CustomOAuth2UserService oAuth2SuccessHandler) {
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public routes - no auth required
                .requestMatchers("/", "/login", "/login/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/api/health", "/api/health/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**").permitAll()
                .requestMatchers("/error").permitAll()
                // Everything else requires authentication
                // (includes /best-bets, /api/best-bets/**, /api/odds/**, /api/soccer/**)
                .anyRequest().authenticated()
            );

        // Only configure OAuth2 if Google credentials are present
        boolean oauthConfigured = googleClientId != null && !googleClientId.isBlank() && !"none".equals(googleClientId);
        if (oauthConfigured) {
            http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .successHandler(oAuth2SuccessHandler)
                .failureUrl("/login?error=true")
            );
        } else {
            // No OAuth2 configured - use form login as fallback
            http.formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            );
        }

        http
            // Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )

            // CSRF protection - cookie-based token for JS compatibility
            // All POST/PUT/DELETE endpoints require CSRF token
            // JS reads XSRF-TOKEN cookie and sends X-XSRF-TOKEN header
            // Thymeleaf forms include token automatically as hidden field
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )

            // Session management
            .sessionManagement(session -> session
                .maximumSessions(3)
            );

        return http.build();
    }
}
