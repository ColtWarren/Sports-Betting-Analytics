package com.coltwarren.sports_betting_analytics.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class KellyCriterionService {
    
    private final BankrollService bankrollService;
    
    @Autowired
    public KellyCriterionService(BankrollService bankrollService) {
        this.bankrollService = bankrollService;
    }
    
    /**
     * Calculate Kelly Criterion optimal bet size
     * Formula: Kelly % = (bp - q) / b
     * Where:
     * - b = decimal odds (e.g., +150 = 1.5, -110 = 0.909)
     * - p = probability of winning
     * - q = probability of losing (1 - p)
     */
    public Map<String, Object> calculateKelly(int americanOdds, double winProbability, boolean fractional) {
        Map<String, Object> result = new HashMap<>();
        
        // Convert American odds to decimal odds
        double decimalOdds = americanToDecimal(americanOdds);
        
        // Calculate probabilities
        double p = winProbability;
        double q = 1 - winProbability;
        
        // Calculate Kelly percentage
        double kellyPercentage = ((decimalOdds * p) - q) / decimalOdds;
        
        // Apply fractional Kelly if requested (typically 0.25 to 0.5 for safety)
        if (fractional) {
            kellyPercentage = kellyPercentage * 0.25; // Quarter Kelly
        }
        
        // Ensure non-negative
        if (kellyPercentage < 0) {
            kellyPercentage = 0;
        }
        
        // Get current bankroll
        BigDecimal currentBankroll = bankrollService.getCurrentBankroll();
        
        // Calculate recommended stake
        BigDecimal recommendedStake = currentBankroll
            .multiply(BigDecimal.valueOf(kellyPercentage))
            .setScale(2, RoundingMode.HALF_UP);
        
        // Calculate expected value
        double ev = (decimalOdds * p) - 1;
        
        result.put("kellyPercentage", kellyPercentage * 100);
        result.put("recommendedStake", recommendedStake);
        result.put("currentBankroll", currentBankroll);
        result.put("expectedValue", ev * 100);
        result.put("decimalOdds", decimalOdds);
        result.put("winProbability", winProbability * 100);
        result.put("fractional", fractional);
        
        return result;
    }
    
    /**
     * Calculate Full Kelly
     */
    public Map<String, Object> calculateFullKelly(int americanOdds, double winProbability) {
        return calculateKelly(americanOdds, winProbability, false);
    }
    
    /**
     * Calculate Quarter Kelly (safer, recommended for most bettors)
     */
    public Map<String, Object> calculateQuarterKelly(int americanOdds, double winProbability) {
        return calculateKelly(americanOdds, winProbability, true);
    }
    
    /**
     * Convert American odds to decimal odds
     */
    private double americanToDecimal(int americanOdds) {
        if (americanOdds > 0) {
            // Positive odds: +150 = 2.5
            return (americanOdds / 100.0) + 1;
        } else {
            // Negative odds: -110 = 1.909
            return (100.0 / Math.abs(americanOdds)) + 1;
        }
    }
    
    /**
     * Calculate implied probability from odds
     */
    public double calculateImpliedProbability(int americanOdds) {
        if (americanOdds > 0) {
            return 100.0 / (americanOdds + 100);
        } else {
            return Math.abs(americanOdds) / (Math.abs(americanOdds) + 100.0);
        }
    }
    
    /**
     * Determine if bet has positive expected value
     */
    public boolean hasPositiveEV(int americanOdds, double winProbability) {
        double decimalOdds = americanToDecimal(americanOdds);
        double ev = (decimalOdds * winProbability) - 1;
        return ev > 0;
    }
}
