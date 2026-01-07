package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.Bet;
import com.coltwarren.sports_betting_analytics.repository.BetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CLVTracker {
    
    private final BetService betService;
    private final BetRepository betRepository;
    
    @Autowired
    public CLVTracker(BetService betService, BetRepository betRepository) {
        this.betService = betService;
        this.betRepository = betRepository;
    }
    
    /**
     * Get comprehensive CLV statistics
     */
    public Map<String, Object> getCLVStats() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Bet> settledBets = betService.getSettledBets();
        
        // Filter bets with closing odds
        List<Bet> betsWithClosingOdds = settledBets.stream()
            .filter(bet -> bet.getClosingOdds() != null)
            .toList();
        
        if (betsWithClosingOdds.isEmpty()) {
            stats.put("hasCLVData", false);
            stats.put("message", "No bets with closing line data yet. Add closing odds to your settled bets!");
            return stats;
        }
        
        // Calculate stats
        long totalBetsWithCLV = betsWithClosingOdds.size();
        long beatClosingLine = betsWithClosingOdds.stream()
            .filter(bet -> bet.getBeatClosingLine() != null && bet.getBeatClosingLine())
            .count();
        
        double clvWinRate = totalBetsWithCLV > 0 ? 
            (double) beatClosingLine / totalBetsWithCLV * 100 : 0;
        
        // Calculate average CLV
        double avgCLV = betsWithClosingOdds.stream()
            .map(Bet::calculateCLV)
            .filter(clv -> clv != null)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        // Calculate CLV for winning bets
        double avgCLVWinners = betsWithClosingOdds.stream()
            .filter(bet -> "WON".equals(bet.getStatus()))
            .map(Bet::calculateCLV)
            .filter(clv -> clv != null)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        // Calculate CLV for losing bets
        double avgCLVLosers = betsWithClosingOdds.stream()
            .filter(bet -> "LOST".equals(bet.getStatus()))
            .map(Bet::calculateCLV)
            .filter(clv -> clv != null)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        // Best and worst CLV
        double bestCLV = betsWithClosingOdds.stream()
            .map(Bet::calculateCLV)
            .filter(clv -> clv != null)
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(0.0);
        
        double worstCLV = betsWithClosingOdds.stream()
            .map(Bet::calculateCLV)
            .filter(clv -> clv != null)
            .mapToDouble(Double::doubleValue)
            .min()
            .orElse(0.0);
        
        stats.put("hasCLVData", true);
        stats.put("totalBetsWithCLV", totalBetsWithCLV);
        stats.put("beatClosingLineCount", beatClosingLine);
        stats.put("clvWinRate", clvWinRate);
        stats.put("avgCLV", avgCLV);
        stats.put("avgCLVWinners", avgCLVWinners);
        stats.put("avgCLVLosers", avgCLVLosers);
        stats.put("bestCLV", bestCLV);
        stats.put("worstCLV", worstCLV);
        stats.put("interpretation", interpretCLV(avgCLV, clvWinRate));
        
        return stats;
    }
    
    /**
     * Update closing odds for a bet
     */
    public Bet updateClosingOdds(Long betId, Integer closingOdds) {
        Bet bet = betRepository.findById(betId)
            .orElseThrow(() -> new RuntimeException("Bet not found"));
        bet.setClosingOdds(BigDecimal.valueOf(closingOdds));
        bet.checkBeatClosingLine();
        return betRepository.save(bet);
    }
    
    private String interpretCLV(double avgCLV, double clvWinRate) {
        if (avgCLV > 3 && clvWinRate > 60) {
            return "🔥 ELITE - You consistently beat the closing line with strong CLV!";
        } else if (avgCLV > 1 && clvWinRate > 55) {
            return "✅ SOLID - Positive CLV indicates real skill and edge.";
        } else if (avgCLV > 0 && clvWinRate > 50) {
            return "⚠️ DEVELOPING - Slight positive CLV. Keep refining your process.";
        } else if (avgCLV < 0) {
            return "❌ CONCERNING - Negative CLV suggests betting into bad numbers.";
        } else {
            return "📊 NEUTRAL - Break even CLV. Look for better entry points.";
        }
    }
}
