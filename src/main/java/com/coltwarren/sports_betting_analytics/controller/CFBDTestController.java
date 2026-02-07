package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.model.college.*;
import com.coltwarren.sports_betting_analytics.service.college.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Test controller for CollegeFootballData.com and CollegeBasketballData.com APIs
 * Provides endpoints to test API connectivity and data retrieval
 */
@RestController
@RequestMapping("/test/cfbd")
public class CFBDTestController {

    @Autowired
    private CFBDataService cfbService;

    @Autowired
    private CBBDataService cbbService;

    /**
     * Test basketball ratings endpoint
     * GET /test/cfbd/basketball/ratings?season=2024&team=Kentucky
     */
    @GetMapping("/basketball/ratings")
    public ResponseEntity<?> getBasketballRatings(
            @RequestParam(required = false, defaultValue = "2024") Integer season,
            @RequestParam(required = false) String team) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("endpoint", "/test/cfbd/basketball/ratings");
        response.put("season", season);
        response.put("apiConfigured", cbbService.isApiConfigured());
        response.put("baseUrl", "https://api.collegebasketballdata.com");

        if (team != null && !team.isEmpty()) {
            // Get specific team stats
            CBBTeamStats stats = cbbService.getTeamStats(team);
            response.put("team", team);
            response.put("stats", stats);
        } else {
            // Return sample of teams with their ratings
            List<String> sampleTeams = List.of(
                "Kentucky", "Duke", "Kansas", "Gonzaga", "Purdue",
                "UConn", "Houston", "Tennessee", "Arizona", "North Carolina"
            );

            List<Map<String, Object>> teamRatings = new ArrayList<>();
            for (String teamName : sampleTeams) {
                CBBTeamStats stats = cbbService.getTeamStats(teamName);
                teamRatings.add(Map.of(
                    "team", teamName,
                    "conference", stats.getConference(),
                    "adjEfficiency", stats.getAdjEfficiency(),
                    "adjOffense", stats.getAdjOffense(),
                    "adjDefense", stats.getAdjDefense(),
                    "kenPomRank", stats.getKenPomRank() != null ? stats.getKenPomRank() : 0,
                    "netRank", stats.getNetRank() != null ? stats.getNetRank() : 0
                ));
            }

            response.put("description", "Basketball SRS/efficiency ratings from CollegeBasketballData.com");
            response.put("teamCount", teamRatings.size());
            response.put("ratings", teamRatings);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Test football ratings endpoint
     * GET /test/cfbd/football/ratings?year=2024&team=Ohio+State
     */
    @GetMapping("/football/ratings")
    public ResponseEntity<?> getFootballRatings(
            @RequestParam(required = false, defaultValue = "2024") Integer year,
            @RequestParam(required = false) String team) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("endpoint", "/test/cfbd/football/ratings");
        response.put("year", year);
        response.put("apiConfigured", cfbService.isApiConfigured());
        response.put("baseUrl", "https://api.collegefootballdata.com");

        if (team != null && !team.isEmpty()) {
            CFBTeamStats stats = cfbService.getTeamStats(team);
            response.put("team", team);
            response.put("stats", stats);
        } else {
            List<String> sampleTeams = List.of(
                "Ohio State", "Michigan", "Georgia", "Alabama", "Texas",
                "Oregon", "Penn State", "Clemson", "Florida State", "Notre Dame"
            );

            List<Map<String, Object>> teamRatings = new ArrayList<>();
            for (String teamName : sampleTeams) {
                CFBTeamStats stats = cfbService.getTeamStats(teamName);
                teamRatings.add(Map.of(
                    "team", teamName,
                    "conference", stats.getConference(),
                    "overallRating", stats.getOverallRating(),
                    "offenseRating", stats.getOffenseRating(),
                    "defenseRating", stats.getDefenseRating(),
                    "apRank", stats.getApRank() != null ? stats.getApRank() : 0
                ));
            }

            response.put("description", "Football SP+ ratings from CollegeFootballData.com");
            response.put("teamCount", teamRatings.size());
            response.put("ratings", teamRatings);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API info endpoint
     * GET /test/cfbd/info
     */
    @GetMapping("/info")
    public ResponseEntity<?> getInfo() {
        return ResponseEntity.ok(Map.of(
            "name", "CFBD Test Endpoints",
            "description", "Test endpoints for CollegeFootballData.com and CollegeBasketballData.com APIs",
            "footballApi", Map.of(
                "baseUrl", "https://api.collegefootballdata.com",
                "configured", cfbService.isApiConfigured(),
                "keyParameter", "year"
            ),
            "basketballApi", Map.of(
                "baseUrl", "https://api.collegebasketballdata.com",
                "configured", cbbService.isApiConfigured(),
                "keyParameter", "season"
            ),
            "endpoints", List.of(
                "GET /test/cfbd/basketball/ratings?season=2024&team=Kentucky",
                "GET /test/cfbd/football/ratings?year=2024&team=Ohio+State",
                "GET /test/cfbd/info"
            )
        ));
    }
}
