package com.coltwarren.sports_betting_analytics.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class MatchupAnalyzerService {
    
    private final WebClient webClient;
    private final String apiKey;
    
    public MatchupAnalyzerService(@Value("${claude.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
            .baseUrl("https://api.anthropic.com/v1")
            .defaultHeader("x-api-key", apiKey)
            .defaultHeader("anthropic-version", "2023-06-01")
            .defaultHeader("content-type", "application/json")
            .build();
    }
    
    public String analyzeMatchup(String game, String betType, String selection, 
                                 int bestOdds, int worstOdds, double valuePoints) {
        
        String prompt = buildAnalysisPrompt(game, betType, selection, bestOdds, worstOdds, valuePoints);
        
        try {
            String response = webClient.post()
                .uri("/messages")
                .bodyValue(Map.of(
                    "model", "claude-sonnet-4-20250514",
                    "max_tokens", 1500,
                    "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                    )
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .map(responseBody -> {
                    List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
                    if (content != null && !content.isEmpty()) {
                        return (String) content.get(0).get("text");
                    }
                    return "Analysis unavailable";
                })
                .block();
            
            return formatAnalysis(response);
            
        } catch (Exception e) {
            return "Error generating analysis: " + e.getMessage();
        }
    }
    
    private String buildAnalysisPrompt(String game, String betType, String selection,
                                      int bestOdds, int worstOdds, double valuePoints) {
        return String.format("""
            You are a professional sports betting analyst. Analyze this betting opportunity:
            
            GAME: %s
            BET TYPE: %s
            SELECTION: %s
            BEST ODDS: %s (best available)
            WORST ODDS: %s (worst available)
            MARKET VALUE: %.0f points (spread between books)
            
            Provide a detailed matchup analysis in this format:
            
            KEY FACTORS:
            - List 3-5 important factors (injuries, trends, matchups, weather if relevant)
            - Use ✅ for factors favoring the bet
            - Use ⚠️ for concerns
            
            TRENDS:
            - Relevant historical trends
            - Recent performance patterns
            - Head-to-head history if applicable
            
            LINE VALUE ASSESSMENT:
            - Is this line value strong, fair, or weak?
            - How does %s odds compare to market?
            
            CONFIDENCE: [HIGH/MEDIUM-HIGH/MEDIUM/MEDIUM-LOW/LOW]
            
            RECOMMENDATION:
            - 2-3 sentences summarizing your analysis
            - Should this bet be placed based on the value and factors?
            - What's the main risk?
            
            Keep it concise, actionable, and data-focused.
            """, 
            game, betType, selection, 
            formatOdds(bestOdds), formatOdds(worstOdds), valuePoints,
            formatOdds(bestOdds)
        );
    }
    
    private String formatOdds(int odds) {
        return odds > 0 ? "+" + odds : String.valueOf(odds);
    }
    
    private String formatAnalysis(String analysis) {
        if (analysis == null || analysis.isEmpty()) {
            return "Analysis unavailable";
        }
        
        // Convert to HTML with proper formatting
        return analysis
            .replace("KEY FACTORS:", "<h3>🎯 KEY FACTORS:</h3>")
            .replace("TRENDS:", "<h3>📊 TRENDS:</h3>")
            .replace("LINE VALUE ASSESSMENT:", "<h3>💎 LINE VALUE ASSESSMENT:</h3>")
            .replace("CONFIDENCE:", "<h3>📈 CONFIDENCE:</h3>")
            .replace("RECOMMENDATION:", "<h3>💡 RECOMMENDATION:</h3>")
            .replace("\n", "<br>");
    }
}
