package com.coltwarren.sports_betting_analytics.service.odds;

import com.coltwarren.sports_betting_analytics.config.OddsApiProperties;
import com.coltwarren.sports_betting_analytics.util.MissouriComplianceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

/**
 * Multi-Sport Odds Service
 *
 * MISSOURI LEGAL COMPLIANCE
 *
 * This application only displays odds from sportsbooks licensed by the
 * Missouri Gaming Commission as of January 2026. These are:
 *
 * Licensed Missouri Sportsbooks:
 * 1. DraftKings
 * 2. FanDuel
 * 3. BetMGM
 * 4. Caesars Sportsbook
 * 5. bet365
 * 6. Fanatics Sportsbook
 * 7. Circa Sports
 * 8. theScore Bet
 *
 * Using unlicensed sportsbooks in Missouri is against state law and can
 * result in account bans, fund confiscation, and legal penalties.
 *
 * This filter ensures users only see legal betting options.
 * All non-licensed bookmakers are FILTERED OUT automatically.
 */
@Slf4j
@Service
public class MultiSportOddsService {

    private final WebClient oddsClient;
    private final OddsApiProperties oddsApiProperties;

    public MultiSportOddsService(@Qualifier("oddsApiWebClient") WebClient oddsClient,
                                 OddsApiProperties oddsApiProperties) {
        this.oddsClient = oddsClient;
        this.oddsApiProperties = oddsApiProperties;
    }

    public List<Map<String, Object>> getOddsForSport(String sport) {
        try {
            String sportKey = getSportKey(sport);
            
            // The Odds API returns an array directly, not an object
            List<Map<String, Object>> oddsData = oddsClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/sports/" + sportKey + "/odds")
                    .queryParam("apiKey", oddsApiProperties.getApiKey())
                    .queryParam("regions", "us")
                    .queryParam("markets", "h2h,spreads,totals")
                    .queryParam("oddsFormat", "american")
                    .build())
                .retrieve()
                .bodyToMono(List.class)
                .block(Duration.ofSeconds(10));
            
            if (oddsData == null || oddsData.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<Map<String, Object>> games = new ArrayList<>();
            
            for (Map<String, Object> game : oddsData) {
                Map<String, Object> parsedGame = parseGame(game, sport);
                games.add(parsedGame);
            }
            
            return games;
            
        } catch (Exception e) {
            log.error("Error fetching odds for {}: {}", sport, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    public Map<String, Object> getBestOddsForGame(String sport, String homeTeam, String awayTeam, String betType) {
        try {
            List<Map<String, Object>> allOdds = getOddsForSport(sport);
            
            // Find the game
            for (Map<String, Object> game : allOdds) {
                String gameHome = (String) game.get("homeTeam");
                String gameAway = (String) game.get("awayTeam");
                
                if (matchesTeams(gameHome, homeTeam) && matchesTeams(gameAway, awayTeam)) {
                    return findBestOddsForBetType(game, betType);
                }
            }
            
            return Collections.emptyMap();
            
        } catch (Exception e) {
            log.error("Error finding best odds: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    private String getSportKey(String sport) {
        return SportKeyMapper.toApiKey(sport);
    }
    
    private Map<String, Object> parseGame(Map<String, Object> game, String sport) {
        Map<String, Object> parsedGame = new HashMap<>();
        
        try {
            parsedGame.put("sport", sport);
            parsedGame.put("gameId", game.get("id"));
            parsedGame.put("homeTeam", game.get("home_team"));
            parsedGame.put("awayTeam", game.get("away_team"));
            parsedGame.put("commenceTime", game.get("commence_time"));
            
            // Parse bookmaker odds
            List<Map<String, Object>> bookmakers = (List<Map<String, Object>>) game.get("bookmakers");

            if (bookmakers != null && !bookmakers.isEmpty()) {
                List<Map<String, Object>> allBookmakerOdds = new ArrayList<>();

                // LEGAL COMPLIANCE: Filter to only Missouri-licensed sportsbooks
                int totalBooks = bookmakers.size();
                int filteredBooks = 0;

                for (Map<String, Object> bookmaker : bookmakers) {
                    String bookmakerName = (String) bookmaker.get("title");

                    // STRICT FILTER: Only process Missouri-licensed bookmakers
                    if (!MissouriComplianceUtils.isMissouriLegal(bookmakerName)) {
                        filteredBooks++;
                        log.info("FILTERED (Not MO-licensed): {}", bookmakerName);
                        continue; // Skip this bookmaker entirely
                    }

                    log.info("LEGAL (MO-licensed): {}", bookmakerName);

                    List<Map<String, Object>> markets = (List<Map<String, Object>>) bookmaker.get("markets");
                    
                    Map<String, Object> bookmakerOdds = new HashMap<>();
                    bookmakerOdds.put("bookmaker", bookmakerName);
                    
                    for (Map<String, Object> market : markets) {
                        String marketKey = (String) market.get("key");
                        List<Map<String, Object>> outcomes = (List<Map<String, Object>>) market.get("outcomes");
                        
                        if ("h2h".equals(marketKey)) {
                            // Moneyline - explicitly match both teams
                            String homeTeam = (String) game.get("home_team");
                            String awayTeam = (String) game.get("away_team");
                            for (Map<String, Object> outcome : outcomes) {
                                String team = (String) outcome.get("name");
                                Integer price = (Integer) outcome.get("price");

                                if (team != null && homeTeam != null && team.equalsIgnoreCase(homeTeam)) {
                                    bookmakerOdds.put("homeML", price);
                                } else if (team != null && awayTeam != null && team.equalsIgnoreCase(awayTeam)) {
                                    bookmakerOdds.put("awayML", price);
                                } else {
                                    log.warn("ML outcome team '{}' doesn't match home '{}' or away '{}'",
                                        team, homeTeam, awayTeam);
                                }
                            }
                        } else if ("spreads".equals(marketKey)) {
                            // Spread - explicitly match both teams
                            String homeTeam = (String) game.get("home_team");
                            String awayTeam = (String) game.get("away_team");
                            for (Map<String, Object> outcome : outcomes) {
                                String team = (String) outcome.get("name");
                                Number point = (Number) outcome.get("point");
                                Integer price = (Integer) outcome.get("price");

                                if (team != null && homeTeam != null && team.equalsIgnoreCase(homeTeam)) {
                                    bookmakerOdds.put("homeSpread", point.doubleValue());
                                    bookmakerOdds.put("homeSpreadOdds", price);
                                } else if (team != null && awayTeam != null && team.equalsIgnoreCase(awayTeam)) {
                                    bookmakerOdds.put("awaySpread", point.doubleValue());
                                    bookmakerOdds.put("awaySpreadOdds", price);
                                } else {
                                    log.warn("Spread outcome team '{}' doesn't match home '{}' or away '{}'",
                                        team, homeTeam, awayTeam);
                                }
                            }
                        } else if ("totals".equals(marketKey)) {
                            // Totals
                            for (Map<String, Object> outcome : outcomes) {
                                String name = (String) outcome.get("name");
                                Number point = (Number) outcome.get("point");
                                Integer price = (Integer) outcome.get("price");
                                
                                if ("Over".equals(name)) {
                                    bookmakerOdds.put("total", point.doubleValue());
                                    bookmakerOdds.put("overOdds", price);
                                } else {
                                    bookmakerOdds.put("underOdds", price);
                                }
                            }
                        }
                    }
                    
                    // Per-bookmaker ML↔spread consistency check
                    validateBookmakerConsistency(bookmakerOdds, (String) game.get("home_team"),
                        (String) game.get("away_team"));

                    allBookmakerOdds.add(bookmakerOdds);
                }

                // Log filtering summary for transparency
                int legalBooks = totalBooks - filteredBooks;
                if (filteredBooks > 0) {
                    log.info("FILTER SUMMARY: {} MO-licensed books, {} non-licensed books filtered out",
                                     legalBooks, filteredBooks);
                }

                parsedGame.put("bookmakers", allBookmakerOdds);

                // Find best odds (ONLY from Missouri-legal bookmakers)
                Map<String, Object> bestOdds = findBestOdds(allBookmakerOdds);

                // Cross-bookmaker ML↔spread consistency check on the assembled best odds
                validateBestOddsConsistency(bestOdds, (String) game.get("home_team"),
                    (String) game.get("away_team"));

                parsedGame.put("bestOdds", bestOdds);
            }
            
        } catch (Exception e) {
            log.error("Error parsing game: {}", e.getMessage(), e);
        }
        
        return parsedGame;
    }
    
    private Map<String, Object> findBestOdds(List<Map<String, Object>> allBookmakerOdds) {
        Map<String, Object> bestOdds = new HashMap<>();
        
        int bestHomeML = Integer.MIN_VALUE;
        int bestAwayML = Integer.MIN_VALUE;
        int bestHomeSpread = Integer.MIN_VALUE;
        int bestAwaySpread = Integer.MIN_VALUE;
        int bestOver = Integer.MIN_VALUE;
        int bestUnder = Integer.MIN_VALUE;
        
        String bestHomeMLBook = "";
        String bestAwayMLBook = "";
        String bestHomeSpreadBook = "";
        String bestAwaySpreadBook = "";
        String bestOverBook = "";
        String bestUnderBook = "";
        
        for (Map<String, Object> bookmaker : allBookmakerOdds) {
            String bookmakerName = (String) bookmaker.get("bookmaker");
            
            if (bookmaker.containsKey("homeML")) {
                int homeML = (Integer) bookmaker.get("homeML");
                if (homeML > bestHomeML) {
                    bestHomeML = homeML;
                    bestHomeMLBook = bookmakerName;
                }
            }
            
            if (bookmaker.containsKey("awayML")) {
                int awayML = (Integer) bookmaker.get("awayML");
                if (awayML > bestAwayML) {
                    bestAwayML = awayML;
                    bestAwayMLBook = bookmakerName;
                }
            }
            
            if (bookmaker.containsKey("homeSpreadOdds")) {
                int homeSpreadOdds = (Integer) bookmaker.get("homeSpreadOdds");
                if (homeSpreadOdds > bestHomeSpread) {
                    bestHomeSpread = homeSpreadOdds;
                    bestHomeSpreadBook = bookmakerName;
                }
            }

            if (bookmaker.containsKey("awaySpreadOdds")) {
                int awaySpreadOdds = (Integer) bookmaker.get("awaySpreadOdds");
                if (awaySpreadOdds > bestAwaySpread) {
                    bestAwaySpread = awaySpreadOdds;
                    bestAwaySpreadBook = bookmakerName;
                }
            }
            
            if (bookmaker.containsKey("overOdds")) {
                int over = (Integer) bookmaker.get("overOdds");
                if (over > bestOver) {
                    bestOver = over;
                    bestOverBook = bookmakerName;
                }
            }
            
            if (bookmaker.containsKey("underOdds")) {
                int under = (Integer) bookmaker.get("underOdds");
                if (under > bestUnder) {
                    bestUnder = under;
                    bestUnderBook = bookmakerName;
                }
            }
        }
        
        if (bestHomeML != Integer.MIN_VALUE) {
            bestOdds.put("homeML", bestHomeML);
            bestOdds.put("homeMLBook", bestHomeMLBook);
        }
        
        if (bestAwayML != Integer.MIN_VALUE) {
            bestOdds.put("awayML", bestAwayML);
            bestOdds.put("awayMLBook", bestAwayMLBook);
        }
        
        if (bestHomeSpread != Integer.MIN_VALUE) {
            bestOdds.put("homeSpreadOdds", bestHomeSpread);
            bestOdds.put("homeSpreadBook", bestHomeSpreadBook);
            // Include the spread point value from the best book's data
            for (Map<String, Object> bookmaker : allBookmakerOdds) {
                if (bestHomeSpreadBook.equals(bookmaker.get("bookmaker")) && bookmaker.containsKey("homeSpread")) {
                    bestOdds.put("homeSpread", bookmaker.get("homeSpread"));
                    break;
                }
            }
        }

        if (bestAwaySpread != Integer.MIN_VALUE) {
            bestOdds.put("awaySpreadOdds", bestAwaySpread);
            bestOdds.put("awaySpreadBook", bestAwaySpreadBook);
            for (Map<String, Object> bookmaker : allBookmakerOdds) {
                if (bestAwaySpreadBook.equals(bookmaker.get("bookmaker")) && bookmaker.containsKey("awaySpread")) {
                    bestOdds.put("awaySpread", bookmaker.get("awaySpread"));
                    break;
                }
            }
        }
        
        if (bestOver != Integer.MIN_VALUE) {
            bestOdds.put("overOdds", bestOver);
            bestOdds.put("overBook", bestOverBook);
        }
        
        if (bestUnder != Integer.MIN_VALUE) {
            bestOdds.put("underOdds", bestUnder);
            bestOdds.put("underBook", bestUnderBook);
        }
        
        return bestOdds;
    }
    
    private Map<String, Object> findBestOddsForBetType(Map<String, Object> game, String betType) {
        Map<String, Object> bestOdds = (Map<String, Object>) game.get("bestOdds");
        Map<String, Object> result = new HashMap<>();
        
        switch (betType.toUpperCase()) {
            case "MONEYLINE":
            case "ML":
                result.put("homeML", bestOdds.get("homeML"));
                result.put("homeMLBook", bestOdds.get("homeMLBook"));
                result.put("awayML", bestOdds.get("awayML"));
                result.put("awayMLBook", bestOdds.get("awayMLBook"));
                break;
            case "SPREAD":
                result.put("homeSpread", bestOdds.get("homeSpreadOdds"));
                result.put("homeSpreadBook", bestOdds.get("homeSpreadBook"));
                result.put("awaySpread", bestOdds.get("awaySpreadOdds"));
                result.put("awaySpreadBook", bestOdds.get("awaySpreadBook"));
                break;
            case "TOTAL":
            case "OVER/UNDER":
                result.put("over", bestOdds.get("overOdds"));
                result.put("overBook", bestOdds.get("overBook"));
                result.put("under", bestOdds.get("underOdds"));
                result.put("underBook", bestOdds.get("underBook"));
                break;
        }
        
        return result;
    }
    
    private boolean matchesTeams(String team1, String team2) {
        return team1.toLowerCase().contains(team2.toLowerCase()) ||
               team2.toLowerCase().contains(team1.toLowerCase());
    }

    /**
     * Validate that a single bookmaker's ML and spread are consistent.
     * If ML implies a spread that differs from actual spread by >10 points,
     * flag the bookmaker's data as potentially stale.
     */
    private void validateBookmakerConsistency(Map<String, Object> bookmakerOdds,
                                               String homeTeam, String awayTeam) {
        String bookmaker = (String) bookmakerOdds.get("bookmaker");

        // Check home side: homeML vs homeSpread
        if (bookmakerOdds.containsKey("homeML") && bookmakerOdds.containsKey("homeSpread")) {
            int homeML = (Integer) bookmakerOdds.get("homeML");
            double homeSpread = (Double) bookmakerOdds.get("homeSpread");
            double impliedSpread = mlToImpliedSpread(homeML);
            double discrepancy = Math.abs(impliedSpread - homeSpread);

            if (discrepancy > 10.0) {
                log.warn("STALE ODDS at {}: {} ML {} implies ~{} spread, but actual spread is {}. "
                    + "Discrepancy: {:.1f} pts. Bookmaker data may be stale.",
                    bookmaker, homeTeam, homeML,
                    String.format("%.1f", impliedSpread), homeSpread, discrepancy);
                bookmakerOdds.put("mlSpreadMismatch", true);
                bookmakerOdds.put("mlSpreadDiscrepancy", discrepancy);
            }
        }
    }

    /**
     * Validate that the assembled "best odds" (which may come from different bookmakers)
     * have consistent ML and spread. Adds warning flags to the bestOdds map if not.
     */
    private void validateBestOddsConsistency(Map<String, Object> bestOdds,
                                              String homeTeam, String awayTeam) {
        // Check home side
        if (bestOdds.containsKey("homeML") && bestOdds.containsKey("homeSpread")) {
            int homeML = (Integer) bestOdds.get("homeML");
            double homeSpread = ((Number) bestOdds.get("homeSpread")).doubleValue();
            String mlBook = (String) bestOdds.get("homeMLBook");
            String spreadBook = (String) bestOdds.get("homeSpreadBook");
            double impliedSpread = mlToImpliedSpread(homeML);
            double discrepancy = Math.abs(impliedSpread - homeSpread);

            if (discrepancy > 10.0) {
                log.warn("ML/SPREAD MISMATCH in best odds for {}: "
                    + "ML {} (from {}) implies ~{} spread, but spread is {} (from {}). "
                    + "Discrepancy: {:.1f} pts. These come from different bookmakers.",
                    homeTeam, homeML, mlBook,
                    String.format("%.1f", impliedSpread), homeSpread, spreadBook, discrepancy);
                bestOdds.put("mlSpreadWarning", true);
                bestOdds.put("mlSpreadDiscrepancy", discrepancy);
                bestOdds.put("mlImpliedSpread", impliedSpread);
            }
        }

        // Check away side
        if (bestOdds.containsKey("awayML") && bestOdds.containsKey("awaySpread")) {
            int awayML = (Integer) bestOdds.get("awayML");
            double awaySpread = ((Number) bestOdds.get("awaySpread")).doubleValue();
            String mlBook = (String) bestOdds.get("awayMLBook");
            String spreadBook = (String) bestOdds.get("awaySpreadBook");
            double impliedSpread = mlToImpliedSpread(awayML);
            double discrepancy = Math.abs(impliedSpread - awaySpread);

            if (discrepancy > 10.0) {
                log.warn("ML/SPREAD MISMATCH in best odds for {}: "
                    + "ML {} (from {}) implies ~{} spread, but spread is {} (from {}). "
                    + "Discrepancy: {:.1f} pts. These come from different bookmakers.",
                    awayTeam, awayML, mlBook,
                    String.format("%.1f", impliedSpread), awaySpread, spreadBook, discrepancy);
                bestOdds.put("mlSpreadWarning", true);
                bestOdds.put("mlSpreadDiscrepancy", discrepancy);
                bestOdds.put("mlImpliedSpread", impliedSpread);
            }
        }
    }

    /**
     * Convert American moneyline odds to an implied point spread.
     *
     * Steps:
     * 1. ML → implied win probability (no-vig)
     * 2. Win probability → approximate spread using ~4% per point (NBA/NFL average)
     *
     * This is an approximation for flagging large discrepancies, not for precise modeling.
     * Negative result = favorite (should have negative spread).
     */
    private double mlToImpliedSpread(int ml) {
        double impliedProb;
        if (ml < 0) {
            // Favorite: prob = |ML| / (|ML| + 100)
            impliedProb = Math.abs(ml) / (Math.abs(ml) + 100.0);
        } else if (ml > 0) {
            // Underdog: prob = 100 / (ML + 100)
            impliedProb = 100.0 / (ml + 100.0);
        } else {
            return 0.0; // Even money = pick'em
        }

        // Convert probability to spread: ~4% per point, centered at 50%
        // Negative spread = favorite, positive = underdog
        double impliedSpread = -((impliedProb - 0.5) / 0.04);
        return Math.round(impliedSpread * 2) / 2.0; // Round to nearest 0.5
    }
}
