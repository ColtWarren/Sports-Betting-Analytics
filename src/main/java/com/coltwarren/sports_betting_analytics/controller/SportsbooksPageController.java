package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.SportsbookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Sportsbooks Directory Page Controller
 * Displays all 8 Missouri-licensed sportsbooks with ratings and bonuses
 */
@Controller
public class SportsbooksPageController {

    @Autowired
    private SportsbookService sportsbookService;

    /**
     * Sportsbooks Directory Page
     * Shows all Missouri-licensed sportsbooks with ratings, bonuses, and descriptions
     */
    @GetMapping("/sportsbooks")
    public String sportsbooksPage(Model model) {
        model.addAttribute("sportsbooks", sportsbookService.getAllSportsbooks());
        model.addAttribute("totalCount", sportsbookService.getCount());
        model.addAttribute("avgRating", sportsbookService.getAverageRating());
        return "sportsbooks";
    }
}
