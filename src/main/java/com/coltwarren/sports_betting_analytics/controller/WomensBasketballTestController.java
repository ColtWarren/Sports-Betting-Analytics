package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.college.WNCAAWDataService;
import com.coltwarren.sports_betting_analytics.service.wnba.WNBADataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Test controller for Women's Basketball APIs (WNBA + WCBB)
 * Provides endpoints to test API connectivity and data retrieval
 */
@RestController
public class WomensBasketballTestController {

    private final WNBADataService wnbaService;
    private final WNCAAWDataService wcbbService;

    public WomensBasketballTestController(WNBADataService wnbaService, WNCAAWDataService wcbbService) {
        this.wnbaService = wnbaService;
        this.wcbbService = wcbbService;
    }

    // ==================== WNBA Endpoints ====================

    /**
     * Test WNBA teams endpoint
     * GET /test/wnba/teams
     */
    @GetMapping("/test/wnba/teams")
    public ResponseEntity<?> getWnbaTeams() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("endpoint", "/test/wnba/teams");
        response.put("apiConfigured", wnbaService.isApiConfigured());
        response.put("baseUrl", "https://api.balldontlie.io/wnba/v1");

        List<Map<String, Object>> teams = wnbaService.getTeams();
        response.put("teamCount", teams.size());
        response.put("teams", teams);

        return ResponseEntity.ok(response);
    }

    /**
     * Test WNBA standings endpoint
     * GET /test/wnba/standings
     */
    @GetMapping("/test/wnba/standings")
    public ResponseEntity<?> getWnbaStandings() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("endpoint", "/test/wnba/standings");
        response.put("apiConfigured", wnbaService.isApiConfigured());
        response.put("baseUrl", "https://api.balldontlie.io/wnba/v1");

        List<Map<String, Object>> standings = wnbaService.getStandings();
        response.put("teamCount", standings.size());
        response.put("standings", standings);

        return ResponseEntity.ok(response);
    }

    // ==================== WCBB Endpoints ====================

    /**
     * Test WCBB ratings endpoint
     * GET /test/wcbb/ratings?season=2024
     */
    @GetMapping("/test/wcbb/ratings")
    public ResponseEntity<?> getWcbbRatings(
            @RequestParam(required = false, defaultValue = "2024") int season) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("endpoint", "/test/wcbb/ratings");
        response.put("season", season);
        response.put("apiConfigured", wcbbService.isApiConfigured());
        response.put("baseUrl", "https://api.collegebasketballdata.com");

        List<Map<String, Object>> ratings = wcbbService.getTeamRatings(season);
        response.put("description", "Women's college basketball SRS/efficiency ratings");
        response.put("teamCount", ratings.size());
        response.put("ratings", ratings);

        return ResponseEntity.ok(response);
    }

    /**
     * Test WCBB teams endpoint
     * GET /test/wcbb/teams
     */
    @GetMapping("/test/wcbb/teams")
    public ResponseEntity<?> getWcbbTeams() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("endpoint", "/test/wcbb/teams");
        response.put("apiConfigured", wcbbService.isApiConfigured());
        response.put("baseUrl", "https://api.collegebasketballdata.com");

        List<Map<String, Object>> teams = wcbbService.getTeams();
        response.put("teamCount", teams.size());
        response.put("teams", teams);

        return ResponseEntity.ok(response);
    }
}
