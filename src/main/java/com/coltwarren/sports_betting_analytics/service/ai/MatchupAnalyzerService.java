package com.coltwarren.sports_betting_analytics.service.ai;

import com.coltwarren.sports_betting_analytics.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class MatchupAnalyzerService {
    
    private final WebClient webClient;
    private final String apiKey;
    private final WeatherService weatherService;
    
    @Autowired
    public MatchupAnalyzerService(@Value("${claude.api.key}") String apiKey,
                                   WeatherService weatherService) {
        this.apiKey = apiKey;
        this.weatherService = weatherService;
        this.webClient = WebClient.builder()
            .baseUrl("https://api.anthropic.com/v1")
            .defaultHeader("x-api-key", apiKey)
            .defaultHeader("anthropic-version", "2023-06-01")
            .defaultHeader("content-type", "application/json")
            .build();
    }
    
    public String analyzeMatchup(String game, String betType, String selection, 
                                 int bestOdds, int worstOdds, double valuePoints) {
        
        // Get live weather data
        Map<String, Object> weather = weatherService.getWeatherForGame(game);
        
        String prompt = buildAnalysisPrompt(game, betType, selection, bestOdds, 
                                           worstOdds, valuePoints, weather);
        
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
            
            return formatAnalysis(response, weather);
            
        } catch (Exception e) {
            return "Error generating analysis: " + e.getMessage();
        }
    }
    
    private String buildAnalysisPrompt(String game, String betType, String selection,
                                      int bestOdds, int worstOdds, double valuePoints,
                                      Map<String, Object> weather) {
        
        StringBuilder weatherInfo = new StringBuilder();
        if ((Boolean) weather.get("available")) {
            weatherInfo.append(String.format("""
                
                LIVE WEATHER DATA:
                Temperature: %d°F (Feels like %d°F)
                Wind: %d mph %s
                Condition: %s
                Humidity: %d%%
                Betting Impact: %s
                """,
                weather.get("temperature"),
                weather.get("feelsLike"),
                weather.get("windSpeed"),
                weather.get("windDirection"),
                weather.get("condition"),
                weather.get("humidity"),
                weather.get("bettingImpact")
            ));
        }
        
        return String.format("""
            You are a professional sports betting analyst. Analyze this betting opportunity:
            
            GAME: %s
            BET TYPE: %s
            SELECTION: %s
            BEST ODDS: %s (best available)
            WORST ODDS: %s (worst available)
            MARKET VALUE: %.0f points (spread between books)
            %s
            
            Provide a detailed matchup analysis in this format:
            
            KEY FACTORS:
            - List 3-5 important factors (injuries, trends, matchups, weather if relevant)
            - Use ✅ for factors favoring the bet
            - Use ⚠️ for concerns
            - If weather data is available, incorporate it into your analysis
            
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
            weatherInfo.toString(),
            formatOdds(bestOdds)
        );
    }
    
    private String formatOdds(int odds) {
        return odds > 0 ? "+" + odds : String.valueOf(odds);
    }
    
    private String formatAnalysis(String analysis, Map<String, Object> weather) {
        if (analysis == null || analysis.isEmpty()) {
            return "Analysis unavailable";
        }
        
        StringBuilder formatted = new StringBuilder();
        
        // Add weather box if available
        if ((Boolean) weather.get("available")) {
            formatted.append(String.format("""
                <div style='background: rgba(59, 130, 246, 0.15); border: 2px solid rgba(59, 130, 246, 0.4); 
                            border-radius: 12px; padding: 15px; margin-bottom: 20px;'>
                    <h3 style='margin: 0 0 10px 0; color: #93c5fd;'>%s LIVE WEATHER - %s</h3>
                    <div style='display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px;'>
                        <div><strong>Temp:</strong> %d°F (feels %d°F)</div>
                        <div><strong>Wind:</strong> %d mph %s</div>
                        <div><strong>Condition:</strong> %s</div>
                        <div><strong>Humidity:</strong> %d%%</div>
                    </div>
                    <div style='margin-top: 10px; padding: 10px; background: rgba(0,0,0,0.2); border-radius: 8px;'>
                        <strong>📊 Impact:</strong> %s
                    </div>
                </div>
                """,
                weather.get("icon"),
                weather.get("city"),
                weather.get("temperature"),
                weather.get("feelsLike"),
                weather.get("windSpeed"),
                weather.get("windDirection"),
                weather.get("condition"),
                weather.get("humidity"),
                weather.get("bettingImpact")
            ));
        }
        
        // Add the main analysis
        formatted.append(analysis
            .replace("KEY FACTORS:", "<h3>🎯 KEY FACTORS:</h3>")
            .replace("TRENDS:", "<h3>📊 TRENDS:</h3>")
            .replace("LINE VALUE ASSESSMENT:", "<h3>💎 LINE VALUE ASSESSMENT:</h3>")
            .replace("CONFIDENCE:", "<h3>📈 CONFIDENCE:</h3>")
            .replace("RECOMMENDATION:", "<h3>💡 RECOMMENDATION:</h3>")
            .replace("\n", "<br>"));
        
        return formatted.toString();
    }
}
