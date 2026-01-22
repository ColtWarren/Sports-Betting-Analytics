package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.college.CFBDataService;
import com.coltwarren.sports_betting_analytics.service.college.CBBDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Test controller for CFBD API integration
 * Use these endpoints to verify the CFBD API is working correctly
 *
 * Test URLs:
 * - http://localhost:8080/test/cfbd/games?year=2025&week=1
 * - http://localhost:8080/test/cfbd/ratings?year=2025
 * - http://localhost:8080/test/cfbd/rankings?year=2025&week=1
 * - http://localhost:8080/test/cfbd/stats?year=2025&team=Alabama
 * - http://localhost:8080/test/cfbd/lines?year=2025&week=1
 * - http://localhost:8080/test/cfbd/talent?year=2025
 */
@RestController
@RequestMapping("/test/cfbd")
public class CFBDTestController {

    @Autowired
    private CFBDataService cfbDataService;

    @Autowired
    private CBBDataService cbbDataService;

    /**
     * Test: Get games for a specific year and week
     * Example: /test/cfbd/games?year=2025&week=1
     */
    @GetMapping("/games")
    public Map<String, Object> testGames(
            @RequestParam(defaultValue = "2025") int year,
            @RequestParam(required = false) Integer week,
            @RequestParam(required = false) String team
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("endpoint", "/games");
        result.put("year", year);
        result.put("week", week);
        result.put("team", team);

        List<Map<String, Object>> games = cfbDataService.getGames(year, week, team);

        result.put("gamesFound", games.size());
        result.put("games", games);

        return result;
    }

    /**
     * Test: Get SP+ ratings for all teams
     * Example: /test/cfbd/ratings?year=2025
     */
    @GetMapping("/ratings")
    public Map<String, Object> testRatings(
            @RequestParam(defaultValue = "2025") int year
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("endpoint", "/ratings/sp");
        result.put("year", year);

        Map<String, Double> ratings = cfbDataService.getSPRatings(year);

        result.put("teamsFound", ratings.size());
        result.put("ratings", ratings);

        // Show top 10 by SP+ rating
        List<Map.Entry<String, Double>> topTeams = ratings.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(10)
            .toList();

        Map<String, Double> top10 = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : topTeams) {
            top10.put(entry.getKey(), entry.getValue());
        }

        result.put("top10", top10);

        return result;
    }

    /**
     * Test: Get AP/Coaches Poll rankings
     * Example: /test/cfbd/rankings?year=2025&week=1
     */
    @GetMapping("/rankings")
    public Map<String, Object> testRankings(
            @RequestParam(defaultValue = "2025") int year,
            @RequestParam(defaultValue = "1") int week
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("endpoint", "/rankings");
        result.put("year", year);
        result.put("week", week);

        List<Map<String, Object>> rankings = cfbDataService.getRankings(year, week);

        result.put("rankings", rankings);

        return result;
    }

    /**
     * Test: Get team statistics
     * Example: /test/cfbd/stats?year=2025&team=Alabama
     */
    @GetMapping("/stats")
    public Map<String, Object> testStats(
            @RequestParam(defaultValue = "2025") int year,
            @RequestParam(defaultValue = "Alabama") String team
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("endpoint", "/stats/season");
        result.put("year", year);
        result.put("team", team);

        Map<String, Object> stats = cfbDataService.getTeamStats(year, team);

        result.put("stats", stats);

        return result;
    }

    /**
     * Test: Get betting lines for games
     * Example: /test/cfbd/lines?year=2025&week=1
     */
    @GetMapping("/lines")
    public Map<String, Object> testLines(
            @RequestParam(defaultValue = "2025") int year,
            @RequestParam(required = false) Integer week,
            @RequestParam(required = false) String team
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("endpoint", "/lines");
        result.put("year", year);
        result.put("week", week);
        result.put("team", team);

        List<Map<String, Object>> lines = cfbDataService.getGameLines(year, week, team);

        result.put("gamesWithLines", lines.size());
        result.put("lines", lines);

        return result;
    }

    /**
     * Test: Get team talent composite ratings
     * Example: /test/cfbd/talent?year=2025
     */
    @GetMapping("/talent")
    public Map<String, Object> testTalent(
            @RequestParam(defaultValue = "2025") int year
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("endpoint", "/talent");
        result.put("year", year);

        List<Map<String, Object>> talent = cfbDataService.getTeamTalent(year);

        result.put("teamsFound", talent.size());
        result.put("talent", talent);

        return result;
    }

    /**
     * Test: Calculate win probability using SP+ ratings
     * Example: /test/cfbd/winprob?team=Alabama&opponent=Georgia&home=true
     */
    @GetMapping("/winprob")
    public Map<String, Object> testWinProbability(
            @RequestParam(defaultValue = "2025") int year,
            @RequestParam(required = true) String team,
            @RequestParam(required = true) String opponent,
            @RequestParam(defaultValue = "true") boolean home
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("team", team);
        result.put("opponent", opponent);
        result.put("homeTeam", home ? team : opponent);

        // Get SP+ ratings
        Map<String, Double> ratings = cfbDataService.getSPRatings(year);

        Double teamSP = ratings.get(team);
        Double opponentSP = ratings.get(opponent);

        if (teamSP == null || opponentSP == null) {
            result.put("error", "One or both teams not found in SP+ ratings");
            result.put("availableTeams", ratings.keySet().stream().sorted().limit(20).toList());
            return result;
        }

        double winProbability = cfbDataService.calculateWinProbability(teamSP, opponentSP, home);

        result.put("teamSP", teamSP);
        result.put("opponentSP", opponentSP);
        result.put("spDifferential", teamSP - opponentSP);
        result.put("homeAdvantage", home ? 2.5 : -2.5);
        result.put("adjustedDifferential", teamSP - opponentSP + (home ? 2.5 : -2.5));
        result.put("winProbability", String.format("%.1f%%", winProbability * 100));
        result.put("impliedOdds", convertProbabilityToOdds(winProbability));

        return result;
    }

    /**
     * Helper: Convert win probability to American odds
     */
    private int convertProbabilityToOdds(double probability) {
        if (probability >= 0.5) {
            // Favorite (negative odds)
            return (int) Math.round(-100 * probability / (1 - probability));
        } else {
            // Underdog (positive odds)
            return (int) Math.round(100 * (1 - probability) / probability);
        }
    }

    /**
     * Test: Get full status of CFBD integration
     * Example: /test/cfbd/status
     */
    @GetMapping("/status")
    public Map<String, Object> testStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", "CFBD API Integration");
        result.put("status", "Running");

        // Test each endpoint
        try {
            List<Map<String, Object>> games = cfbDataService.getGames(2025, 1, null);
            result.put("gamesEndpoint", games.isEmpty() ? "⚠️ No data" : "✅ Working");
        } catch (Exception e) {
            result.put("gamesEndpoint", "❌ Error: " + e.getMessage());
        }

        try {
            Map<String, Double> ratings = cfbDataService.getSPRatings(2025);
            result.put("ratingsEndpoint", ratings.isEmpty() ? "⚠️ No data" : "✅ Working (" + ratings.size() + " teams)");
        } catch (Exception e) {
            result.put("ratingsEndpoint", "❌ Error: " + e.getMessage());
        }

        try {
            List<Map<String, Object>> rankings = cfbDataService.getRankings(2025, 1);
            result.put("rankingsEndpoint", rankings.isEmpty() ? "⚠️ No data" : "✅ Working");
        } catch (Exception e) {
            result.put("rankingsEndpoint", "❌ Error: " + e.getMessage());
        }

        result.put("testUrls", Map.of(
            "games", "http://localhost:8080/test/cfbd/games?year=2025&week=1",
            "ratings", "http://localhost:8080/test/cfbd/ratings?year=2025",
            "winprob", "http://localhost:8080/test/cfbd/winprob?year=2025&team=Alabama&opponent=Georgia&home=true"
        ));

        return result;
    }

    // ===========================
    // BASKETBALL TEST ENDPOINTS
    // ===========================

    /**
     * Test: Get basketball SP+ ratings (Pomeroy-style ratings)
     * Example: /test/cfbd/basketball/ratings?year=2025
     */
    @GetMapping("/basketball/ratings")
    public Map<String, Object> testBasketballRatings(
            @RequestParam(defaultValue = "2025") int year
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("endpoint", "/ratings/srs");
        result.put("sport", "College Basketball");
        result.put("season", year);

        Map<String, Double> ratings = cbbDataService.getSRSRatings(year, null);

        result.put("teamsFound", ratings.size());
        result.put("dataQuality", ratings.isEmpty() ? "❌ No data" : "✅ Data available");

        // Show top 25 teams by rating
        List<Map.Entry<String, Double>> topTeams = ratings.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(25)
            .toList();

        Map<String, Object> top25 = new LinkedHashMap<>();
        int rank = 1;
        for (Map.Entry<String, Double> entry : topTeams) {
            top25.put("#" + rank + " " + entry.getKey(),
                     String.format("%.2f rating", entry.getValue()));
            rank++;
        }

        result.put("top25Teams", top25);
        result.put("allRatings", ratings);

        return result;
    }

    /**
     * Test: Get basketball games
     * Example: /test/cfbd/basketball/games?year=2025&seasonType=regular
     */
    @GetMapping("/basketball/games")
    public Map<String, Object> testBasketballGames(
            @RequestParam(defaultValue = "2025") int year,
            @RequestParam(defaultValue = "regular") String seasonType
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("endpoint", "/games");
        result.put("sport", "College Basketball");
        result.put("year", year);
        result.put("seasonType", seasonType);

        List<Map<String, Object>> games = cbbDataService.getGames(year, seasonType);

        result.put("gamesFound", games.size());
        result.put("dataQuality", games.isEmpty() ? "❌ No data" : "✅ Data available");

        // Show first 10 games as examples
        List<Map<String, Object>> sampleGames = games.stream().limit(10).toList();
        result.put("sampleGames", sampleGames);

        // Summary statistics
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalGames", games.size());

        if (!games.isEmpty()) {
            // Count games by status if available
            Map<String, Long> statusCounts = games.stream()
                .filter(g -> g.containsKey("status"))
                .collect(java.util.stream.Collectors.groupingBy(
                    g -> (String) g.get("status"),
                    java.util.stream.Collectors.counting()
                ));
            summary.put("gamesByStatus", statusCounts);
        }

        result.put("summary", summary);

        return result;
    }

    /**
     * Note: Basketball AP Poll rankings are not available through the CBB Data API
     * Use SRS ratings or adjusted efficiency ratings instead for basketball analytics
     */

    /**
     * Test: Get basketball team statistics
     * Example: /test/cfbd/basketball/stats?year=2025&team=Duke
     */
    @GetMapping("/basketball/stats")
    public Map<String, Object> testBasketballStats(
            @RequestParam(defaultValue = "2025") int year,
            @RequestParam(required = false) String team
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("endpoint", "/stats/season");
        result.put("sport", "College Basketball");
        result.put("year", year);
        result.put("team", team);

        List<Map<String, Object>> stats = cbbDataService.getStats(year, team);

        result.put("teamsFound", stats.size());
        result.put("dataQuality", stats.isEmpty() ? "❌ No data" : "✅ Data available");

        // Show first 10 teams as examples
        List<Map<String, Object>> sampleStats = stats.stream().limit(10).toList();
        result.put("sampleStats", sampleStats);
        result.put("allStats", stats);

        return result;
    }

    /**
     * Test: Basketball integration status check
     * Example: /test/cfbd/basketball/status
     */
    @GetMapping("/basketball/status")
    public Map<String, Object> testBasketballStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", "CBB Basketball API Integration");
        result.put("status", "Running");
        result.put("sport", "College Basketball (CBB)");
        result.put("apiUrl", "https://api.collegebasketballdata.com");

        // Test each basketball endpoint
        try {
            Map<String, Double> ratings = cbbDataService.getSRSRatings(2025, null);
            result.put("ratingsEndpoint", ratings.isEmpty() ? "⚠️ No data" : "✅ Working (" + ratings.size() + " teams)");
        } catch (Exception e) {
            result.put("ratingsEndpoint", "❌ Error: " + e.getMessage());
        }

        try {
            List<Map<String, Object>> games = cbbDataService.getGames(2025, "regular");
            result.put("gamesEndpoint", games.isEmpty() ? "⚠️ No data" : "✅ Working (" + games.size() + " games)");
        } catch (Exception e) {
            result.put("gamesEndpoint", "❌ Error: " + e.getMessage());
        }

        try {
            List<Map<String, Object>> stats = cbbDataService.getStats(2025, null);
            result.put("statsEndpoint", stats.isEmpty() ? "⚠️ No data" : "✅ Working (" + stats.size() + " teams)");
        } catch (Exception e) {
            result.put("statsEndpoint", "❌ Error: " + e.getMessage());
        }

        result.put("testUrls", Map.of(
            "ratings", "http://localhost:8080/test/cfbd/basketball/ratings?year=2025",
            "games", "http://localhost:8080/test/cfbd/basketball/games?year=2025",
            "stats", "http://localhost:8080/test/cfbd/basketball/stats?year=2025"
        ));

        return result;
    }
}
