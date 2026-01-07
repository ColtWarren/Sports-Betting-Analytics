package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.service.ai.ClaudeAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdvancedEVCalculator {
    
    private final ClaudeAIService claudeAIService;
    private final KellyCriterionService kellyService;
    
    @Autowired
    public AdvancedEVCalculator(ClaudeAIService claudeAIService, KellyCriterionService kellyService) {
        this.claudeAIService = claudeAIService;
        this.kellyService = kellyService;
    }
    
    /**
     * Calculate comprehensive EV analysis with Claude AI
     */
    public Map<String, Object> analyzeEV(String sport, String event, String selection, 
                                         int odds, String betType, String context) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Ask Claude AI to estimate win probability
            String prompt = buildEVPrompt(sport, event, selection, odds, betType, context);
            String aiResponse = claudeAIService.callClaudeAPI(prompt);
            
            // Extract probability from AI response (looking for percentage)
            double estimatedProbability = extractProbability(aiResponse);
            
            // Calculate EV
            double decimalOdds = americanToDecimal(odds);
            double ev = (decimalOdds * estimatedProbability) - 1;
            double evPercentage = ev * 100;
            
            // Calculate Kelly recommendation
            Map<String, Object> kellyData = kellyService.calculateQuarterKelly(odds, estimatedProbability);
            
            // Determine if bet is +EV
            boolean isPositiveEV = ev > 0;
            
            // Calculate implied probability from odds
            double impliedProb = kellyService.calculateImpliedProbability(odds);
            
            // Calculate edge (difference between true prob and implied prob)
            double edge = (estimatedProbability - impliedProb) * 100;
            
            result.put("sport", sport);
            result.put("event", event);
            result.put("selection", selection);
            result.put("odds", odds);
            result.put("betType", betType);
            result.put("estimatedWinProbability", estimatedProbability * 100);
            result.put("impliedProbability", impliedProb * 100);
            result.put("edge", edge);
            result.put("expectedValue", evPercentage);
            result.put("isPositiveEV", isPositiveEV);
            result.put("kellyRecommendation", kellyData.get("recommendedStake"));
            result.put("kellyPercentage", kellyData.get("kellyPercentage"));
            result.put("aiAnalysis", aiResponse);
            result.put("recommendation", generateRecommendation(isPositiveEV, evPercentage, edge));
            
        } catch (Exception e) {
            result.put("error", "Failed to analyze EV: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Simple EV calculation without AI
     */
    public Map<String, Object> calculateSimpleEV(int odds, double winProbability) {
        Map<String, Object> result = new HashMap<>();
        
        double decimalOdds = americanToDecimal(odds);
        double ev = (decimalOdds * winProbability) - 1;
        double evPercentage = ev * 100;
        
        double impliedProb = kellyService.calculateImpliedProbability(odds);
        double edge = (winProbability - impliedProb) * 100;
        
        boolean isPositiveEV = ev > 0;
        
        Map<String, Object> kellyData = kellyService.calculateQuarterKelly(odds, winProbability);
        
        result.put("odds", odds);
        result.put("winProbability", winProbability * 100);
        result.put("impliedProbability", impliedProb * 100);
        result.put("edge", edge);
        result.put("expectedValue", evPercentage);
        result.put("isPositiveEV", isPositiveEV);
        result.put("kellyRecommendation", kellyData.get("recommendedStake"));
        result.put("kellyPercentage", kellyData.get("kellyPercentage"));
        result.put("recommendation", generateRecommendation(isPositiveEV, evPercentage, edge));
        
        return result;
    }
    
    private String buildEVPrompt(String sport, String event, String selection, 
                                  int odds, String betType, String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional sports betting analyst. ");
        prompt.append("Analyze this bet and estimate the TRUE win probability.\n\n");
        prompt.append("Sport: ").append(sport).append("\n");
        prompt.append("Event: ").append(event).append("\n");
        prompt.append("Bet Type: ").append(betType).append("\n");
        prompt.append("Selection: ").append(selection).append("\n");
        prompt.append("Odds: ").append(odds).append("\n");
        if (context != null && !context.isEmpty()) {
            prompt.append("Additional Context: ").append(context).append("\n");
        }
        prompt.append("\nProvide:\n");
        prompt.append("1. Your estimated win probability (as a percentage)\n");
        prompt.append("2. Key factors influencing this probability\n");
        prompt.append("3. Confidence level in this estimate\n\n");
        prompt.append("Format your response starting with: 'ESTIMATED PROBABILITY: XX%'");
        
        return prompt.toString();
    }
    
    private double extractProbability(String aiResponse) {
        // Look for percentage in AI response
        String[] lines = aiResponse.split("\n");
        for (String line : lines) {
            if (line.toUpperCase().contains("ESTIMATED PROBABILITY:")) {
                String probStr = line.replaceAll("[^0-9.]", "");
                try {
                    return Double.parseDouble(probStr) / 100.0;
                } catch (NumberFormatException e) {
                    // Continue searching
                }
            }
        }
        
        // If not found in header, look for any percentage
        String cleaned = aiResponse.replaceAll("[^0-9.%]", " ");
        String[] tokens = cleaned.split("\\s+");
        for (String token : tokens) {
            if (token.contains("%") || token.matches("\\d+\\.?\\d*")) {
                try {
                    double prob = Double.parseDouble(token.replace("%", ""));
                    if (prob > 0 && prob <= 100) {
                        return prob / 100.0;
                    }
                } catch (NumberFormatException e) {
                    // Continue
                }
            }
        }
        
        // Default to 50% if can't extract
        return 0.50;
    }
    
    private double americanToDecimal(int americanOdds) {
        if (americanOdds > 0) {
            return (americanOdds / 100.0) + 1;
        } else {
            return (100.0 / Math.abs(americanOdds)) + 1;
        }
    }
    
    private String generateRecommendation(boolean isPositiveEV, double evPercentage, double edge) {
        if (!isPositiveEV) {
            return "❌ SKIP - Negative EV bet. No edge detected.";
        } else if (evPercentage > 10 && edge > 5) {
            return "🔥 STRONG BET - Significant +EV with clear edge!";
        } else if (evPercentage > 5) {
            return "✅ GOOD BET - Positive EV detected. Consider betting.";
        } else if (evPercentage > 0) {
            return "⚠️ MARGINAL - Slight +EV but small edge. Proceed with caution.";
        } else {
            return "❌ SKIP - Not worth the risk.";
        }
    }
}
