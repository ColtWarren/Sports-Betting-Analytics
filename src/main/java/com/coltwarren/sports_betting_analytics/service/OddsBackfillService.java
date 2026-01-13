package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.GameStats;
import com.coltwarren.sports_betting_analytics.repository.GameStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class OddsBackfillService {
    
    private final WebClient webClient;
    private final GameStatsRepository gameStatsRepository;
    private final String oddsApiKey;
    
    // Team name mapping for better matching
    private static final Map<String, String> TEAM_NICKNAMES = new HashMap<>();
    static {
        TEAM_NICKNAMES.put("Cardinals", "Arizona Cardinals");
        TEAM_NICKNAMES.put("Falcons", "Atlanta Falcons");
        TEAM_NICKNAMES.put("Ravens", "Baltimore Ravens");
        TEAM_NICKNAMES.put("Bills", "Buffalo Bills");
        TEAM_NICKNAMES.put("Panthers", "Carolina Panthers");
        TEAM_NICKNAMES.put("Bears", "Chicago Bears");
        TEAM_NICKNAMES.put("Bengals", "Cincinnati Bengals");
        TEAM_NICKNAMES.put("Browns", "Cleveland Browns");
        TEAM_NICKNAMES.put("Cowboys", "Dallas Cowboys");
        TEAM_NICKNAMES.put("Broncos", "Denver Broncos");
        TEAM_NICKNAMES.put("Lions", "Detroit Lions");
        TEAM_NICKNAMES.put("Packers", "Green Bay Packers");
        TEAM_NICKNAMES.put("Texans", "Houston Texans");
        TEAM_NICKNAMES.put("Colts", "Indianapolis Colts");
        TEAM_NICKNAMES.put("Jaguars", "Jacksonville Jaguars");
        TEAM_NICKNAMES.put("Chiefs", "Kansas City Chiefs");
        TEAM_NICKNAMES.put("Raiders", "Las Vegas Raiders");
        TEAM_NICKNAMES.put("Chargers", "Los Angeles Chargers");
        TEAM_NICKNAMES.put("Rams", "Los Angeles Rams");
        TEAM_NICKNAMES.put("Dolphins", "Miami Dolphins");
        TEAM_NICKNAMES.put("Vikings", "Minnesota Vikings");
        TEAM_NICKNAMES.put("Patriots", "New England Patriots");
        TEAM_NICKNAMES.put("Saints", "New Orleans Saints");
        TEAM_NICKNAMES.put("Giants", "New York Giants");
        TEAM_NICKNAMES.put("Jets", "New York Jets");
        TEAM_NICKNAMES.put("Eagles", "Philadelphia Eagles");
        TEAM_NICKNAMES.put("Steelers", "Pittsburgh Steelers");
        TEAM_NICKNAMES.put("49ers", "San Francisco 49ers");
        TEAM_NICKNAMES.put("Seahawks", "Seattle Seahawks");
        TEAM_NICKNAMES.put("Buccaneers", "Tampa Bay Buccaneers");
        TEAM_NICKNAMES.put("Titans", "Tennessee Titans");
        TEAM_NICKNAMES.put("Commanders", "Washington Commanders");
    }
    
    @Autowired
    public OddsBackfillService(GameStatsRepository gameStatsRepository,
                              @Value("${odds.api.key}") String oddsApiKey) {
        this.gameStatsRepository = gameStatsRepository;
        this.oddsApiKey = oddsApiKey;
        
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024))
            .build();
        
        this.webClient = WebClient.builder()
            .baseUrl("https://api.the-odds-api.com/v4")
            .exchangeStrategies(strategies)
            .build();
    }
    
    public Map<String, Object> backfillClosingLines() {
        Map<String, Object> result = new HashMap<>();
        int gamesUpdated = 0;
        int gamesProcessed = 0;
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
            
            System.out.println("📊 Found " + gamesNeedingOdds.size() + " games needing odds data");
            
            // Group games by date to minimize API calls
            Map<String, List<GameStats>> gamesByDate = new HashMap<>();
            for (GameStats game : gamesNeedingOdds) {
                String dateKey = game.getGameTime().toLocalDate().toString();
                gamesByDate.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(game);
            }
            
            // Fetch odds for each date
            for (Map.Entry<String, List<GameStats>> entry : gamesByDate.entrySet()) {
                String date = entry.getKey();
                List<GameStats> dateGames = entry.getValue();
                
                try {
                    System.out.println("🔍 Fetching odds for " + date + " (" + dateGames.size() + " games)");
                    
                    // Fetch historical odds for this date
                    String isoDate = date + "T12:00:00Z";
                    List<Map<String, Object>> oddsData = fetchHistoricalOdds(isoDate);
                    
                    if (oddsData != null && !oddsData.isEmpty()) {
                        System.out.println("✅ Got " + oddsData.size() + " events from API");
                        
                        // Match odds to games
                        for (GameStats game : dateGames) {
                            gamesProcessed++;
                            Map<String, Object> matchedOdds = findMatchingOdds(game, oddsData);
                            
                            if (matchedOdds != null) {
                                updateGameWithOdds(game, matchedOdds);
                                gameStatsRepository.save(game);
                                gamesUpdated++;
                                System.out.println("✅ Updated: " + game.getAwayTeam() + " @ " + game.getHomeTeam());
                            } else {
                                System.out.println("⚠️ No match: " + game.getAwayTeam() + " @ " + game.getHomeTeam());
                            }
                        }
                    } else {
                        System.out.println("⚠️ No odds data available for " + date);
                    }
                    
                    // Rate limiting - wait 1 second between API calls
                    Thread.sleep(1000);
                    
                } catch (Exception e) {
                    String error = "Error fetching odds for " + date + ": " + e.getMessage();
                    errors.add(error);
                    System.err.println("❌ " + error);
                }
            }
            
            result.put("success", true);
            result.put("gamesProcessed", gamesProcessed);
            result.put("gamesUpdated", gamesUpdated);
            result.put("gamesMissingOdds", gamesProcessed - gamesUpdated);
            result.put("errors", errors);
            
            System.out.println(String.format("✅ Backfill complete! Processed: %d, Updated: %d, Missing: %d", 
                gamesProcessed, gamesUpdated, gamesProcessed - gamesUpdated));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            System.err.println("❌ Backfill failed: " + e.getMessage());
        }
        
        return result;
    }
    
    private List<Map<String, Object>> fetchHistoricalOdds(String isoDate) {
        try {
            // API returns an array directly, not wrapped in object
            List<Map<String, Object>> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/sports/americanfootball_nfl/odds")
                    .queryParam("apiKey", oddsApiKey)
                    .queryParam("regions", "us")
                    .queryParam("markets", "spreads,totals")
                    .queryParam("oddsFormat", "american")
                    .queryParam("date", isoDate)
                    .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();
            
            return response != null ? response : Collections.emptyList();
            
        } catch (Exception e) {
            System.err.println("Error fetching odds: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private Map<String, Object> findMatchingOdds(GameStats game, List<Map<String, Object>> oddsData) {
        String homeTeam = game.getHomeTeam();
        String awayTeam = game.getAwayTeam();
        
        // Normalize team names to full names
        String homeFullName = TEAM_NICKNAMES.getOrDefault(homeTeam, homeTeam);
        String awayFullName = TEAM_NICKNAMES.getOrDefault(awayTeam, awayTeam);
        
        for (Map<String, Object> event : oddsData) {
            String homeTeamOdds = (String) event.get("home_team");
            String awayTeamOdds = (String) event.get("away_team");
            
            if (homeTeamOdds == null || awayTeamOdds == null) continue;
            
            // Try multiple matching strategies
            boolean homeMatch = 
                homeTeamOdds.equalsIgnoreCase(homeFullName) ||
                homeTeamOdds.equalsIgnoreCase(homeTeam) ||
                homeTeamOdds.toLowerCase().contains(homeTeam.toLowerCase()) ||
                homeFullName.toLowerCase().contains(homeTeamOdds.toLowerCase()) ||
                normalizeTeamName(homeTeamOdds).equals(normalizeTeamName(homeTeam)) ||
                normalizeTeamName(homeTeamOdds).equals(normalizeTeamName(homeFullName));
            
            boolean awayMatch = 
                awayTeamOdds.equalsIgnoreCase(awayFullName) ||
                awayTeamOdds.equalsIgnoreCase(awayTeam) ||
                awayTeamOdds.toLowerCase().contains(awayTeam.toLowerCase()) ||
                awayFullName.toLowerCase().contains(awayTeamOdds.toLowerCase()) ||
                normalizeTeamName(awayTeamOdds).equals(normalizeTeamName(awayTeam)) ||
                normalizeTeamName(awayTeamOdds).equals(normalizeTeamName(awayFullName));
            
            if (homeMatch && awayMatch) {
                System.out.println("🎯 Matched: " + awayTeam + " @ " + homeTeam + 
                                 " <-> " + awayTeamOdds + " @ " + homeTeamOdds);
                return event;
            }
        }
        
        return null;
    }
    
    private String normalizeTeamName(String teamName) {
        // Extract just the nickname (last word) and lowercase
        String[] parts = teamName.trim().split("\\s+");
        return parts[parts.length - 1].toLowerCase();
    }
    
    private void updateGameWithOdds(GameStats game, Map<String, Object> oddsEvent) {
        try {
            List<Map<String, Object>> bookmakers = 
                (List<Map<String, Object>>) oddsEvent.get("bookmakers");
            
            if (bookmakers == null || bookmakers.isEmpty()) return;
            
            // Use consensus (average) from multiple books
            List<Double> spreads = new ArrayList<>();
            List<Double> totals = new ArrayList<>();
            
            String homeTeamFull = TEAM_NICKNAMES.getOrDefault(game.getHomeTeam(), game.getHomeTeam());
            
            for (Map<String, Object> bookmaker : bookmakers) {
                List<Map<String, Object>> markets = 
                    (List<Map<String, Object>>) bookmaker.get("markets");
                
                if (markets == null) continue;
                
                for (Map<String, Object> market : markets) {
                    String marketKey = (String) market.get("key");
                    List<Map<String, Object>> outcomes = 
                        (List<Map<String, Object>>) market.get("outcomes");
                    
                    if (outcomes == null) continue;
                    
                    if ("spreads".equals(marketKey)) {
                        // Find home team spread
                        for (Map<String, Object> outcome : outcomes) {
                            String name = (String) outcome.get("name");
                            
                            // Match home team
                            if (name.equalsIgnoreCase(homeTeamFull) || 
                                name.equalsIgnoreCase(game.getHomeTeam()) ||
                                normalizeTeamName(name).equals(normalizeTeamName(game.getHomeTeam()))) {
                                
                                Object pointObj = outcome.get("point");
                                if (pointObj != null) {
                                    double point = pointObj instanceof Integer ? 
                                        ((Integer) pointObj).doubleValue() : (Double) pointObj;
                                    spreads.add(point);
                                }
                            }
                        }
                    } else if ("totals".equals(marketKey)) {
                        // Get total
                        for (Map<String, Object> outcome : outcomes) {
                            Object pointObj = outcome.get("point");
                            if (pointObj != null) {
                                double point = pointObj instanceof Integer ? 
                                    ((Integer) pointObj).doubleValue() : (Double) pointObj;
                                totals.add(point);
                                break; // Only need one total
                            }
                        }
                    }
                }
            }
            
            // Set consensus spread and total
            if (!spreads.isEmpty()) {
                double avgSpread = spreads.stream().mapToDouble(d -> d).average().orElse(0.0);
                game.setClosingSpread(avgSpread);
                System.out.println("  📊 Spread: " + avgSpread);
            }
            
            if (!totals.isEmpty()) {
                double avgTotal = totals.stream().mapToDouble(d -> d).average().orElse(0.0);
                game.setClosingTotal(avgTotal);
                System.out.println("  📊 Total: " + avgTotal);
            }
            
            // Trigger recalculation of ATS and O/U
            game.setHomeScore(game.getHomeScore());
            game.setAwayScore(game.getAwayScore());
            
        } catch (Exception e) {
            System.err.println("Error updating game with odds: " + e.getMessage());
        }
    }
}
