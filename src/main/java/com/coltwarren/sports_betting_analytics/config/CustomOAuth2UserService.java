package com.coltwarren.sports_betting_analytics.config;

import com.coltwarren.sports_betting_analytics.model.User;
import com.coltwarren.sports_betting_analytics.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            String googleId = oAuth2User.getAttribute("sub");
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String pictureUrl = oAuth2User.getAttribute("picture");

            log.info("OAuth2 login attempt: googleId={}, email={}, name={}", googleId, email, name);

            // Find existing user or create new one
            Optional<User> existingUser = userRepository.findByGoogleId(googleId);

            if (existingUser.isPresent()) {
                // Update existing user on login
                User user = existingUser.get();
                user.setName(name);
                user.setPictureUrl(pictureUrl);
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
                log.info("User logged in: {} ({})", user.getName(), user.getEmail());
            } else {
                // Create new user
                User newUser = new User(email, name, googleId);
                newUser.setPictureUrl(pictureUrl);
                newUser.setLastLoginAt(LocalDateTime.now());
                User saved = userRepository.save(newUser);
                log.info("New user registered: {} ({}) with id={}", name, email, saved.getId());
            }
        } catch (Exception e) {
            log.error("Failed to save user during OAuth2 login", e);
        }

        return oAuth2User;
    }
}
