package com.coltwarren.sports_betting_analytics.config;

import com.coltwarren.sports_betting_analytics.model.User;
import com.coltwarren.sports_betting_analytics.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Saves/updates the user in the database after successful OAuth2 login,
 * then redirects to /dashboard.
 *
 * This replaces the old DefaultOAuth2UserService approach which failed
 * because Google OIDC doesn't call the OAuth2 userInfo endpoint —
 * super.loadUser() threw before our save code could run.
 */
@Component
@Slf4j
public class CustomOAuth2UserService implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String pictureUrl = oAuth2User.getAttribute("picture");

        log.info("OAuth2 login success: googleId={}", googleId);

        try {
            Optional<User> existingUser = userRepository.findByGoogleIdAndDeletedFalse(googleId);

            if (existingUser.isPresent()) {
                User user = existingUser.get();
                user.setName(name);
                user.setPictureUrl(pictureUrl);
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
                log.info("User updated: id={}", user.getId());
            } else {
                User newUser = new User(email, name, googleId);
                newUser.setPictureUrl(pictureUrl);
                newUser.setLastLoginAt(LocalDateTime.now());
                User saved = userRepository.save(newUser);
                log.info("New user registered: id={}", saved.getId());
            }
        } catch (Exception e) {
            log.error("Failed to save user during OAuth2 login", e);
        }

        response.sendRedirect("/dashboard");
    }
}
