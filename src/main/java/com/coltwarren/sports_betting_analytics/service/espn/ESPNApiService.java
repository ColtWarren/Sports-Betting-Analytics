package com.coltwarren.sports_betting_analytics.service.espn;

import com.coltwarren.sports_betting_analytics.model.BetStatus;
import com.coltwarren.sports_betting_analytics.model.BetType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class ESPNApiService {
    
    private final WebClient webClient;
    
    public ESPNApiService(@Qualifier("espnWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
    
    /**
     * Get game result from ESPN
     */
    public Map<String, Object> getGameResult(String sport, String homeTeam, String awayTeam) {
        String sportPath = mapSportToESPN(sport);
        
        try {
            String response = webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/{sport}/scoreboard")
                    .build(sportPath))
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            return parseGameResult(response, homeTeam, awayTeam);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch game result: " + e.getMessage());
            return error;
        }
    }
    
    /**
     * Check if bet won based on game result
     */
    public BetStatus determineBetOutcome(Map<String, Object> gameResult, String selection,
                                     BetType betType, Integer odds, Double line) {

        if (gameResult.containsKey("error")) {
            return BetStatus.PENDING;
        }

        String status = (String) gameResult.get("status");
        if (!"FINAL".equals(status)) {
            return BetStatus.PENDING;
        }

        int homeScore = (Integer) gameResult.get("homeScore");
        int awayScore = (Integer) gameResult.get("awayScore");
        String homeTeam = (String) gameResult.get("homeTeam");
        String awayTeam = (String) gameResult.get("awayTeam");

        boolean selectedHome = selection.toUpperCase().contains(homeTeam.toUpperCase());
        boolean selectedAway = selection.toUpperCase().contains(awayTeam.toUpperCase());

        if (!selectedHome && !selectedAway) {
            return BetStatus.PENDING;
        }

        if (betType == BetType.MONEYLINE) {
            if (homeScore == awayScore) return BetStatus.PUSH;

            boolean homeWon = homeScore > awayScore;
            if (selectedHome) {
                return homeWon ? BetStatus.WON : BetStatus.LOST;
            } else {
                return homeWon ? BetStatus.LOST : BetStatus.WON;
            }

        } else if (betType == BetType.SPREAD && line != null) {
            double adjustedScore;
            double opponentScore;

            if (selectedHome) {
                adjustedScore = homeScore + line;
                opponentScore = awayScore;
            } else {
                adjustedScore = awayScore + line;
                opponentScore = homeScore;
            }

            if (adjustedScore == opponentScore) return BetStatus.PUSH;
            return adjustedScore > opponentScore ? BetStatus.WON : BetStatus.LOST;

        } else if (betType == BetType.TOTAL_OVER || betType == BetType.TOTAL_UNDER || betType == BetType.OVER_GOALS || betType == BetType.UNDER_GOALS) {
            if (line == null) return BetStatus.PENDING;

            int totalPoints = homeScore + awayScore;

            if (totalPoints == line) return BetStatus.PUSH;

            boolean isOver = betType == BetType.TOTAL_OVER || betType == BetType.OVER_GOALS;
            if (isOver) {
                return totalPoints > line ? BetStatus.WON : BetStatus.LOST;
            } else {
                return totalPoints < line ? BetStatus.WON : BetStatus.LOST;
            }
        }

        return BetStatus.PENDING;
    }
    
    private String mapSportToESPN(String sport) {
        return switch (sport.toUpperCase()) {
            case "NFL" -> "football/nfl";
            case "NBA" -> "basketball/nba";
            case "MLB" -> "baseball/mlb";
            case "NHL" -> "hockey/nhl";
            case "NCAAF" -> "football/college-football";
            case "NCAAB" -> "basketball/mens-college-basketball";
            default -> "football/nfl";
        };
    }
    
    private Map<String, Object> parseGameResult(String jsonResponse, String homeTeam, String awayTeam) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("homeTeam", homeTeam);
            result.put("awayTeam", awayTeam);
            result.put("homeScore", 0);
            result.put("awayScore", 0);
            result.put("status", "PENDING");
            result.put("note", "ESPN API integration active - manual settlement required for now");
            
        } catch (Exception e) {
            result.put("error", "Failed to parse game result");
        }
        
        return result;
    }
}
