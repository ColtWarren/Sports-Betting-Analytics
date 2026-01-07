package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.CLVTracker;
import com.coltwarren.sports_betting_analytics.model.Bet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class CLVController {
    
    private final CLVTracker clvTracker;
    
    @Autowired
    public CLVController(CLVTracker clvTracker) {
        this.clvTracker = clvTracker;
    }
    
    @GetMapping("/clv")
    public String clvPage(Model model) {
        Map<String, Object> stats = clvTracker.getCLVStats();
        model.addAttribute("stats", stats);
        return "clv";
    }
    
    @PostMapping("/clv/update/{betId}")
    public String updateClosingOdds(@PathVariable Long betId, @RequestParam Integer closingOdds) {
        clvTracker.updateClosingOdds(betId, closingOdds);
        return "redirect:/clv";
    }
    
    @GetMapping("/api/clv/stats")
    @ResponseBody
    public Map<String, Object> getCLVStats() {
        return clvTracker.getCLVStats();
    }
}
