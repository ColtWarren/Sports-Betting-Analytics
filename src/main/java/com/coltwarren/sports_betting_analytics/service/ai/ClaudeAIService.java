package com.coltwarren.sports_betting_analytics.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeAIService {
    
    private final WebClient webClient;
    private final String model;
    
    public ClaudeAIService(
            @Value("${claude.api.url}") String apiUrl,
            @Value("${claude.api.key}") String apiKey,
            @Value("${claude.model}") String model) {
        this.model = model;
        this.webClient = WebClient.builder()
            .baseUrl(apiUrl)
            .defaultHeader("x-api-key", apiKey)
            .defaultHeader("anthropic-version", "2023-06-01")
            .defaultHeader("content-type", "application/json")
            .build();
    }
    
    public String calculateEV(String sport, String eventName, String betType, 
                             String selection, BigDecimal yourOdds, BigDecimal stake) {
        
        String prompt = String.format("""
            You are a professional sports betting analyst. Calculate the Expected Value (EV) for this bet.
            
            BET DETAILS:
            - Sport: %s
            - Event: %s
            - Bet Type: %s
            - Selection: %s
            - Your Odds: %s (American format)
            - Stake: $%s
            
            ANALYSIS REQUIRED:
            1. Convert American odds to implied probability
            2. Estimate the TRUE probability of this outcome
            3. Calculate Expected Value
            4. Determine if this is +EV
            
            PROVIDE (keep it brief):
            - Implied Probability: X%%
            - Estimated True Probability: X%%
            - Expected Value: $X.XX
            - Recommendation: TAKE IT or SKIP IT
            - Reasoning: 2-3 sentences
            """,
            sport, eventName, betType, selection, yourOdds, stake);
        
        return callClaudeAPI(prompt);
    }
    
    public String analyzeClosingLineValue(BigDecimal yourOdds, BigDecimal closingOdds) {
        String prompt = String.format("""
            Analyze the closing line value (CLV) for this bet.
            
            YOUR ODDS: %s
            CLOSING ODDS: %s
            
            Briefly explain:
            1. Did this beat the closing line?
            2. What does this indicate?
            3. Is this a good sign?
            """,
            yourOdds, closingOdds);
        
        return callClaudeAPI(prompt);
    }
    
    public String analyzeBettingPerformance(long totalBets, long wonCount, long lostCount,
                                           BigDecimal profitLoss, Double winRate, Double roi) {
        
        String prompt = String.format("""
            Analyze this sports bettor's performance.
            
            STATS:
            - Total Bets: %d
            - Won: %d
            - Lost: %d
            - Total Profit/Loss: $%s
            - Win Rate: %.1f%%
            - ROI: %.1f%%
            
            PROVIDE:
            1. Performance Assessment
            2. Key Strengths
            3. Areas for Improvement
            4. 2-3 Actionable Tips
            
            Keep it encouraging but honest.
            """,
            totalBets, wonCount, lostCount, profitLoss, winRate, roi);
        
        return callClaudeAPI(prompt);
    }
    
    private String callClaudeAPI(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 1024,
                "messages", List.of(
                    Map.of(
                        "role", "user",
                        "content", prompt
                    )
                )
            );
            
            Mono<Map> response = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class);
            
            Map<String, Object> result = response.block();
            
            if (result != null && result.containsKey("content")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
                if (!content.isEmpty()) {
                    return (String) content.get(0).get("text");
                }
            }
            
            return "Unable to get AI response. Please try again.";
            
        } catch (Exception e) {
            return "Error calling Claude AI: " + e.getMessage();
        }
    }
}
