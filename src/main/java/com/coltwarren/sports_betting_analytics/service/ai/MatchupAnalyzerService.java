package com.coltwarren.sports_betting_analytics.service.ai;

import com.coltwarren.sports_betting_analytics.service.WeatherService;
import com.coltwarren.sports_betting_analytics.service.EspnService;
import com.coltwarren.sports_betting_analytics.service.StatsService;
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
    private final EspnService espnService;
    private final StatsService statsService;
    
    @Autowired
    public MatchupAnalyzerService(@Value("${claude.api.key}") String apiKey,
                                   WeatherService weatherService,
                                   EspnService espnService,
                                   StatsService statsService) {
        this.apiKey = apiKey;
        this.weatherService = weatherService;
        this.espnService = espnService;
        this.statsService = statsService;
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
        
        // Get live injury data
        Map<String, Object> injuries = espnService.getInjuriesForGame(game);
        
        // Get historical stats
        Map<String, Object> stats = getStatsForGame(game);
        
        String prompt = buildAnalysisPrompt(game, betType, selection, bestOdds, 
                                           worstOdds, valuePoints, weather, injuries, stats);
        
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
            
            return formatAnalysis(response, weather, injuries, stats);
            
        } catch (Exception e) {
            return "Error generating analysis: " + e.getMessage();
        }
    }
    
    private Map<String, Object> getStatsForGame(String game) {
        try {
            // Extract team names
            String[] teams = game.split("@|vs");
            if (teams.length == 2) {
                String team1 = teams[0].trim().split(" ")[teams[0].trim().split(" ").length - 1];
                String team2 = teams[1].trim().split(" ")[teams[1].trim().split(" ").length - 1];
                
                return statsService.getMatchupStats(team1, team2);
            }
        } catch (Exception e) {
            // Return unavailable if parsing fails
        }
        
        Map<String, Object> unavailable = new java.util.HashMap<>();
        unavailable.put("available", false);
        return unavailable;
    }
    
    private String buildAnalysisPrompt(String game, String betType, String selection,
                                      int bestOdds, int worstOdds, double valuePoints,
                                      Map<String, Object> weather,
                                      Map<String, Object> injuries,
                                      Map<String, Object> stats) {
        
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
        
        StringBuilder injuryInfo = new StringBuilder();
        if ((Boolean) injuries.get("available")) {
            injuryInfo.append("\nLIVE INJURY REPORT (ESPN):\n");
            injuryInfo.append(String.format("Summary: %s\n", injuries.get("impactSummary")));
            
            List<Map<String, Object>> team1Injuries = (List<Map<String, Object>>) injuries.get("team1Injuries");
            List<Map<String, Object>> team2Injuries = (List<Map<String, Object>>) injuries.get("team2Injuries");
            
            if (!team1Injuries.isEmpty()) {
                injuryInfo.append(String.format("\n%s Injuries:\n", injuries.get("team1")));
                for (Map<String, Object> injury : team1Injuries) {
                    injuryInfo.append(String.format("  %s %s (%s) - %s: %s\n",
                        injury.get("icon"),
                        injury.get("name"),
                        injury.get("position"),
                        injury.get("status"),
                        injury.get("type")
                    ));
                }
            }
            
            if (!team2Injuries.isEmpty()) {
                injuryInfo.append(String.format("\n%s Injuries:\n", injuries.get("team2")));
                for (Map<String, Object> injury : team2Injuries) {
                    injuryInfo.append(String.format("  %s %s (%s) - %s: %s\n",
                        injury.get("icon"),
                        injury.get("name"),
                        injury.get("position"),
                        injury.get("status"),
                        injury.get("type")
                    ));
                }
            }
        }
        
        StringBuilder statsInfo = new StringBuilder();
        if ((Boolean) stats.get("available")) {
            statsInfo.append("\nVERIFIED HISTORICAL STATS:\n");
            statsInfo.append(String.format("Summary: %s\n", stats.get("summary")));
            
            Map<String, Object> team1Stats = (Map<String, Object>) stats.get("team1Stats");
            Map<String, Object> team2Stats = (Map<String, Object>) stats.get("team2Stats");
            
            if ((Boolean) team1Stats.get("available")) {
                statsInfo.append(String.format("\n%s Stats:\n", stats.get("team1")));
                statsInfo.append(String.format("  ATS Record: %s (%.1f%%)\n", 
                    team1Stats.get("atsRecord"), team1Stats.get("atsPercentage")));
                statsInfo.append(String.format("  O/U Record: %s (%.1f%% Over)\n", 
                    team1Stats.get("ouRecord"), team1Stats.get("overPercentage")));
                statsInfo.append(String.format("  Recent ATS: %s\n", team1Stats.get("recentATS")));
            }
            
            if ((Boolean) team2Stats.get("available")) {
                statsInfo.append(String.format("\n%s Stats:\n", stats.get("team2")));
                statsInfo.append(String.format("  ATS Record: %s (%.1f%%)\n", 
                    team2Stats.get("atsRecord"), team2Stats.get("atsPercentage")));
                statsInfo.append(String.format("  O/U Record: %s (%.1f%% Over)\n", 
                    team2Stats.get("ouRecord"), team2Stats.get("overPercentage")));
                statsInfo.append(String.format("  Recent ATS: %s\n", team2Stats.get("recentATS")));
            }
        }
        
        return String.format("""
            You are a professional sports betting analyst. Analyze this betting opportunity:
            
            GAME: %s
            BET TYPE: %s
            SELECTION: %s
            BEST ODDS: %s (best available)
            WORST ODDS: %s (worst available)
            MARKET VALUE: %.0f points (spread between books)
            %s%s%s
            
            Provide a detailed matchup analysis in this format:
            
            KEY FACTORS:
            - List 3-5 important factors (injuries, trends, matchups, weather)
            - Use ✅ for factors favoring the bet
            - Use ⚠️ for concerns
            - Incorporate ALL live data provided above (weather, injuries, verified stats)
            
            TRENDS:
            - Use the VERIFIED STATS provided above (don't guess!)
            - If stats unavailable, note that data is limited
            
            LINE VALUE ASSESSMENT:
            - Is this line value strong, fair, or weak?
            - How does %s odds compare to market?
            
            CONFIDENCE: [HIGH/MEDIUM-HIGH/MEDIUM/MEDIUM-LOW/LOW]
            
            RECOMMENDATION:
            - 2-3 sentences summarizing your analysis
            - Should this bet be placed based on the value and factors?
            - What's the main risk?
            
            Keep it concise, actionable, and data-focused. Only use verified statistics provided above.
            """, 
            game, betType, selection, 
            formatOdds(bestOdds), formatOdds(worstOdds), valuePoints,
            weatherInfo.toString(),
            injuryInfo.toString(),
            statsInfo.toString(),
            formatOdds(bestOdds)
        );
    }
    
    private String formatOdds(int odds) {
        return odds > 0 ? "+" + odds : String.valueOf(odds);
    }
    
    private String formatAnalysis(String analysis, Map<String, Object> weather, 
                                  Map<String, Object> injuries, Map<String, Object> stats) {
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
        
        // Add injury box if available
        if ((Boolean) injuries.get("available")) {
            List<Map<String, Object>> team1Injuries = (List<Map<String, Object>>) injuries.get("team1Injuries");
            List<Map<String, Object>> team2Injuries = (List<Map<String, Object>>) injuries.get("team2Injuries");
            
            StringBuilder injuryHtml = new StringBuilder();
            injuryHtml.append(String.format("""
                <div style='background: rgba(239, 68, 68, 0.15); border: 2px solid rgba(239, 68, 68, 0.4); 
                            border-radius: 12px; padding: 15px; margin-bottom: 20px;'>
                    <h3 style='margin: 0 0 10px 0; color: #fca5a5;'>🏥 LIVE INJURY REPORT (ESPN)</h3>
                    <div style='padding: 10px; background: rgba(0,0,0,0.2); border-radius: 8px; margin-bottom: 10px;'>
                        <strong>Summary:</strong> %s
                    </div>
                """, injuries.get("impactSummary")));
            
            if (!team1Injuries.isEmpty() || !team2Injuries.isEmpty()) {
                injuryHtml.append("<div style='display: grid; grid-template-columns: 1fr 1fr; gap: 15px;'>");
                
                // Team 1 injuries
                injuryHtml.append(String.format("<div><strong>%s:</strong><br>", injuries.get("team1")));
                if (team1Injuries.isEmpty()) {
                    injuryHtml.append("<span style='color: #6ee7b7;'>✅ No major injuries</span>");
                } else {
                    for (Map<String, Object> injury : team1Injuries) {
                        injuryHtml.append(String.format("%s %s (%s) - <em>%s</em><br>",
                            injury.get("icon"),
                            injury.get("name"),
                            injury.get("position"),
                            injury.get("status")
                        ));
                    }
                }
                injuryHtml.append("</div>");
                
                // Team 2 injuries
                injuryHtml.append(String.format("<div><strong>%s:</strong><br>", injuries.get("team2")));
                if (team2Injuries.isEmpty()) {
                    injuryHtml.append("<span style='color: #6ee7b7;'>✅ No major injuries</span>");
                } else {
                    for (Map<String, Object> injury : team2Injuries) {
                        injuryHtml.append(String.format("%s %s (%s) - <em>%s</em><br>",
                            injury.get("icon"),
                            injury.get("name"),
                            injury.get("position"),
                            injury.get("status")
                        ));
                    }
                }
                injuryHtml.append("</div>");
                
                injuryHtml.append("</div>");
            }
            
            injuryHtml.append("</div>");
            formatted.append(injuryHtml);
        }
        
        // Add stats box if available
        if ((Boolean) stats.get("available")) {
            Map<String, Object> team1Stats = (Map<String, Object>) stats.get("team1Stats");
            Map<String, Object> team2Stats = (Map<String, Object>) stats.get("team2Stats");
            
            formatted.append(String.format("""
                <div style='background: rgba(34, 197, 94, 0.15); border: 2px solid rgba(34, 197, 94, 0.4); 
                            border-radius: 12px; padding: 15px; margin-bottom: 20px;'>
                    <h3 style='margin: 0 0 10px 0; color: #86efac;'>📊 VERIFIED STATS (YOUR DATABASE)</h3>
                    <div style='padding: 10px; background: rgba(0,0,0,0.2); border-radius: 8px; margin-bottom: 10px;'>
                        <strong>Summary:</strong> %s
                    </div>
                    <div style='display: grid; grid-template-columns: 1fr 1fr; gap: 15px;'>
                        <div>
                            <strong>%s:</strong><br>
                            ATS: %s (%.1f%%)<br>
                            O/U: %s<br>
                            Recent: %s
                        </div>
                        <div>
                            <strong>%s:</strong><br>
                            ATS: %s (%.1f%%)<br>
                            O/U: %s<br>
                            Recent: %s
                        </div>
                    </div>
                </div>
                """,
                stats.get("summary"),
                stats.get("team1"),
                team1Stats.get("atsRecord"),
                team1Stats.get("atsPercentage"),
                team1Stats.get("ouRecord"),
                team1Stats.get("recentATS"),
                stats.get("team2"),
                team2Stats.get("atsRecord"),
                team2Stats.get("atsPercentage"),
                team2Stats.get("ouRecord"),
                team2Stats.get("recentATS")
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
