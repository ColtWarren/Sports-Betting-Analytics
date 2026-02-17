package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.User;
import com.coltwarren.sports_betting_analytics.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Helper service to get the current authenticated user.
 * Used by all services for row-level security (user-scoped data access).
 */
@Service
@Slf4j
public class UserContextService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Get the current authenticated User entity.
     * Returns empty if not authenticated or user not found.
     */
    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof OAuth2User oAuth2User) {
            String googleId = oAuth2User.getAttribute("sub");
            if (googleId != null) {
                return userRepository.findByGoogleId(googleId);
            }
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
