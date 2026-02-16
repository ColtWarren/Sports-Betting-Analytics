package com.coltwarren.sports_betting_analytics.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Phase 1: Permit all endpoints - no lockdown yet
            // Phase 5 will restrict endpoints to authenticated users
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )

            // OAuth2 login with Google
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
            )

            // Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )

            // CSRF protection (enabled by default, disable for REST API endpoints)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            )

            // Session management
            .sessionManagement(session -> session
                .maximumSessions(3)
            );

        return http.build();
    }
}
