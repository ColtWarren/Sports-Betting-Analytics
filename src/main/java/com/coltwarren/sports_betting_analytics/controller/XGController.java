package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.model.xg.TeamXGStats;
import com.coltwarren.sports_betting_analytics.model.xg.MatchXGAnalysis;
import com.coltwarren.sports_betting_analytics.service.xg.XGDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * xG Controller
 *
 * REST endpoints for Expected Goals (xG) analytics.
 * Provides statistical edge analysis for soccer betting.
 */
@RestController
@RequestMapping("/api/xg")
@Slf4j
public class XGController {

    @Autowired
    private XGDataService xgDataService;

    /**
     * Get xG stats for a team
     * Example: GET /api/xg/team/Liverpool?league=EPL
     */
    @GetMapping("/team/{teamName}")
    public ResponseEntity<?> getTeamXG(
            @PathVariable String teamName,
            @RequestParam(defaultValue = "EPL") String league) {
        TeamXGStats stats = xgDataService.getTeamXGStats(teamName, league);
        return ResponseEntity.ok(stats);
    }

    /**
     * Analyze a match using xG
     * Example: GET /api/xg/match?homeTeam=Arsenal&awayTeam=Chelsea&league=EPL
     */
    @GetMapping("/match")
    public ResponseEntity<?> analyzeMatch(
            @RequestParam String homeTeam,
            @RequestParam String awayTeam,
            @RequestParam(defaultValue = "EPL") String league) {
        MatchXGAnalysis analysis = xgDataService.analyzeMatch(homeTeam, awayTeam, league);
        return ResponseEntity.ok(analysis);
    }

    /**
     * Get teams with regression signals
     * Example: GET /api/xg/regression-signals?league=EPL
     */
    @GetMapping("/regression-signals")
    public ResponseEntity<?> getRegressionSignals(
            @RequestParam(required = false) String league) {
        List<TeamXGStats> teams = xgDataService.getTeamsWithRegressionSignals(league);
        return ResponseEntity.ok(Map.of(
            "count", teams.size(),
            "teams", teams
        ));
    }

    /**
     * Test with sample matches across leagues
     * Example: GET /api/xg/test
     */
    @GetMapping("/test")
    public ResponseEntity<?> testXG() {
        List<Map<String, Object>> analyses = new ArrayList<>();

        // Test several matches across leagues
        String[][] matches = {
            {"Liverpool", "Manchester City", "EPL"},
            {"Arsenal", "Chelsea", "EPL"},
            {"Nottingham Forest", "Brighton", "EPL"},
            {"Real Madrid", "Barcelona", "LA_LIGA"},
            {"Bayern Munich", "Dortmund", "BUNDESLIGA"}
        };

        for (String[] match : matches) {
            MatchXGAnalysis analysis = xgDataService.analyzeMatch(match[0], match[1], match[2]);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("match", match[0] + " vs " + match[1]);
            result.put("league", match[2]);
            result.put("recommendation", analysis.getXgRecommendation());
            result.put("confidence", analysis.getXgConfidence());
            result.put("reason", analysis.getXgReason());
            result.put("projectedTotal", analysis.getProjectedTotal());
            result.put("homeXGPerGame", analysis.getHomeXGStats().getXGPerGame());
            result.put("awayXGPerGame", analysis.getAwayXGStats().getXGPerGame());
            result.put("homePerformanceStatus", analysis.getHomeXGStats().getPerformanceStatus());
            result.put("awayPerformanceStatus", analysis.getAwayXGStats().getPerformanceStatus());

            analyses.add(result);
        }

        return ResponseEntity.ok(Map.of(
            "description", "xG Analysis Test Results",
            "matchesAnalyzed", analyses.size(),
            "results", analyses
        ));
    }

    /**
     * Get API info and documentation
     * Example: GET /api/xg/info
     */
    @GetMapping("/info")
    public ResponseEntity<?> getInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", "Expected Goals (xG) Analytics");
        info.put("version", "v1.0.0");
        info.put("description", "Statistical measure of shot quality for soccer betting edge");

        info.put("concepts", Map.of(
            "xG", "Expected Goals - probability each shot becomes a goal based on location, angle, etc.",
            "xGA", "Expected Goals Against - defensive quality metric",
            "goalsVsXG", "Actual goals minus xG - positive means overperforming, negative means underperforming",
            "Regression", "Teams significantly over/under-performing xG tend to regress to their xG mean"
        ));

        info.put("bettingEdge", List.of(
            "Team scoring MORE than xG = OVERPERFORMING = due for regression DOWN = FADE",
            "Team scoring LESS than xG = UNDERPERFORMING = due for regression UP = BET ON",
            "xG differential (xG - xGA) indicates true team quality",
            "Recent form xG can identify hot/cold streaks about to end"
        ));

        info.put("performanceThresholds", Map.of(
            "OVERPERFORMING", "Goals scored > xG by 0.3+ per game",
            "UNDERPERFORMING", "Goals scored < xG by 0.3+ per game",
            "NEUTRAL", "Goals within 0.3 of xG per game"
        ));

        info.put("endpoints", Map.of(
            "GET /api/xg/team/{name}", "Get team xG stats",
            "GET /api/xg/match", "Analyze match with xG (params: homeTeam, awayTeam, league)",
            "GET /api/xg/regression-signals", "Find teams due for regression",
            "GET /api/xg/test", "Test with sample European matches",
            "GET /api/xg/info", "This documentation"
        ));

        info.put("supportedLeagues", List.of(
            "EPL - English Premier League",
            "LA_LIGA - Spanish La Liga",
            "BUNDESLIGA - German Bundesliga",
            "SERIE_A - Italian Serie A",
            "LIGUE_1 - French Ligue 1"
        ));

        info.put("cacheStats", xgDataService.getCacheStats());

        return ResponseEntity.ok(info);
    }

    /**
     * Clear the xG cache
     * Example: POST /api/xg/cache/clear
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<?> clearCache() {
        xgDataService.clearCache();
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "xG cache cleared"
        ));
    }
}
