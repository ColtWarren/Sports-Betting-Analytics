package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.UserContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @Autowired
    private UserContextService userContextService;

    @GetMapping("/login")
    public String loginPage(Model model) {
        // If already authenticated, redirect to dashboard
        if (userContextService.isAuthenticated()) {
            return "redirect:/";
        }
        return "login";
    }
}
