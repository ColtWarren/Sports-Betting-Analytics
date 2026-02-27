package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.GameStats;
import com.coltwarren.sports_betting_analytics.repository.GameStatsRepository;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EspnGameSyncService {
    
    private final WebClient webClient;
    private final GameStatsRepository gameStatsRepository;
    
    public EspnGameSyncService(GameStatsRepository gameStatsRepository) {
        this.gameStatsRepository = gameStatsRepository;
        
        // Increase buffer size to handle large responses
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024)) // 10 MB buffer
            .build();
        
        this.webClient = WebClient.builder()
            .baseUrl("https://site.api.espn.com/apis/site/v2/sports/football/nfl")
            .exchangeStrategies(strategies)
            .build();
    }
    
    // Run automatically every day at 3 AM
    @Scheduled(cron = "0 0 3 * * *")
    public void syncCompletedGamesDaily() {
        System.out.println("🔄 Starting daily ESPN game sync...");
        syncCompletedGames();
    }
    
    // Manual sync method (can be called via endpoint)
    public Map<String, Object> syncCompletedGames() {
        Map<String, Object> result = new HashMap<>();
        int gamesProcessed = 0;
        int gamesSaved = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            // Fetch scoreboard for current season
            Map<String, Object> scoreboard = fetchScoreboard();
            
            if (scoreboard == null || !scoreboard.containsKey("events")) {
                result.put("success", false);
                result.put("error", "No events found in scoreboard");
                return result;
            }
            
            List<Map<String, Object>> events = (List<Map<String, Object>>) scoreboard.get("events");
            
            for (Map<String, Object> event : events) {
                gamesProcessed++;
                try {
                    // Only process completed games
                    Map<String, Object> status = (Map<String, Object>) 
                        ((Map<String, Object>) event.get("status")).get("type");
                    
                    if (!"STATUS_FINAL".equals(status.get("name"))) {
                        continue; // Skip in-progress or scheduled games
                    }
                    
                    GameStats gameStats = parseGameFromEvent(event);
                    
                    if (gameStats != null) {
                        // Check if game already exists (avoid duplicates)
                        if (!gameExists(gameStats)) {
                            gameStatsRepository.save(gameStats);
                            gamesSaved++;
                            System.out.println("✅ Saved: " + gameStats.getAwayTeam() + " @ " + 
                                             gameStats.getHomeTeam() + " (" + gameStats.getAwayScore() + 
                                             "-" + gameStats.getHomeScore() + ")");
                        }
                    }
                    
                } catch (Exception e) {
                    errors.add("Error processing game: " + e.getMessage());
                    System.err.println("❌ Error processing game: " + e.getMessage());
                }
            }
            
            result.put("success", true);
            result.put("gamesProcessed", gamesProcessed);
            result.put("gamesSaved", gamesSaved);
            result.put("gamesSkipped", gamesProcessed - gamesSaved);
            result.put("errors", errors);
            
            System.out.println(String.format("✅ Sync complete! Processed: %d, Saved: %d, Skipped: %d", 
                gamesProcessed, gamesSaved, gamesProcessed - gamesSaved));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            System.err.println("❌ Sync failed: " + e.getMessage());
        }
        
        return result;
    }
    
    private Map<String, Object> fetchScoreboard() {
        try {
            return webClient.get()
                .uri("/scoreboard")
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        } catch (Exception e) {
            System.err.println("Error fetching scoreboard: " + e.getMessage());
            return null;
        }
    }
    
    private GameStats parseGameFromEvent(Map<String, Object> event) {
        try {
            GameStats game = new GameStats();
            game.setSport("NFL");
            
            // Get game date
            String dateStr = (String) event.get("date");
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateStr);
            game.setGameTime(zonedDateTime.toLocalDateTime());
            
            // Get teams
            List<Map<String, Object>> competitions = 
                (List<Map<String, Object>>) event.get("competitions");
            Map<String, Object> competition = competitions.get(0);
            
            List<Map<String, Object>> competitors = 
                (List<Map<String, Object>>) competition.get("competitors");
            
            String homeTeam = null;
            String awayTeam = null;
            Integer homeScore = null;
            Integer awayScore = null;
            
            for (Map<String, Object> competitor : competitors) {
                String homeAway = (String) competitor.get("homeAway");
                Map<String, Object> team = (Map<String, Object>) competitor.get("team");
                String teamName = extractTeamName((String) team.get("displayName"));
                Integer score = Integer.parseInt((String) competitor.get("score"));
                
                if ("home".equals(homeAway)) {
                    homeTeam = teamName;
                    homeScore = score;
                } else {
                    awayTeam = teamName;
                    awayScore = score;
                }
            }
            
            game.setHomeTeam(homeTeam);
            game.setAwayTeam(awayTeam);
            game.setHomeScore(homeScore);
            game.setAwayScore(awayScore);
            
            // Try to get odds if available
            if (competition.containsKey("odds")) {
                try {
                    List<Map<String, Object>> odds = 
                        (List<Map<String, Object>>) competition.get("odds");
                    if (!odds.isEmpty()) {
                        Map<String, Object> oddsData = odds.get(0);
                        
                        if (oddsData.containsKey("spread")) {
                            game.setClosingSpread((Double) oddsData.get("spread"));
                        }
                        
                        if (oddsData.containsKey("overUnder")) {
                            game.setClosingTotal((Double) oddsData.get("overUnder"));
                        }
                    }
                } catch (Exception e) {
                    // Odds not available, that's okay
                }
            }
            
            return game;
            
        } catch (Exception e) {
            System.err.println("Error parsing game: " + e.getMessage());
            return null;
        }
    }
    
    private String extractTeamName(String displayName) {
        // Extract last word (team name) from "City Team" format
        // e.g., "Buffalo Bills" -> "Bills"
        String[] parts = displayName.split(" ");
        return parts[parts.length - 1];
    }
    
    private boolean gameExists(GameStats newGame) {
        // Check if game with same teams and date already exists
        List<GameStats> existing = gameStatsRepository.findAll();
        
        for (GameStats game : existing) {
            if (game.getHomeTeam().equals(newGame.getHomeTeam()) &&
                game.getAwayTeam().equals(newGame.getAwayTeam()) &&
                game.getGameTime().toLocalDate().equals(newGame.getGameTime().toLocalDate())) {
                return true;
            }
        }
        
        return false;
    }
    
    // Sync specific date range (useful for backfilling) - SYNC WEEK BY WEEK
    public Map<String, Object> syncDateRange(String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        int totalSaved = 0;
        
        try {
            Map<String, Object> scoreboard = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/scoreboard")
                    .queryParam("dates", startDate + "-" + endDate)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (scoreboard != null && scoreboard.containsKey("events")) {
                List<Map<String, Object>> events = 
                    (List<Map<String, Object>>) scoreboard.get("events");
                
                for (Map<String, Object> event : events) {
                    try {
                        Map<String, Object> status = (Map<String, Object>) 
                            ((Map<String, Object>) event.get("status")).get("type");
                        
                        if ("STATUS_FINAL".equals(status.get("name"))) {
                            GameStats gameStats = parseGameFromEvent(event);
                            
                            if (gameStats != null && !gameExists(gameStats)) {
                                gameStatsRepository.save(gameStats);
                                totalSaved++;
                                System.out.println("✅ Saved: " + gameStats.getAwayTeam() + " @ " + 
                                                 gameStats.getHomeTeam());
                            }
                        }
                    } catch (Exception e) {
                        // Continue processing other games
                    }
                }
            }
            
            result.put("success", true);
            result.put("gamesSaved", totalSaved);
            result.put("dateRange", startDate + " to " + endDate);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
