package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.model.soccer.SoccerFixture;
import com.coltwarren.sports_betting_analytics.model.soccer.TeamStanding;
import com.coltwarren.sports_betting_analytics.service.soccer.SoccerDataAggregatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * Soccer Page Controller
 *
 * Handles the soccer betting page with:
 * - 3-way odds display (Home/Draw/Away)
 * - Missouri local team highlighting (Sporting KC, St. Louis City SC)
 * - League filtering
 * - Standings display
 *
 * SOCCER 3-WAY BETTING:
 * Unlike US sports (2-way), soccer has 3 outcomes:
 * - Home Win (1)
 * - Draw (X)
 * - Away Win (2)
 */
@Controller
public class SoccerPageController {

    @Autowired
    private SoccerDataAggregatorService soccerDataAggregator;

    /**
     * Main soccer page
     */
    @GetMapping("/soccer")
    public String getSoccerPage(
            @RequestParam(required = false) String league,
            Model model
    ) {
        try {
            // Get fixtures - use odds-only mode to conserve API-Football rate limits
            Map<String, List<SoccerFixture>> allFixtures = soccerDataAggregator.getAllFixturesFromOddsOnly();

            // Get Missouri local team games (prioritized)
            List<SoccerFixture> localGames = new ArrayList<>();
            if (allFixtures.containsKey("MLS")) {
                for (SoccerFixture fixture : allFixtures.get("MLS")) {
                    if (fixture.isLocalTeam()) {
                        localGames.add(fixture);
                    }
                }
            }

            // Get standings if a specific league is selected
            List<TeamStanding> standings = new ArrayList<>();
            if (league != null && !league.isEmpty()) {
                standings = soccerDataAggregator.getStandings(league);
            }

            // Get API usage stats
            Map<String, Object> apiStats = soccerDataAggregator.getApiStats();

            // Add to model
            model.addAttribute("allFixtures", allFixtures);
            model.addAttribute("localGames", localGames);
            model.addAttribute("standings", standings);
            model.addAttribute("selectedLeague", league);
            model.addAttribute("apiStats", apiStats);

            // Available leagues
            model.addAttribute("leagues", Arrays.asList(
                "MLS", "PREMIER_LEAGUE", "CHAMPIONS_LEAGUE",
                "LA_LIGA", "BUNDESLIGA", "SERIE_A", "LIGUE_1"
            ));

            return "soccer";

        } catch (Exception e) {
            System.err.println("Error loading soccer page: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Unable to load soccer data. Please try again later.");
            model.addAttribute("allFixtures", Collections.emptyMap());
            model.addAttribute("localGames", Collections.emptyList());
            return "soccer";
        }
    }

    /**
     * API endpoint to get fixtures for a specific league
     */
    @GetMapping("/api/soccer/fixtures")
    @ResponseBody
    public Map<String, Object> getFixtures(@RequestParam(required = false) String league) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<SoccerFixture> fixtures;
            if (league != null && !league.isEmpty()) {
                fixtures = soccerDataAggregator.getFixturesFromOddsOnly(league);
            } else {
                fixtures = new ArrayList<>();
                soccerDataAggregator.getAllFixturesFromOddsOnly().values()
                    .forEach(fixtures::addAll);
            }

            response.put("success", true);
            response.put("fixtures", fixtures);
            response.put("count", fixtures.size());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("fixtures", Collections.emptyList());
        }

        return response;
    }

    /**
     * API endpoint to get Missouri local team games
     */
    @GetMapping("/api/soccer/local-games")
    @ResponseBody
    public Map<String, Object> getLocalGames() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<SoccerFixture> localGames = soccerDataAggregator.getLocalTeamGamesWithOdds();

            response.put("success", true);
            response.put("localGames", localGames);
            response.put("count", localGames.size());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("localGames", Collections.emptyList());
        }

        return response;
    }

    /**
     * API endpoint to get league standings
     */
    @GetMapping("/api/soccer/standings")
    @ResponseBody
    public Map<String, Object> getStandings(@RequestParam String league) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<TeamStanding> standings = soccerDataAggregator.getStandings(league);

            response.put("success", true);
            response.put("standings", standings);
            response.put("league", league);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("standings", Collections.emptyList());
        }

        return response;
    }

    /**
     * API endpoint to get API usage stats
     */
    @GetMapping("/api/soccer/stats")
    @ResponseBody
    public Map<String, Object> getApiStats() {
        return soccerDataAggregator.getApiStats();
    }
}
