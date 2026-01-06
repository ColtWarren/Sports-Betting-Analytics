package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.BetService;
import com.coltwarren.sports_betting_analytics.service.ai.ClaudeAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/ai")
public class AIController {
    
    private final ClaudeAIService claudeAIService;
    private final BetService betService;
    
    @Autowired
    public AIController(ClaudeAIService claudeAIService, BetService betService) {
        this.claudeAIService = claudeAIService;
        this.betService = betService;
    }
    
    @GetMapping("/analyze-performance")
    public String analyzePerformance(Model model) {
        // Get current stats
        long totalBets = betService.getAllBets().size();
        long wonCount = betService.countBetsByStatus("WON");
        long lostCount = betService.countBetsByStatus("LOST");
        BigDecimal profitLoss = betService.calculateTotalProfitLoss();
        Double winRate = betService.calculateWinRate();
        Double roi = betService.calculateROI();
        
        // Get AI analysis
        String analysis = claudeAIService.analyzeBettingPerformance(
            totalBets, wonCount, lostCount, profitLoss, winRate, roi
        );
        
        model.addAttribute("analysis", analysis);
        model.addAttribute("totalBets", totalBets);
        model.addAttribute("wonCount", wonCount);
        model.addAttribute("lostCount", lostCount);
        model.addAttribute("profitLoss", profitLoss);
        model.addAttribute("winRate", winRate);
        model.addAttribute("roi", roi);
        
        return "ai-analysis";
    }
    
    @PostMapping("/calculate-ev")
    @ResponseBody
    public String calculateEV(
            @RequestParam String sport,
            @RequestParam String eventName,
            @RequestParam String betType,
            @RequestParam String selection,
            @RequestParam BigDecimal odds,
            @RequestParam BigDecimal stake) {
        
        return claudeAIService.calculateEV(sport, eventName, betType, selection, odds, stake);
    }
}
