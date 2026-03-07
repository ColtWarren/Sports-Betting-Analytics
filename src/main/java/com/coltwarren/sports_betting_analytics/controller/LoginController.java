package com.coltwarren.sports_betting_analytics.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @GetMapping("/login")
    public String loginPage(Model model) {
        // If already authenticated via OAuth2, redirect to dashboard
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof OAuth2User) {
            return "redirect:/dashboard";
        }
        boolean oauthEnabled = googleClientId != null && !googleClientId.isBlank();
        model.addAttribute("oauthEnabled", oauthEnabled);
        return "login";
    }
}
