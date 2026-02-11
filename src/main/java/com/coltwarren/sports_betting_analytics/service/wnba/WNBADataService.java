package com.coltwarren.sports_betting_analytics.service.wnba;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WNBA Data Service
 *
 * Fetches WNBA team stats, games, standings, and player data from BALLDONTLIE API.
 *
 * API Details:
 * - Base URL: https://api.balldontlie.io/wnba/v1
 * - Auth: Header "Authorization: YOUR_API_KEY" (no "Bearer" prefix)
 * - Free tier: 5 requests/minute
 *
 * Key Endpoints:
 * - GET /teams - All WNBA teams
 * - GET /games - Games/scores (supports ?seasons[]=2024)
 * - GET /standings - Standings
 * - GET /players - Player info
 * - GET /player_injuries - Current injuries
 * - GET /player_season_stats - Season averages
 *
 * WNBA Season: May through October
 */
@Service
@Slf4j
public class WNBADataService {

    @Value("${balldontlie.api.key:}")
    private String apiKey;

    @Value("${balldontlie.api.base-url:https://api.balldontlie.io/wnba/v1}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, Map<String, Object>> teamsCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> statsCache = new ConcurrentHashMap<>();

    // All 12 WNBA Teams (2024 season)
    private static final List<String> WNBA_TEAMS = List.of(
        "Atlanta Dream", "Chicago Sky", "Connecticut Sun", "Dallas Wings",
        "Indiana Fever", "Las Vegas Aces", "Los Angeles Sparks", "Minnesota Lynx",
        "New York Liberty", "Phoenix Mercury", "Seattle Storm", "Washington Mystics"
    );

    // Championship contenders (for tier-based analysis)
    private static final List<String> ELITE_TEAMS = List.of(
        "Las Vegas Aces", "New York Liberty", "Connecticut Sun", "Minnesota Lynx"
    );

    private static final List<String> PLAYOFF_TEAMS = List.of(
        "Seattle Storm", "Phoenix Mercury", "Chicago Sky", "Indiana Fever"
    );

    /**
     * Get all WNBA teams
     */
    public List<Map<String, Object>> getTeams() {
        if (isApiConfigured()) {
            return fetchTeamsFromApi();
        }
        return generateSimulatedTeams();
    }

    /**
     * Fetch teams from BALLDONTLIE API
     */
    private List<Map<String, Object>> fetchTeamsFromApi() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey); // No "Bearer" prefix for BALLDONTLIE
            headers.set("Accept", "application/json");

            String url = baseUrl + "/teams";
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                List<Map<String, Object>> teams = (List<Map<String, Object>>) response.getBody().get("data");
                log.info("Successfully fetched {} WNBA teams from API", teams.size());
                return teams;
            }
        } catch (Exception e) {
            log.warn("Error fetching WNBA teams from API: {}", e.getMessage());
        }

        return generateSimulatedTeams();
    }

    /**
     * Generate simulated teams when API is not configured
     */
    private List<Map<String, Object>> generateSimulatedTeams() {
        List<Map<String, Object>> teams = new ArrayList<>();
        int id = 1;
        for (String teamName : WNBA_TEAMS) {
            Map<String, Object> team = new LinkedHashMap<>();
            team.put("id", id++);
            team.put("name", teamName);
            team.put("city", teamName.substring(0, teamName.lastIndexOf(" ")));
            team.put("abbreviation", getTeamAbbreviation(teamName));
            team.put("conference", id <= 6 ? "Eastern" : "Western");
            teams.add(team);
        }
        return teams;
    }

    /**
     * Get games for a season
     */
    public List<Map<String, Object>> getGames(int season) {
        if (isApiConfigured()) {
            return fetchGamesFromApi(season);
        }
        return generateSimulatedGames(season);
    }

    /**
     * Fetch games from BALLDONTLIE API
     */
    private List<Map<String, Object>> fetchGamesFromApi(int season) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            headers.set("Accept", "application/json");

            String url = baseUrl + "/games?seasons[]=" + season;
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                List<Map<String, Object>> games = (List<Map<String, Object>>) response.getBody().get("data");
                log.info("Successfully fetched {} WNBA games for season {}", games.size(), season);
                return games;
            }
        } catch (Exception e) {
            log.warn("Error fetching WNBA games from API: {}", e.getMessage());
        }

        return generateSimulatedGames(season);
    }

    /**
     * Generate simulated games when API is not configured
     */
    private List<Map<String, Object>> generateSimulatedGames(int season) {
        List<Map<String, Object>> games = new ArrayList<>();
        Random rand = new Random(season);

        // Generate sample upcoming games
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 10; i++) {
            int homeIdx = rand.nextInt(WNBA_TEAMS.size());
            int awayIdx = (homeIdx + 1 + rand.nextInt(WNBA_TEAMS.size() - 1)) % WNBA_TEAMS.size();

            Map<String, Object> game = new LinkedHashMap<>();
            game.put("id", i + 1);
            game.put("date", today.plusDays(i).toString());
            game.put("season", season);
            game.put("status", i < 3 ? "Final" : "Scheduled");
            game.put("home_team", Map.of("name", WNBA_TEAMS.get(homeIdx)));
            game.put("visitor_team", Map.of("name", WNBA_TEAMS.get(awayIdx)));

            if (i < 3) {
                // Completed games with scores
                game.put("home_team_score", 75 + rand.nextInt(25));
                game.put("visitor_team_score", 75 + rand.nextInt(25));
            }

            games.add(game);
        }

        return games;
    }

    /**
     * Get current standings
     */
    public List<Map<String, Object>> getStandings() {
        if (isApiConfigured()) {
            return fetchStandingsFromApi();
        }
        return generateSimulatedStandings();
    }

    /**
     * Fetch standings from BALLDONTLIE API
     */
    private List<Map<String, Object>> fetchStandingsFromApi() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            headers.set("Accept", "application/json");

            String url = baseUrl + "/standings";
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                List<Map<String, Object>> standings = (List<Map<String, Object>>) response.getBody().get("data");
                log.info("Successfully fetched WNBA standings");
                return standings;
            }
        } catch (Exception e) {
            log.warn("Error fetching WNBA standings from API: {}", e.getMessage());
        }

        return generateSimulatedStandings();
    }

    /**
     * Generate simulated standings when API is not configured
     */
    private List<Map<String, Object>> generateSimulatedStandings() {
        List<Map<String, Object>> standings = new ArrayList<>();
        Random rand = new Random(LocalDate.now().getYear());

        List<String> shuffledTeams = new ArrayList<>(WNBA_TEAMS);
        // Sort by tier first
        shuffledTeams.sort((a, b) -> {
            int tierA = ELITE_TEAMS.contains(a) ? 1 : (PLAYOFF_TEAMS.contains(a) ? 2 : 3);
            int tierB = ELITE_TEAMS.contains(b) ? 1 : (PLAYOFF_TEAMS.contains(b) ? 2 : 3);
            return tierA - tierB;
        });

        int rank = 1;
        for (String team : shuffledTeams) {
            int tier = ELITE_TEAMS.contains(team) ? 1 : (PLAYOFF_TEAMS.contains(team) ? 2 : 3);
            int wins = switch (tier) {
                case 1 -> 24 + rand.nextInt(8);  // Elite: 24-31 wins
                case 2 -> 18 + rand.nextInt(8);  // Playoff: 18-25 wins
                default -> 10 + rand.nextInt(12); // Others: 10-21 wins
            };
            int losses = 40 - wins; // 40-game season

            Map<String, Object> standing = new LinkedHashMap<>();
            standing.put("rank", rank++);
            standing.put("team", team);
            standing.put("abbreviation", getTeamAbbreviation(team));
            standing.put("wins", wins);
            standing.put("losses", losses);
            standing.put("winPct", String.format("%.3f", (double) wins / 40));
            standing.put("gamesBack", rank == 1 ? "-" : String.valueOf((rank - 1) * 0.5));
            standing.put("streak", (rand.nextBoolean() ? "W" : "L") + (1 + rand.nextInt(5)));

            standings.add(standing);
        }

        return standings;
    }

    /**
     * Get team statistics
     */
    @Cacheable(value = "wnbaTeamStatsCache", key = "#teamName")
    public Map<String, Object> getTeamStats(String teamName) {
        if (statsCache.containsKey(teamName.toLowerCase())) {
            return statsCache.get(teamName.toLowerCase());
        }

        Map<String, Object> stats;
        if (isApiConfigured()) {
            stats = fetchTeamStatsFromApi(teamName);
        } else {
            stats = generateSimulatedTeamStats(teamName);
        }

        if (stats != null) {
            statsCache.put(teamName.toLowerCase(), stats);
        }

        return stats;
    }

    /**
     * Fetch team stats from BALLDONTLIE API
     */
    private Map<String, Object> fetchTeamStatsFromApi(String teamName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            headers.set("Accept", "application/json");

            // First get team ID
            String teamsUrl = baseUrl + "/teams";
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> teamsResponse = restTemplate.exchange(
                teamsUrl, HttpMethod.GET, entity, Map.class);

            if (teamsResponse.getBody() != null && teamsResponse.getBody().containsKey("data")) {
                List<Map<String, Object>> teams = (List<Map<String, Object>>) teamsResponse.getBody().get("data");

                for (Map<String, Object> team : teams) {
                    String name = (String) team.get("name");
                    if (name != null && name.toLowerCase().contains(teamName.toLowerCase())) {
                        log.info("Found WNBA team: {}", name);
                        // Could fetch more detailed stats here
                        return generateSimulatedTeamStats(name);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error fetching WNBA team stats for {}: {}", teamName, e.getMessage());
        }

        return generateSimulatedTeamStats(teamName);
    }

    /**
     * Generate simulated team stats
     */
    private Map<String, Object> generateSimulatedTeamStats(String teamName) {
        Map<String, Object> stats = new LinkedHashMap<>();
        Random rand = new Random(teamName.hashCode());

        int tier = ELITE_TEAMS.contains(teamName) ? 1 : (PLAYOFF_TEAMS.contains(teamName) ? 2 : 3);

        stats.put("teamName", teamName);
        stats.put("abbreviation", getTeamAbbreviation(teamName));
        stats.put("tier", tier);

        // Record based on tier
        int wins = switch (tier) {
            case 1 -> 24 + rand.nextInt(8);
            case 2 -> 18 + rand.nextInt(8);
            default -> 10 + rand.nextInt(12);
        };
        stats.put("wins", wins);
        stats.put("losses", 40 - wins);
        stats.put("winPct", (double) wins / 40);

        // Offensive stats
        stats.put("pointsPerGame", 78.0 + tier * -3 + rand.nextDouble() * 8);
        stats.put("fieldGoalPct", 42.0 + rand.nextDouble() * 6);
        stats.put("threePointPct", 32.0 + rand.nextDouble() * 8);
        stats.put("freeThrowPct", 75.0 + rand.nextDouble() * 10);

        // Defensive stats
        stats.put("pointsAllowedPerGame", 75.0 + tier * 2 + rand.nextDouble() * 8);
        stats.put("reboundsPerGame", 32.0 + rand.nextDouble() * 6);
        stats.put("assistsPerGame", 18.0 + rand.nextDouble() * 6);
        stats.put("stealsPerGame", 6.0 + rand.nextDouble() * 4);
        stats.put("blocksPerGame", 3.0 + rand.nextDouble() * 3);

        // Betting stats
        int atsWins = (int) (18 + (rand.nextDouble() - 0.5) * 10);
        stats.put("atsWins", atsWins);
        stats.put("atsLosses", 40 - atsWins);
        stats.put("atsPercentage", (double) atsWins / 40 * 100);

        int overHits = 18 + rand.nextInt(6);
        stats.put("overHits", overHits);
        stats.put("underHits", 40 - overHits);

        // Betting trend
        double atsPct = (double) atsWins / 40 * 100;
        if (atsPct > 55) {
            stats.put("bettingTrend", "HOT");
        } else if (atsPct < 45) {
            stats.put("bettingTrend", "COLD");
        } else {
            stats.put("bettingTrend", "NEUTRAL");
        }

        // Key insights
        List<String> insights = new ArrayList<>();
        if (ELITE_TEAMS.contains(teamName)) {
            insights.add("Championship contender - public often overvalues");
        }
        if (atsPct > 55) {
            insights.add("ATS: " + String.format("%.1f%%", atsPct) + " - covering spreads consistently");
        }
        if (overHits > 22) {
            insights.add("OVER tendency - high-scoring games");
        } else if (overHits < 18) {
            insights.add("UNDER tendency - defensive focus");
        }
        stats.put("keyInsights", insights);

        return stats;
    }

    /**
     * Get team abbreviation
     */
    private String getTeamAbbreviation(String teamName) {
        Map<String, String> abbrevs = Map.ofEntries(
            Map.entry("Atlanta Dream", "ATL"),
            Map.entry("Chicago Sky", "CHI"),
            Map.entry("Connecticut Sun", "CON"),
            Map.entry("Dallas Wings", "DAL"),
            Map.entry("Indiana Fever", "IND"),
            Map.entry("Las Vegas Aces", "LVA"),
            Map.entry("Los Angeles Sparks", "LAS"),
            Map.entry("Minnesota Lynx", "MIN"),
            Map.entry("New York Liberty", "NYL"),
            Map.entry("Phoenix Mercury", "PHO"),
            Map.entry("Seattle Storm", "SEA"),
            Map.entry("Washington Mystics", "WAS")
        );
        return abbrevs.getOrDefault(teamName, teamName.substring(0, 3).toUpperCase());
    }

    /**
     * Check if API is configured
     */
    public boolean isApiConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * Get all WNBA team names
     */
    public List<String> getAllTeamNames() {
        return new ArrayList<>(WNBA_TEAMS);
    }

    /**
     * Clear all caches
     */
    public void clearCache() {
        teamsCache.clear();
        statsCache.clear();
    }
}
