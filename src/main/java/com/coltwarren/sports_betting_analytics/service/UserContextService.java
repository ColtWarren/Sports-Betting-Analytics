package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.User;
import com.coltwarren.sports_betting_analytics.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

/**
 * Helper service to get the current authenticated user.
 * Used by all services for row-level security (user-scoped data access).
 * Caches the DB lookup per request to avoid repeated queries.
 */
@Service
@Slf4j
public class UserContextService {

    private static final String CACHE_KEY = "UserContextService.currentUser";
    private static final String CACHE_MISS_KEY = "UserContextService.noUser";

    private final UserRepository userRepository;

    public UserContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get the current authenticated User entity.
     * Returns empty if not authenticated or user not found.
     * Result is cached per-request to avoid repeated DB lookups.
     */
    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof OAuth2User oAuth2User) {
            String googleId = oAuth2User.getAttribute("sub");
            if (googleId == null) {
                return Optional.empty();
            }

            // Check per-request cache
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                User cached = (User) attrs.getAttribute(CACHE_KEY, RequestAttributes.SCOPE_REQUEST);
                if (cached != null) return Optional.of(cached);
                if (Boolean.TRUE.equals(attrs.getAttribute(CACHE_MISS_KEY, RequestAttributes.SCOPE_REQUEST))) {
                    return Optional.empty();
                }
            }

            // DB lookup (once per request)
            Optional<User> user = userRepository.findByGoogleId(googleId);

            // Cache the result
            if (attrs != null) {
                if (user.isPresent()) {
                    attrs.setAttribute(CACHE_KEY, user.get(), RequestAttributes.SCOPE_REQUEST);
                } else {
                    attrs.setAttribute(CACHE_MISS_KEY, true, RequestAttributes.SCOPE_REQUEST);
                }
            }

            return user;
        }

        return Optional.empty();
    }

    /**
     * Get current user's ID. Returns null if not authenticated.
     */
    public Long getCurrentUserId() {
        return getCurrentUser().map(User::getId).orElse(null);
    }

    /**
     * Check if a user is currently authenticated.
     */
    public boolean isAuthenticated() {
        return getCurrentUser().isPresent();
    }

    /**
     * Check if current user is an admin.
     */
    public boolean isAdmin() {
        return getCurrentUser().map(User::isAdmin).orElse(false);
    }
}
