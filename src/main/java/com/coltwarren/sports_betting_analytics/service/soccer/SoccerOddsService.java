package com.coltwarren.sports_betting_analytics.service.soccer;

import com.coltwarren.sports_betting_analytics.model.soccer.SoccerFixture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Soccer Odds Service - Fetches 3-Way betting odds from The Odds API
 *
 * SOCCER 3-WAY BETTING:
 * Unlike US sports (2-way moneyline), soccer has 3 outcomes:
 * - Home Win (1)
 * - Draw (X)
 * - Away Win (2)
 *
 * The draw outcome typically has ~25-30% implied probability.
 *
 * MISSOURI LEGAL COMPLIANCE:
 * Only shows odds from Missouri-licensed sportsbooks:
 * - DraftKings, FanDuel, BetMGM, Caesars, bet365, Fanatics, Circa, theScore
 *
 * Markets supported:
 * - h2h (3-way moneyline: Home/Draw/Away)
 * - totals (Over/Under goals, typically 2.5)
 * - btts (Both Teams To Score - Yes/No)
 */
@Slf4j
@Service
public class SoccerOddsService {

    @Value("${odds.api.key}")
    private String apiKey;

    private final WebClient oddsClient;

    // Missouri legal sportsbooks
    private static final Set<String> MISSOURI_LEGAL_BOOKS = Set.of(
        "draftkings",
        "fanduel",
        "betmgm",
        "mgm",
        "caesars",
        "bet365",
        "fanatics",
        "circa",
        "thescore",
        "score"
    );

    // Soccer league to sport_key mapping for The Odds API
    private static final Map<String, String> LEAGUE_TO_SPORT_KEY = Map.of(
        "MLS", "soccer_usa_mls",
        "PREMIER_LEAGUE", "soccer_epl",
        "CHAMPIONS_LEAGUE", "soccer_uefa_champs_league",
        "LA_LIGA", "soccer_spain_la_liga",
        "BUNDESLIGA", "soccer_germany_bundesliga",
        "SERIE_A", "soccer_italy_serie_a",
        "LIGUE_1", "soccer_france_ligue_one"
    );

    public SoccerOddsService() {
        this.oddsClient = WebClient.builder()
            .baseUrl("https://api.the-odds-api.com/v4")
            .build();
    }

    /**
     * Get 3-way soccer odds for a league
     *
     * Returns a map where key = "HomeTeam vs AwayTeam" and value = SoccerOdds object
     */
    public Map<String, SoccerOdds> getSoccerOdds(String sportKey) {
        Map<String, SoccerOdds> oddsMap = new HashMap<>();

        try {
            // Fetch odds with h2h (3-way) and totals markets
            List<Map<String, Object>> oddsData = oddsClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/sports/" + sportKey + "/odds")
                    .queryParam("apiKey", apiKey)
                    .queryParam("regions", "us")
                    .queryParam("markets", "h2h,totals")
                    .queryParam("oddsFormat", "american")
                    .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();

            if (oddsData == null || oddsData.isEmpty()) {
                log.info("No odds data returned for {}", sportKey);
                return oddsMap;
            }

            log.info("Found {} games with odds for {}", oddsData.size(), sportKey);

            for (Map<String, Object> game : oddsData) {
                try {
                    SoccerOdds odds = parseGameOdds(game);
                    if (odds != null) {
                        String gameKey = odds.getHomeTeam() + " vs " + odds.getAwayTeam();
                        oddsMap.put(gameKey, odds);
                    }
                } catch (Exception e) {
                    log.error("Error parsing game odds: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error fetching soccer odds for {}: {}", sportKey, e.getMessage());
        }

        return oddsMap;
    }

    /**
     * Get odds for a specific league by league name
     */
    public Map<String, SoccerOdds> getOddsForLeague(String league) {
        String sportKey = LEAGUE_TO_SPORT_KEY.get(league);
        if (sportKey == null) {
            log.error("Unknown league: {}", league);
            return Collections.emptyMap();
        }
        return getSoccerOdds(sportKey);
    }

    /**
     * Parse game odds from API response
     */
    private SoccerOdds parseGameOdds(Map<String, Object> game) {
        SoccerOdds odds = new SoccerOdds();

        String homeTeam = (String) game.get("home_team");
        String awayTeam = (String) game.get("away_team");
        String commenceTime = (String) game.get("commence_time");

        odds.setHomeTeam(homeTeam);
        odds.setAwayTeam(awayTeam);
        odds.setCommenceTime(commenceTime);

        List<Map<String, Object>> bookmakers = (List<Map<String, Object>>) game.get("bookmakers");
        if (bookmakers == null || bookmakers.isEmpty()) {
            return odds;
        }

        // Track best odds for each outcome
        double bestHomeOdds = Double.NEGATIVE_INFINITY;
        double bestDrawOdds = Double.NEGATIVE_INFINITY;
        double bestAwayOdds = Double.NEGATIVE_INFINITY;
        double bestOverOdds = Double.NEGATIVE_INFINITY;
        double bestUnderOdds = Double.NEGATIVE_INFINITY;

        String bestHomeBook = null;
        String bestDrawBook = null;
        String bestAwayBook = null;
        String bestOverBook = null;
        String bestUnderBook = null;

        Double goalLine = null;

        for (Map<String, Object> bookmaker : bookmakers) {
            String bookmakerName = (String) bookmaker.get("title");

            // STRICT FILTER: Only Missouri-licensed bookmakers
            if (!isMissouriLegal(bookmakerName)) {
                continue;
            }

            List<Map<String, Object>> markets = (List<Map<String, Object>>) bookmaker.get("markets");
            if (markets == null) continue;

            for (Map<String, Object> market : markets) {
                String marketKey = (String) market.get("key");
                List<Map<String, Object>> outcomes = (List<Map<String, Object>>) market.get("outcomes");

                if ("h2h".equals(marketKey)) {
                    // 3-way moneyline: Home/Draw/Away
                    for (Map<String, Object> outcome : outcomes) {
                        String name = (String) outcome.get("name");
                        Integer price = (Integer) outcome.get("price");

                        if (name.equals(homeTeam)) {
                            if (price > bestHomeOdds) {
                                bestHomeOdds = price;
                                bestHomeBook = bookmakerName;
                            }
                        } else if (name.equals(awayTeam)) {
                            if (price > bestAwayOdds) {
                                bestAwayOdds = price;
                                bestAwayBook = bookmakerName;
                            }
                        } else if ("Draw".equalsIgnoreCase(name)) {
                            if (price > bestDrawOdds) {
                                bestDrawOdds = price;
                                bestDrawBook = bookmakerName;
                            }
                        }
                    }
                } else if ("totals".equals(marketKey)) {
                    // Over/Under goals
                    for (Map<String, Object> outcome : outcomes) {
                        String name = (String) outcome.get("name");
                        Integer price = (Integer) outcome.get("price");
                        Number point = (Number) outcome.get("point");

                        if (point != null && goalLine == null) {
                            goalLine = point.doubleValue();
                        }

                        if ("Over".equalsIgnoreCase(name)) {
                            if (price > bestOverOdds) {
                                bestOverOdds = price;
                                bestOverBook = bookmakerName;
                            }
                        } else if ("Under".equalsIgnoreCase(name)) {
                            if (price > bestUnderOdds) {
                                bestUnderOdds = price;
                                bestUnderBook = bookmakerName;
                            }
                        }
                    }
                }
            }
        }

        // Set best odds
        if (bestHomeOdds != Double.NEGATIVE_INFINITY) {
            odds.setHomeWinOdds(bestHomeOdds);
            odds.setBestHomeBook(bestHomeBook);
        }
        if (bestDrawOdds != Double.NEGATIVE_INFINITY) {
            odds.setDrawOdds(bestDrawOdds);
            odds.setBestDrawBook(bestDrawBook);
        }
        if (bestAwayOdds != Double.NEGATIVE_INFINITY) {
            odds.setAwayWinOdds(bestAwayOdds);
            odds.setBestAwayBook(bestAwayBook);
        }
        if (bestOverOdds != Double.NEGATIVE_INFINITY) {
            odds.setOverOdds(bestOverOdds);
            odds.setBestOverBook(bestOverBook);
        }
        if (bestUnderOdds != Double.NEGATIVE_INFINITY) {
            odds.setUnderOdds(bestUnderOdds);
            odds.setBestUnderBook(bestUnderBook);
        }
        if (goalLine != null) {
            odds.setGoalLine(goalLine);
        }

        return odds;
    }

    /**
     * Check if a bookmaker is Missouri-legal
     */
    private boolean isMissouriLegal(String bookmakerName) {
        if (bookmakerName == null || bookmakerName.isEmpty()) {
            return false;
        }

        String normalized = bookmakerName.toLowerCase()
            .replace("sportsbook", "")
            .replace(" ", "")
            .replace(".", "")
            .replace("ag", "")
            .replace("the", "")
            .trim();

        return MISSOURI_LEGAL_BOOKS.stream()
            .anyMatch(legalBook -> normalized.contains(legalBook) || legalBook.contains(normalized));
    }

    /**
     * Inner class to hold soccer odds for a single game
     */
    public static class SoccerOdds {
        private String homeTeam;
        private String awayTeam;
        private String commenceTime;

        // 3-Way odds
        private Double homeWinOdds;
        private Double drawOdds;
        private Double awayWinOdds;
        private String bestHomeBook;
        private String bestDrawBook;
        private String bestAwayBook;

        // Totals
        private Double overOdds;
        private Double underOdds;
        private Double goalLine;
        private String bestOverBook;
        private String bestUnderBook;

        // Getters and Setters
        public String getHomeTeam() { return homeTeam; }
        public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }

        public String getAwayTeam() { return awayTeam; }
        public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }

        public String getCommenceTime() { return commenceTime; }
        public void setCommenceTime(String commenceTime) { this.commenceTime = commenceTime; }

        public Double getHomeWinOdds() { return homeWinOdds; }
        public void setHomeWinOdds(Double homeWinOdds) { this.homeWinOdds = homeWinOdds; }

        public Double getDrawOdds() { return drawOdds; }
        public void setDrawOdds(Double drawOdds) { this.drawOdds = drawOdds; }

        public Double getAwayWinOdds() { return awayWinOdds; }
        public void setAwayWinOdds(Double awayWinOdds) { this.awayWinOdds = awayWinOdds; }

        public String getBestHomeBook() { return bestHomeBook; }
        public void setBestHomeBook(String bestHomeBook) { this.bestHomeBook = bestHomeBook; }

        public String getBestDrawBook() { return bestDrawBook; }
        public void setBestDrawBook(String bestDrawBook) { this.bestDrawBook = bestDrawBook; }

        public String getBestAwayBook() { return bestAwayBook; }
        public void setBestAwayBook(String bestAwayBook) { this.bestAwayBook = bestAwayBook; }

        public Double getOverOdds() { return overOdds; }
        public void setOverOdds(Double overOdds) { this.overOdds = overOdds; }

        public Double getUnderOdds() { return underOdds; }
        public void setUnderOdds(Double underOdds) { this.underOdds = underOdds; }

        public Double getGoalLine() { return goalLine; }
        public void setGoalLine(Double goalLine) { this.goalLine = goalLine; }

        public String getBestOverBook() { return bestOverBook; }
        public void setBestOverBook(String bestOverBook) { this.bestOverBook = bestOverBook; }

        public String getBestUnderBook() { return bestUnderBook; }
        public void setBestUnderBook(String bestUnderBook) { this.bestUnderBook = bestUnderBook; }
    }
}
