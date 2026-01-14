package com.coltwarren.sports_betting_analytics.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BestBetsPageController {
    
    @GetMapping("/best-bets")
    public String bestBetsPage() {
        return "best-bets";
    }
}
