package com.coltwarren.sports_betting_analytics.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class LiveGameService {
    
    private final WebClient espnClient;
    
    public LiveGameService(@Qualifier("espnWebClient") WebClient espnClient) {
        this.espnClient = espnClient;
    }
    
    public List<Map<String, Object>> getLiveGames(String sport) {
        try {
            String endpoint = getEndpointForSport(sport);
            
            Map<String, Object> scoreboard = espnClient.get()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(10));
            
            if (scoreboard == null || !scoreboard.containsKey("events")) {
                return Collections.emptyList();
            }
            
            List<Map<String, Object>> events = (List<Map<String, Object>>) scoreboard.get("events");
            List<Map<String, Object>> liveGames = new ArrayList<>();
            
            for (Map<String, Object> event : events) {
                Map<String, Object> gameInfo = parseGameInfo(event, sport);
                
                String status = (String) gameInfo.get("status");
                
                // Include live games and upcoming games (next 24 hours)
                if ("IN_PROGRESS".equals(status) || "UPCOMING".equals(status)) {
                    liveGames.add(gameInfo);
                }
            }
            
            return liveGames;
            
        } catch (Exception e) {
            log.error("Error fetching live games for {}: {}", sport, e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private String getEndpointForSport(String sport) {
        switch (sport.toUpperCase()) {
            case "NFL":
                return "/football/nfl/scoreboard";
            case "CFB":
            case "COLLEGE-FOOTBALL":
                return "/football/college-football/scoreboard";
            case "NBA":
                return "/basketball/nba/scoreboard";
            case "CBB":
            case "COLLEGE-BASKETBALL":
                return "/basketball/mens-college-basketball/scoreboard";
            case "WCBB":
                return "/basketball/womens-college-basketball/scoreboard";
            case "MLB":
                return "/baseball/mlb/scoreboard";
            case "NHL":
                return "/hockey/nhl/scoreboard";
            default:
                return "/football/nfl/scoreboard";
        }
    }
    
    private String getSportDisplayName(String sport) {
        switch (sport.toUpperCase()) {
            case "NFL":
                return "NFL";
            case "CFB":
            case "COLLEGE-FOOTBALL":
                return "College Football";
            case "NBA":
                return "NBA";
            case "CBB":
            case "COLLEGE-BASKETBALL":
                return "College Basketball";
            case "WCBB":
                return "Women's College Basketball";
            case "MLB":
                return "MLB";
            case "NHL":
                return "NHL";
            default:
                return sport;
        }
    }
    
    private Map<String, Object> parseGameInfo(Map<String, Object> event, String sport) {
        Map<String, Object> gameInfo = new HashMap<>();
        
        try {
            // Event ID
            gameInfo.put("eventId", event.get("id"));
            gameInfo.put("sport", getSportDisplayName(sport));
            
            // Get competition data
            List<Map<String, Object>> competitions = 
                (List<Map<String, Object>>) event.get("competitions");
            
            if (competitions == null || competitions.isEmpty()) {
                return gameInfo;
            }
            
            Map<String, Object> competition = competitions.get(0);
            
            // Get status
            Map<String, Object> status = (Map<String, Object>) competition.get("status");
            Map<String, Object> statusType = (Map<String, Object>) status.get("type");
            String statusState = (String) statusType.get("state");
            String statusDetail = (String) statusType.get("detail");
            
            // Map status
            if ("in".equals(statusState)) {
                gameInfo.put("status", "IN_PROGRESS");
            } else if ("pre".equals(statusState)) {
                gameInfo.put("status", "UPCOMING");
            } else {
                gameInfo.put("status", "COMPLETED");
            }
            
            gameInfo.put("statusDetail", statusDetail);
            
            // Get game time
            String dateStr = (String) event.get("date");
            gameInfo.put("gameTime", dateStr);
            
            // Get clock info
            if (status.containsKey("displayClock")) {
                gameInfo.put("clock", status.get("displayClock"));
            }
            
            if (status.containsKey("period")) {
                gameInfo.put("period", status.get("period"));
            }
            
            // Get teams
            List<Map<String, Object>> competitors = 
                (List<Map<String, Object>>) competition.get("competitors");
            
            if (competitors != null && competitors.size() == 2) {
                for (Map<String, Object> competitor : competitors) {
                    Map<String, Object> team = (Map<String, Object>) competitor.get("team");
                    String homeAway = (String) competitor.get("homeAway");
                    String score = (String) competitor.get("score");
                    String displayName = (String) team.get("displayName");
                    String abbreviation = (String) team.get("abbreviation");
                    
                    if ("home".equals(homeAway)) {
                        gameInfo.put("homeTeam", displayName);
                        gameInfo.put("homeTeamAbbr", abbreviation);
                        gameInfo.put("homeScore", score != null ? Integer.parseInt(score) : 0);
                    } else {
                        gameInfo.put("awayTeam", displayName);
                        gameInfo.put("awayTeamAbbr", abbreviation);
                        gameInfo.put("awayScore", score != null ? Integer.parseInt(score) : 0);
                    }
                }
            }
            
            // Get venue
            if (competition.containsKey("venue")) {
                Map<String, Object> venue = (Map<String, Object>) competition.get("venue");
                if (venue.containsKey("fullName")) {
                    gameInfo.put("venue", venue.get("fullName"));
                }
            }
            
            // Get broadcast info
            if (competition.containsKey("broadcasts")) {
                List<Map<String, Object>> broadcasts = 
                    (List<Map<String, Object>>) competition.get("broadcasts");
                if (!broadcasts.isEmpty()) {
                    Map<String, Object> broadcast = broadcasts.get(0);
                    if (broadcast.containsKey("names")) {
                        List<String> networks = (List<String>) broadcast.get("names");
                        if (!networks.isEmpty()) {
                            gameInfo.put("network", networks.get(0));
                        }
                    }
                }
            }
            
            // Get ranking info (for college sports)
            if (competitors != null) {
                for (Map<String, Object> competitor : competitors) {
                    if (competitor.containsKey("curatedRank")) {
                        Map<String, Object> rank = (Map<String, Object>) competitor.get("curatedRank");
                        if (rank != null && rank.containsKey("current")) {
                            String homeAway = (String) competitor.get("homeAway");
                            if ("home".equals(homeAway)) {
                                gameInfo.put("homeRank", rank.get("current"));
                            } else {
                                gameInfo.put("awayRank", rank.get("current"));
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error parsing game info: {}", e.getMessage());
        }
        
        return gameInfo;
    }
}
