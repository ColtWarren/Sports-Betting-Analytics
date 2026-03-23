package com.coltwarren.sports_betting_analytics.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates service-to-service requests using an internal API key.
 *
 * The best-bets-tracker app sends this header on automated calls:
 *   X-Internal-API-Key: {shared secret from INTERNAL_API_KEY env var}
 *
 * If the key matches, the request is authenticated as a "SERVICE" user
 * with ROLE_SERVICE authority — bypassing OAuth2/form login.
 *
 * This filter runs BEFORE Spring Security's authentication filters,
 * so if the key is valid, no further auth is needed.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Internal-API-Key";

    @Value("${internal.api.key:}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Only process if the header is present and the key is configured
        String providedKey = request.getHeader(HEADER_NAME);
        if (providedKey != null && !internalApiKey.isBlank() && internalApiKey.equals(providedKey)) {

            // Authenticate as a service account — no user session needed
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "SERVICE",          // principal
                    null,               // credentials (not needed)
                    List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
