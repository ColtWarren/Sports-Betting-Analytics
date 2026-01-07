package com.coltwarren.sports_betting_analytics.service.espn;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class ESPNApiService {
    
    private final WebClient webClient;
    
    public ESPNApiService() {
        this.webClient = WebClient.builder()
            .baseUrl("https://site.api.espn.com/apis/site/v2/sports")
            .build();
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
    public String determineBetOutcome(Map<String, Object> gameResult, String selection, 
                                      String betType, Integer odds, Double line) {
        
        if (gameResult.containsKey("error")) {
            return "PENDING";
        }
        
        String status = (String) gameResult.get("status");
        if (!"FINAL".equals(status)) {
            return "PENDING";
        }
        
        int homeScore = (Integer) gameResult.get("homeScore");
        int awayScore = (Integer) gameResult.get("awayScore");
        String homeTeam = (String) gameResult.get("homeTeam");
        String awayTeam = (String) gameResult.get("awayTeam");
        
        boolean selectedHome = selection.toUpperCase().contains(homeTeam.toUpperCase());
        boolean selectedAway = selection.toUpperCase().contains(awayTeam.toUpperCase());
        
        if (!selectedHome && !selectedAway) {
            return "PENDING";
        }
        
        if ("MONEYLINE".equalsIgnoreCase(betType)) {
            if (homeScore == awayScore) return "PUSH";
            
            boolean homeWon = homeScore > awayScore;
            if (selectedHome) {
                return homeWon ? "WON" : "LOST";
            } else {
                return homeWon ? "LOST" : "WON";
            }
            
        } else if ("SPREAD".equalsIgnoreCase(betType) && line != null) {
            double adjustedScore;
            double opponentScore;
            
            if (selectedHome) {
                adjustedScore = homeScore + line;
                opponentScore = awayScore;
            } else {
                adjustedScore = awayScore + line;
                opponentScore = homeScore;
            }
            
            if (adjustedScore == opponentScore) return "PUSH";
            return adjustedScore > opponentScore ? "WON" : "LOST";
            
        } else if (betType.toUpperCase().contains("OVER") || betType.toUpperCase().contains("UNDER")) {
            if (line == null) return "PENDING";
            
            int totalPoints = homeScore + awayScore;
            
            if (totalPoints == line) return "PUSH";
            
            boolean isOver = betType.toUpperCase().contains("OVER");
            if (isOver) {
                return totalPoints > line ? "WON" : "LOST";
            } else {
                return totalPoints < line ? "WON" : "LOST";
            }
        }
        
        return "PENDING";
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
