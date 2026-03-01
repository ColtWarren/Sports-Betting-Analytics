package com.coltwarren.sports_betting_analytics.service.ai;

import com.coltwarren.sports_betting_analytics.service.WeatherService;
import com.coltwarren.sports_betting_analytics.service.EspnService;
import com.coltwarren.sports_betting_analytics.service.StatsService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MatchupAnalyzerService {

    private final MatchupPromptBuilder promptBuilder;
    private final ClaudeAIService claudeAIService;
    private final MatchupResponseFormatter responseFormatter;
    private final WeatherService weatherService;
    private final EspnService espnService;
    private final StatsService statsService;

    public MatchupAnalyzerService(MatchupPromptBuilder promptBuilder,
                                   ClaudeAIService claudeAIService,
                                   MatchupResponseFormatter responseFormatter,
                                   WeatherService weatherService,
                                   EspnService espnService,
                                   StatsService statsService) {
        this.promptBuilder = promptBuilder;
        this.claudeAIService = claudeAIService;
        this.responseFormatter = responseFormatter;
        this.weatherService = weatherService;
        this.espnService = espnService;
        this.statsService = statsService;
    }

    public String analyzeMatchup(String game, String betType, String selection,
                                 int bestOdds, int worstOdds, double valuePoints) {

        Map<String, Object> weather = weatherService.getWeatherForGame(game);
        Map<String, Object> injuries = espnService.getInjuriesForGame(game);
        Map<String, Object> stats = getStatsForGame(game);

        String prompt = promptBuilder.buildAnalysisPrompt(game, betType, selection,
                bestOdds, worstOdds, valuePoints, weather, injuries, stats);

        try {
            String response = claudeAIService.callClaudeAPI(prompt, 1500);
            return responseFormatter.formatAnalysis(response, weather, injuries, stats);
        } catch (Exception e) {
            return "Error generating analysis: " + e.getMessage();
        }
    }

    private Map<String, Object> getStatsForGame(String game) {
        try {
            String[] teams = game.split("@|vs");
            if (teams.length == 2) {
                String team1 = teams[0].trim().split(" ")[teams[0].trim().split(" ").length - 1];
                String team2 = teams[1].trim().split(" ")[teams[1].trim().split(" ").length - 1];
                return statsService.getMatchupStats(team1, team2);
            }
        } catch (Exception e) {
            // Return unavailable if parsing fails
        }

        Map<String, Object> unavailable = new HashMap<>();
        unavailable.put("available", false);
        return unavailable;
    }
}
