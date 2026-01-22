package com.coltwarren.sports_betting_analytics.service.college;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Service for interacting with College Football Data API (CFBD)
 * API Documentation: https://collegefootballdata.com/api/docs
 *
 * This single API key works for both College Football (CFB) and College Basketball (CBB)
 */
@Service
public class CFBDataService {

    @Value("${cfbd.api.key}")
    private String apiKey;

    @Value("${cfbd.api.url}")
    private String apiUrl;

    private final WebClient webClient;

    public CFBDataService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Get games for a specific year, week, and optionally team
     *
     * @param year Season year (e.g., 2025, 2026)
     * @param week Week number (1-15 for regular season, null for all weeks)
     * @param team Team name (optional, null for all teams)
     * @return List of game data maps
     */
    public List<Map<String, Object>> getGames(int year, Integer week, String team) {
        try {
            String url = apiUrl + "/games?year=" + year;

            if (week != null) {
                url += "&week=" + week;
            }

            if (team != null) {
                url += "&team=" + team;
            }

            System.out.println("🏈 Fetching CFBD games: " + url);

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            List<Map<String, Object>> games = response != null ? Arrays.asList(response) : Collections.emptyList();
            System.out.println("✅ CFBD returned " + games.size() + " games");

            return games;

        } catch (Exception e) {
            System.err.println("❌ Error fetching CFBD games: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Get SP+ ratings for all teams in a given year
     * SP+ is the most predictive college football rating system
     *
     * @param year Season year
     * @return Map of team name to SP+ rating
     */
    public Map<String, Double> getSPRatings(int year) {
        try {
            String url = apiUrl + "/ratings/sp?year=" + year;

            System.out.println("📊 Fetching SP+ ratings for " + year);

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            Map<String, Double> ratings = new HashMap<>();

            if (response != null) {
                for (Map<String, Object> team : response) {
                    String teamName = (String) team.get("team");
                    Double rating = ((Number) team.get("rating")).doubleValue();
                    ratings.put(teamName, rating);
                }
            }

            System.out.println("✅ CFBD returned SP+ ratings for " + ratings.size() + " teams");
            return ratings;

        } catch (Exception e) {
            System.err.println("❌ Error fetching SP+ ratings: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    /**
     * Get current AP Poll and Coaches Poll rankings
     *
     * @param year Season year
     * @param week Week number
     * @return List of ranking data
     */
    public List<Map<String, Object>> getRankings(int year, int week) {
        try {
            String url = apiUrl + "/rankings?year=" + year + "&week=" + week;

            System.out.println("🏆 Fetching rankings for year " + year + " week " + week);

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            List<Map<String, Object>> rankings = response != null ? Arrays.asList(response) : Collections.emptyList();
            System.out.println("✅ CFBD returned rankings");

            return rankings;

        } catch (Exception e) {
            System.err.println("❌ Error fetching rankings: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Get team statistics for a season
     *
     * @param year Season year
     * @param team Team name
     * @return Map of team statistics
     */
    public Map<String, Object> getTeamStats(int year, String team) {
        try {
            String url = apiUrl + "/stats/season?year=" + year + "&team=" + team;

            System.out.println("📈 Fetching stats for " + team + " in " + year);

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            Map<String, Object> stats = response != null && response.length > 0 ? response[0] : Collections.emptyMap();
            System.out.println("✅ CFBD returned team stats");

            return stats;

        } catch (Exception e) {
            System.err.println("❌ Error fetching team stats: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    /**
     * Get advanced team statistics (success rates, explosiveness, etc.)
     *
     * @param year Season year
     * @param team Team name (optional)
     * @return List of advanced stats
     */
    public List<Map<String, Object>> getAdvancedStats(int year, String team) {
        try {
            String url = apiUrl + "/stats/season/advanced?year=" + year;

            if (team != null) {
                url += "&team=" + team;
            }

            System.out.println("📊 Fetching advanced stats for " + year);

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            List<Map<String, Object>> stats = response != null ? Arrays.asList(response) : Collections.emptyList();
            System.out.println("✅ CFBD returned advanced stats");

            return stats;

        } catch (Exception e) {
            System.err.println("❌ Error fetching advanced stats: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Get historical betting lines for games
     * This is extremely valuable for CLV (Closing Line Value) tracking
     *
     * @param year Season year
     * @param week Week number (optional)
     * @param team Team name (optional)
     * @return List of game lines
     */
    public List<Map<String, Object>> getGameLines(int year, Integer week, String team) {
        try {
            String url = apiUrl + "/lines?year=" + year;

            if (week != null) {
                url += "&week=" + week;
            }

            if (team != null) {
                url += "&team=" + team;
            }

            System.out.println("💰 Fetching betting lines for " + year);

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            List<Map<String, Object>> lines = response != null ? Arrays.asList(response) : Collections.emptyList();
            System.out.println("✅ CFBD returned " + lines.size() + " games with betting lines");

            return lines;

        } catch (Exception e) {
            System.err.println("❌ Error fetching game lines: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Get team talent composite ratings
     * Based on recruiting rankings - useful for identifying talent mismatches
     *
     * @param year Season year
     * @return List of team talent ratings
     */
    public List<Map<String, Object>> getTeamTalent(int year) {
        try {
            String url = apiUrl + "/talent?year=" + year;

            System.out.println("⭐ Fetching team talent ratings for " + year);

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            List<Map<String, Object>> talent = response != null ? Arrays.asList(response) : Collections.emptyList();
            System.out.println("✅ CFBD returned talent ratings for " + talent.size() + " teams");

            return talent;

        } catch (Exception e) {
            System.err.println("❌ Error fetching team talent: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Calculate win probability based on SP+ rating differential
     * This is a key method for improving Kelly Criterion calculations
     *
     * @param teamSP Team's SP+ rating
     * @param opponentSP Opponent's SP+ rating
     * @param isHome True if team is home (adds ~2.5 point advantage)
     * @return Win probability as decimal (0.0 to 1.0)
     */
    public double calculateWinProbability(double teamSP, double opponentSP, boolean isHome) {
        // Home field advantage is worth approximately 2.5 points in college football
        double homeAdvantage = isHome ? 2.5 : -2.5;

        // Calculate SP+ differential (positive = team is favored)
        double differential = teamSP - opponentSP + homeAdvantage;

        // Convert SP+ differential to win probability using logistic regression
        // Formula calibrated from historical SP+ vs actual results
        // 0.13 is the coefficient that best fits historical data
        double winProbability = 1.0 / (1.0 + Math.exp(-0.13 * differential));

        return winProbability;
    }

    /**
     * Get current week of CFB season
     *
     * @return Current week number (1-15)
     */
    public int getCurrentWeek() {
        // This is a simplified version - in production you'd calculate based on current date
        // For now, return a reasonable default
        return 1;
    }
}
