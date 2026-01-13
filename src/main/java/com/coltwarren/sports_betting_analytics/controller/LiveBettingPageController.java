package com.coltwarren.sports_betting_analytics.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LiveBettingPageController {
    
    @GetMapping("/live-betting")
    public String liveBetting() {
        return "live-betting";
    }
}
