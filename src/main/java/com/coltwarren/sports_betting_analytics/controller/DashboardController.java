package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.model.Bet;
import com.coltwarren.sports_betting_analytics.service.BetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class DashboardController {
    
    private final BetService betService;
    
    @Autowired
    public DashboardController(BetService betService) {
        this.betService = betService;
    }
    
    @GetMapping("/")
    public String home(Model model) {
        // Get all bets
        List<Bet> allBets = betService.getAllBets();
        List<Bet> pendingBets = betService.getPendingBets();
        List<Bet> settledBets = betService.getSettledBets();
        
        // Get analytics
        BigDecimal totalProfitLoss = betService.calculateTotalProfitLoss();
        Double winRate = betService.calculateWinRate();
        Double roi = betService.calculateROI();
        
        // Get counts
        long totalCount = allBets.size();
        long pendingCount = betService.countBetsByStatus("PENDING");
        long wonCount = betService.countBetsByStatus("WON");
        long lostCount = betService.countBetsByStatus("LOST");
        long pushCount = betService.countBetsByStatus("PUSH");
        
        // Add to model
        model.addAttribute("allBets", allBets);
        model.addAttribute("pendingBets", pendingBets);
        model.addAttribute("settledBets", settledBets);
        model.addAttribute("totalProfitLoss", totalProfitLoss);
        model.addAttribute("winRate", winRate);
        model.addAttribute("roi", roi);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("wonCount", wonCount);
        model.addAttribute("lostCount", lostCount);
        model.addAttribute("pushCount", pushCount);
        
        return "dashboard";
    }
    
    @GetMapping("/bets/new")
    public String newBetForm(Model model) {
        model.addAttribute("bet", new Bet());
        return "bet-form";
    }
    
    @PostMapping("/bets/create")
    public String createBet(@ModelAttribute Bet bet) {
        betService.createBet(bet);
        return "redirect:/";
    }
    
    @PostMapping("/bets/{id}/won")
    public String markAsWon(@PathVariable Long id) {
        betService.markBetAsWon(id);
        return "redirect:/";
    }
    
    @PostMapping("/bets/{id}/lost")
    public String markAsLost(@PathVariable Long id) {
        betService.markBetAsLost(id);
        return "redirect:/";
    }
    
    @PostMapping("/bets/{id}/push")
    public String markAsPush(@PathVariable Long id) {
        betService.markBetAsPush(id);
        return "redirect:/";
    }
    
    @PostMapping("/bets/{id}/delete")
    public String deleteBet(@PathVariable Long id) {
        betService.deleteBet(id);
        return "redirect:/";
    }
}
