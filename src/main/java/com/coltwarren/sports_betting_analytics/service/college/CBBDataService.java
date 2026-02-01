package com.coltwarren.sports_betting_analytics.service.college;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Service for interacting with College Basketball Data API
 * API Documentation: https://api.collegebasketballdata.com
 * GitHub: https://github.com/CFBD/cbbd-python
 *
 * This API is part of the CollegeFootballData.com ecosystem (CFBD)
 * Uses the same API key as CFBD but different base URL
 *
 * Key differences from football API:
 * - Uses "season" parameter instead of "year" for ratings/stats
 * - Uses "year" parameter for games
 * - Provides adjusted efficiency ratings (offensive + defensive)
 * - Home court advantage is ~3.5 points (vs 2.5 for football)
 */
@Service
public class CBBDataService {

    @Value("${cbb.api.key:}")
    private String apiKey;

    @Value("${cbb.api.url:https://api.collegebasketballdata.com}")
    private String apiUrl;

    private final WebClient webClient;

    public CBBDataService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Get basketball games for a specific season
     *
     * @param year Season year (e.g., 2025)
     * @param seasonType "regular" or "postseason"
     * @return List of basketball game data
     */
    public List<Map<String, Object>> getGames(int year, String seasonType) {
        try {
            String url = apiUrl + "/games?year=" + year;
            if (seasonType != null) {
                url += "&seasonType=" + seasonType;
            }

            System.out.println("🏀 Fetching basketball games: " + url);

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            List<Map<String, Object>> games = response != null ? Arrays.asList(response) : Collections.emptyList();
            System.out.println("✅ CBB API returned " + games.size() + " games");
            return games;

        } catch (Exception e) {
            System.err.println("❌ Error fetching basketball games: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get adjusted efficiency ratings for basketball teams
     * This is the RECOMMENDED method for betting analytics
     *
     * Returns offensive rating, defensive rating, and net rating for each team
     * Similar to KenPom ratings
     *
     * @param season Season year (e.g., 2025)
     * @param team Optional team name filter
     * @return List of team efficiency ratings
     */
    public List<Map<String, Object>> getAdjustedEfficiency(int season, String team) {
        try {
            String url = apiUrl + "/ratings/adjusted?season=" + season;
            if (team != null) {
                url += "&team=" + team;
            }

            System.out.println("🏀 Fetching adjusted efficiency ratings for " + season);

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            List<Map<String, Object>> ratings = response != null ? Arrays.asList(response) : Collections.emptyList();
            System.out.println("✅ CBB API returned adjusted efficiency for " + ratings.size() + " teams");
            return ratings;

        } catch (Exception e) {
            System.err.println("❌ Error fetching adjusted efficiency: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get SRS (Simple Rating System) ratings for basketball teams
     * Simpler alternative to adjusted efficiency
     *
     * @param season Season year (e.g., 2025)
     * @param team Optional team name filter
     * @return Map of team name to SRS rating
     */
    public Map<String, Double> getSRSRatings(int season, String team) {
        try {
            String url = apiUrl + "/ratings/srs?season=" + season;
            if (team != null) {
                url += "&team=" + team;
            }

            System.out.println("🏀 Fetching SRS ratings for " + season);

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            Map<String, Double> ratings = new HashMap<>();
            if (response != null) {
                for (Map<String, Object> teamData : response) {
                    String teamName = (String) teamData.get("team");
                    Object ratingObj = teamData.get("rating");
                    if (teamName != null && ratingObj != null) {
                        ratings.put(teamName, ((Number) ratingObj).doubleValue());
                    }
                }
            }

            System.out.println("✅ CBB API returned SRS ratings for " + ratings.size() + " teams");
            return ratings;

        } catch (Exception e) {
            System.err.println("❌ Error fetching SRS ratings: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Get team season statistics
     *
     * @param season Season year (e.g., 2025)
     * @param team Optional team name filter
     * @return List of team statistics
     */
    public List<Map<String, Object>> getStats(int season, String team) {
        try {
            String url = apiUrl + "/stats/team/season?season=" + season;
            if (team != null) {
                url += "&team=" + team;
            }

            System.out.println("🏀 Fetching team stats for " + season);

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            List<Map<String, Object>> stats = response != null ? Arrays.asList(response) : Collections.emptyList();
            System.out.println("✅ CBB API returned stats for " + stats.size() + " teams");
            return stats;

        } catch (Exception e) {
            System.err.println("❌ Error fetching team stats: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get basketball teams
     *
     * @param conference Optional conference filter (e.g., "ACC", "Big Ten")
     * @return List of basketball teams
     */
    public List<Map<String, Object>> getTeams(String conference) {
        try {
            String url = apiUrl + "/teams";
            if (conference != null) {
                url += "?conference=" + conference;
            }

            System.out.println("🏀 Fetching basketball teams");

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            List<Map<String, Object>> teams = response != null ? Arrays.asList(response) : Collections.emptyList();
            System.out.println("✅ CBB API returned " + teams.size() + " teams");
            return teams;

        } catch (Exception e) {
            System.err.println("❌ Error fetching teams: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Calculate win probability using adjusted efficiency ratings
     * This is MORE ACCURATE than using simple ratings
     *
     * Formula:
     * 1. Expected Team Points = (Team Offensive Rating + Opponent Defensive Rating) / 2
     * 2. Expected Opponent Points = (Opponent Offensive Rating + Team Defensive Rating) / 2
     * 3. Point Differential = Expected Team Points - Expected Opponent Points + Home Advantage
     * 4. Win Probability = 1 / (1 + e^(-k * Point Differential))
     *
     * @param teamOffensiveRating Team's offensive efficiency (points per 100 possessions)
     * @param teamDefensiveRating Team's defensive efficiency (points allowed per 100 possessions)
     * @param opponentOffensiveRating Opponent's offensive efficiency
     * @param opponentDefensiveRating Opponent's defensive efficiency
     * @param isHome True if team is playing at home
     * @return Win probability as decimal (0.0 to 1.0)
     */
    public double calculateWinProbabilityFromEfficiency(
            double teamOffensiveRating,
            double teamDefensiveRating,
            double opponentOffensiveRating,
            double opponentDefensiveRating,
            boolean isHome) {

        // Calculate expected points for each team based on efficiency matchup
        double teamExpectedPoints = (teamOffensiveRating + opponentDefensiveRating) / 2.0;
        double opponentExpectedPoints = (opponentOffensiveRating + teamDefensiveRating) / 2.0;

        // Apply home court advantage (approximately 3.5 points in college basketball)
        double homeAdvantage = isHome ? 3.5 : -3.5;

        // Calculate point differential
        double pointDifferential = (teamExpectedPoints - opponentExpectedPoints) + homeAdvantage;

        // Convert point differential to win probability using logistic regression
        // k = 0.15 for college basketball (slightly higher than football's 0.13)
        // This accounts for the lower scoring and tighter variance in basketball
        double winProbability = 1.0 / (1.0 + Math.exp(-0.15 * pointDifferential));

        return winProbability;
    }

    /**
     * Calculate win probability using SRS ratings (simpler method)
     * Use efficiency-based calculation for better accuracy when available
     *
     * @param teamSRS Team's SRS rating
     * @param opponentSRS Opponent's SRS rating
     * @param isHome True if team is playing at home
     * @return Win probability as decimal (0.0 to 1.0)
     */
    public double calculateWinProbabilityFromSRS(double teamSRS, double opponentSRS, boolean isHome) {
        // Home court advantage is approximately 3.5 points in college basketball
        double homeAdvantage = isHome ? 3.5 : -3.5;

        // Calculate rating differential
        double differential = teamSRS - opponentSRS + homeAdvantage;

        // Convert to win probability using logistic regression
        double winProbability = 1.0 / (1.0 + Math.exp(-0.15 * differential));

        return winProbability;
    }

    /**
     * Get current week of college basketball season
     * Basketball season runs approximately 20 weeks (November - March)
     *
     * @return Current week number (1-20)
     */
    public int getCurrentWeek() {
        // Simplified version - in production, calculate based on current date
        // Basketball season: Early November (week 1) through early March (week 20)
        return 10; // Mid-season default
    }
}
