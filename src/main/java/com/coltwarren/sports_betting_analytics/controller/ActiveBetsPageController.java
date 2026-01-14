package com.coltwarren.sports_betting_analytics.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ActiveBetsPageController {
    
    @GetMapping("/active-bets")
    public String activeBetsPage() {
        return "active-bets";
    }
}
