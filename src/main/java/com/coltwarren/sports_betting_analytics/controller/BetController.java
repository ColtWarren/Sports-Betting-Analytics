package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.model.Bet;
import com.coltwarren.sports_betting_analytics.service.BetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bets")
@CrossOrigin(origins = "*")
public class BetController {
    
    private final BetService betService;
    
    @Autowired
    public BetController(BetService betService) {
        this.betService = betService;
    }
    
    @PostMapping
    public ResponseEntity<Bet> createBet(@RequestBody Bet bet) {
        try {
            Bet createdBet = betService.createBet(bet);
            return ResponseEntity.status(201).body(createdBet);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Bet>> getAllBets() {
        return ResponseEntity.ok(betService.getAllBets());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Bet> getBetById(@PathVariable Long id) {
        return betService.getBetById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<Bet>> getPendingBets() {
        return ResponseEntity.ok(betService.getPendingBets());
    }
    
    @GetMapping("/settled")
    public ResponseEntity<List<Bet>> getSettledBets() {
        return ResponseEntity.ok(betService.getSettledBets());
    }
    
    @GetMapping("/sportsbook/{name}")
    public ResponseEntity<List<Bet>> getBetsBySportsbook(@PathVariable String name) {
        return ResponseEntity.ok(betService.getBetsBySportsbook(name));
    }
    
    @GetMapping("/sport/{sport}")
    public ResponseEntity<List<Bet>> getBetsBySport(@PathVariable String sport) {
        return ResponseEntity.ok(betService.getBetsBySport(sport));
    }
    
    @PutMapping("/{id}/won")
    public ResponseEntity<Bet> markBetAsWon(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(betService.markBetAsWon(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}/lost")
    public ResponseEntity<Bet> markBetAsLost(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(betService.markBetAsLost(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}/push")
    public ResponseEntity<Bet> markBetAsPush(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(betService.markBetAsPush(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBet(@PathVariable Long id) {
        try {
            betService.deleteBet(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/analytics/profit-loss")
    public ResponseEntity<Map<String, BigDecimal>> getTotalProfitLoss() {
        return ResponseEntity.ok(Map.of("totalProfitLoss", betService.calculateTotalProfitLoss()));
    }
    
    @GetMapping("/analytics/win-rate")
    public ResponseEntity<Map<String, Double>> getWinRate() {
        return ResponseEntity.ok(Map.of("winRate", betService.calculateWinRate()));
    }
    
    @GetMapping("/analytics/roi")
    public ResponseEntity<Map<String, Double>> getROI() {
        return ResponseEntity.ok(Map.of("roi", betService.calculateROI()));
    }
    
    @GetMapping("/analytics/counts")
    public ResponseEntity<Map<String, Long>> getCountsByStatus() {
        Map<String, Long> counts = Map.of(
            "total", (long) betService.getAllBets().size(),
            "pending", betService.countBetsByStatus("PENDING"),
            "won", betService.countBetsByStatus("WON"),
            "lost", betService.countBetsByStatus("LOST"),
            "push", betService.countBetsByStatus("PUSH")
        );
        return ResponseEntity.ok(counts);
    }
}
