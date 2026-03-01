package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.GameStats;
import com.coltwarren.sports_betting_analytics.repository.GameStatsRepository;
import com.coltwarren.sports_betting_analytics.util.TeamNameUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Slf4j
@Service
public class OddsBackfillService {
    
    private final WebClient espnScoreboardClient;
    private final WebClient espnOddsClient;
    private final GameStatsRepository gameStatsRepository;
    
    public OddsBackfillService(GameStatsRepository gameStatsRepository,
                               WebClient.Builder webClientBuilder) {
        this.gameStatsRepository = gameStatsRepository;
        this.espnScoreboardClient = webClientBuilder
            .baseUrl("https://site.api.espn.com/apis/site/v2/sports/football/nfl")
            .build();
        this.espnOddsClient = webClientBuilder
            .baseUrl("https://sports.core.api.espn.com/v2/sports/football/leagues/nfl")
            .build();
    }
    
    @Transactional
    public Map<String, Object> backfillClosingLines() {
        Map<String, Object> result = new HashMap<>();
        int gamesUpdated = 0;
        int gamesProcessed = 0;
        int gamesFailed = 0;
        List<String> errors = new ArrayList<>();
        
        try {
            // Get all games without closing lines
            List<GameStats> games = gameStatsRepository.findAll();
            List<GameStats> gamesNeedingOdds = new ArrayList<>();
            
            for (GameStats game : games) {
                if (game.getClosingSpread() == null || game.getClosingTotal() == null) {
                    gamesNeedingOdds.add(game);
                }
            }
            
            log.info("Found {} games needing odds data", gamesNeedingOdds.size());
            
            // Process each game individually
            for (GameStats game : gamesNeedingOdds) {
                gamesProcessed++;
                
                try {
                    log.info("Fetching odds for: {} @ {}", game.getAwayTeam(), game.getHomeTeam());
                    
                    Map<String, Object> oddsData = fetchEspnOdds(game);
                    
                    if (oddsData != null) {
                        updateGameWithEspnOdds(game, oddsData);
                        gameStatsRepository.save(game);
                        gamesUpdated++;
                        log.info("Updated: {} @ {} (Spread: {}, Total: {})", game.getAwayTeam(), game.getHomeTeam(), game.getClosingSpread(), game.getClosingTotal());
                    } else {
                        gamesFailed++;
                        log.warn("No odds found: {} @ {}", game.getAwayTeam(), game.getHomeTeam());
                    }
                    
                    // Rate limiting - wait 500ms between requests
                    Thread.sleep(500);
                    
                } catch (Exception e) {
                    String error = "Error processing " + game.getAwayTeam() + " @ " + game.getHomeTeam() + ": " + e.getMessage();
                    errors.add(error);
                    gamesFailed++;
                    log.error("{}", error);
                }
            }
            
            result.put("success", true);
            result.put("gamesProcessed", gamesProcessed);
            result.put("gamesUpdated", gamesUpdated);
            result.put("gamesFailed", gamesFailed);
            result.put("errors", errors);
            
            log.info("Backfill complete! Processed: {}, Updated: {}, Failed: {}", gamesProcessed, gamesUpdated, gamesFailed);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("Backfill failed: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    private Map<String, Object> fetchEspnOdds(GameStats game) {
        try {
            // First, get the scoreboard for the game date to find the ESPN event ID
            String dateStr = game.getGameTime().toLocalDate().toString().replace("-", "");
            
            Map<String, Object> scoreboard = espnScoreboardClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/scoreboard")
                    .queryParam("dates", dateStr)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (scoreboard == null || !scoreboard.containsKey("events")) {
                return null;
            }
            
            List<Map<String, Object>> events = (List<Map<String, Object>>) scoreboard.get("events");
            
            // Find the matching game
            for (Map<String, Object> event : events) {
                String eventId = (String) event.get("id");
                List<Map<String, Object>> competitions = 
                    (List<Map<String, Object>>) event.get("competitions");
                
                if (competitions == null || competitions.isEmpty()) continue;
                
                Map<String, Object> competition = competitions.get(0);
                List<Map<String, Object>> competitors = 
                    (List<Map<String, Object>>) competition.get("competitors");
                
                if (competitors == null || competitors.size() != 2) continue;
                
                // Extract team names
                String espnHomeTeam = null;
                String espnAwayTeam = null;
                
                for (Map<String, Object> competitor : competitors) {
                    Map<String, Object> team = (Map<String, Object>) competitor.get("team");
                    String homeAway = (String) competitor.get("homeAway");
                    String teamName = TeamNameUtils.extractTeamName((String) team.get("displayName"));
                    
                    if ("home".equals(homeAway)) {
                        espnHomeTeam = teamName;
                    } else {
                        espnAwayTeam = teamName;
                    }
                }
                
                // Check if teams match
                if (game.getHomeTeam().equals(espnHomeTeam) && game.getAwayTeam().equals(espnAwayTeam)) {
                    // Found the game! Now fetch odds
                    return fetchOddsForEvent(eventId);
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Error fetching ESPN odds: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private Map<String, Object> fetchOddsForEvent(String eventId) {
        try {
            Map<String, Object> oddsResponse = espnOddsClient.get()
                .uri("/events/" + eventId + "/competitions/" + eventId + "/odds")
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (oddsResponse == null || !oddsResponse.containsKey("items")) {
                return null;
            }
            
            List<Map<String, Object>> items = (List<Map<String, Object>>) oddsResponse.get("items");
            
            if (items.isEmpty()) {
                return null;
            }
            
            // Return the first odds provider (usually DraftKings)
            return items.get(0);
            
        } catch (Exception e) {
            log.error("Error fetching odds for event {}: {}", eventId, e.getMessage(), e);
            return null;
        }
    }
    
    private void updateGameWithEspnOdds(GameStats game, Map<String, Object> oddsData) {
        try {
            // Get closing spread
            if (oddsData.containsKey("spread")) {
                Object spreadObj = oddsData.get("spread");
                if (spreadObj instanceof Number) {
                    double spread = ((Number) spreadObj).doubleValue();
                    game.setClosingSpread(spread);
                }
            }
            
            // Get closing total
            if (oddsData.containsKey("overUnder")) {
                Object totalObj = oddsData.get("overUnder");
                if (totalObj instanceof Number) {
                    double total = ((Number) totalObj).doubleValue();
                    game.setClosingTotal(total);
                }
            }
            
            // Try to get opening lines from "open" object
            if (oddsData.containsKey("open") && game.getOpeningSpread() == null) {
                Map<String, Object> open = (Map<String, Object>) oddsData.get("open");
                
                // Get opening total
                if (open.containsKey("total")) {
                    Map<String, Object> total = (Map<String, Object>) open.get("total");
                    if (total.containsKey("american")) {
                        try {
                            String totalStr = (String) total.get("american");
                            game.setOpeningTotal(Double.parseDouble(totalStr));
                        } catch (Exception e) {
                            // Skip if can't parse
                        }
                    }
                }
            }
            
            // Trigger recalculation of ATS and O/U
            if (game.getHomeScore() != null && game.getAwayScore() != null) {
                game.setHomeScore(game.getHomeScore());
                game.setAwayScore(game.getAwayScore());
            }
            
        } catch (Exception e) {
            log.error("Error updating game with ESPN odds: {}", e.getMessage(), e);
        }
    }
    
}
