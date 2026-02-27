package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.model.injury.PlayerInjury;
import com.coltwarren.sports_betting_analytics.model.injury.InjuryImpact;
import com.coltwarren.sports_betting_analytics.service.injury.ESPNInjuryService;
import com.coltwarren.sports_betting_analytics.service.injury.InjuryAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Injury Test Controller
 *
 * Test endpoints for injury tracking and analysis.
 * Uses ESPN's free API for real-time injury data.
 */
@RestController
@RequestMapping("/api/test/injuries")
public class InjuryTestController {

    private final ESPNInjuryService espnInjuryService;
    private final InjuryAnalysisService injuryAnalysisService;

    public InjuryTestController(ESPNInjuryService espnInjuryService,
                                InjuryAnalysisService injuryAnalysisService) {
        this.espnInjuryService = espnInjuryService;
        this.injuryAnalysisService = injuryAnalysisService;
    }

    @GetMapping("/{sport}")
    public ResponseEntity<?> getInjuries(@PathVariable String sport) {
        List<PlayerInjury> injuries = espnInjuryService.getAllInjuries(sport);

        // Group by team
        Map<String, List<PlayerInjury>> byTeam = injuries.stream()
            .collect(Collectors.groupingBy(PlayerInjury::getTeam));

        // Count significant injuries
        long outCount = injuries.stream()
            .filter(i -> i.getStatus() != null &&
                        (i.getStatus().equalsIgnoreCase("OUT") ||
                         i.getStatus().toUpperCase().contains("OUT")))
            .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sport", sport.toUpperCase());
        result.put("totalInjuries", injuries.size());
        result.put("playersOut", outCount);
        result.put("teamsAffected", byTeam.size());
        result.put("injuriesByTeam", byTeam);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{sport}/team/{teamName}")
    public ResponseEntity<?> getTeamInjuries(@PathVariable String sport,
                                             @PathVariable String teamName) {
        InjuryImpact impact = injuryAnalysisService.analyzeTeamInjuries(sport, teamName, null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("team", teamName);
        result.put("sport", sport.toUpperCase());
        result.put("severity", impact.getSeverity());
        result.put("spreadImpact", impact.getSpreadImpact());
        result.put("totalImpact", impact.getTotalImpact());
        result.put("hasSignificantInjuries", impact.getHasSignificantInjuries());
        result.put("summary", impact.getSummary());
        result.put("bettingRecommendation", impact.getBettingRecommendation());
        result.put("injuries", impact.getInjuries());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{sport}/matchup")
    public ResponseEntity<?> analyzeMatchup(@PathVariable String sport,
                                           @RequestParam String homeTeam,
                                           @RequestParam String awayTeam) {
        Map<String, InjuryImpact> impacts = injuryAnalysisService.analyzeMatchupInjuries(
            sport, homeTeam, null, awayTeam, null
        );

        Double netAdvantage = injuryAnalysisService.getNetInjuryAdvantage(impacts);
        String worstSeverity = injuryAnalysisService.getWorstSeverity(impacts);

        InjuryImpact homeImpact = impacts.get("home");
        InjuryImpact awayImpact = impacts.get("away");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matchup", awayTeam + " @ " + homeTeam);
        result.put("sport", sport.toUpperCase());

        // Home team injury summary
        Map<String, Object> homeSummary = new LinkedHashMap<>();
        homeSummary.put("team", homeTeam);
        homeSummary.put("severity", homeImpact.getSeverity());
        homeSummary.put("spreadImpact", homeImpact.getSpreadImpact());
        homeSummary.put("hasSignificantInjuries", homeImpact.getHasSignificantInjuries());
        homeSummary.put("summary", homeImpact.getSummary());
        homeSummary.put("outCount", homeImpact.getOutCount());
        result.put("homeInjuryImpact", homeSummary);

        // Away team injury summary
        Map<String, Object> awaySummary = new LinkedHashMap<>();
        awaySummary.put("team", awayTeam);
        awaySummary.put("severity", awayImpact.getSeverity());
        awaySummary.put("spreadImpact", awayImpact.getSpreadImpact());
        awaySummary.put("hasSignificantInjuries", awayImpact.getHasSignificantInjuries());
        awaySummary.put("summary", awayImpact.getSummary());
        awaySummary.put("outCount", awayImpact.getOutCount());
        result.put("awayInjuryImpact", awaySummary);

        // Net advantage analysis
        result.put("netInjuryAdvantage", netAdvantage);
        result.put("worstSeverity", worstSeverity);

        // Betting recommendation
        String recommendation;
        if (netAdvantage > 3.0) {
            recommendation = String.format("🏥 HOME ADVANTAGE: %s is %.1f pts healthier - LEAN %s",
                homeTeam, netAdvantage, homeTeam);
        } else if (netAdvantage < -3.0) {
            recommendation = String.format("🏥 AWAY ADVANTAGE: %s is %.1f pts healthier - LEAN %s",
                awayTeam, Math.abs(netAdvantage), awayTeam);
        } else if (netAdvantage > 2.0) {
            recommendation = String.format("Home team %s slightly healthier (+%.1f pts)", homeTeam, netAdvantage);
        } else if (netAdvantage < -2.0) {
            recommendation = String.format("Away team %s slightly healthier (+%.1f pts)", awayTeam, Math.abs(netAdvantage));
        } else {
            recommendation = "Injury situation roughly even - no significant advantage";
        }
        result.put("recommendation", recommendation);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/info")
    public ResponseEntity<?> getInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        info.put("description", "Enhanced Injury Tracking System");
        info.put("dataSource", "ESPN Hidden API (free, no key required)");
        info.put("sportsSupported", List.of("NFL", "NBA", "MLB", "NHL", "CFB"));

        Map<String, Object> impactCalc = new LinkedHashMap<>();
        impactCalc.put("NFL_QB_OUT", "7.0 points");
        impactCalc.put("NFL_WR_OUT", "2.0 points");
        impactCalc.put("NFL_CB_OUT", "2.0 points");
        impactCalc.put("NFL_LT_OUT", "2.5 points");
        impactCalc.put("NBA_STARTER_OUT", "3.0-4.0 points avg");
        impactCalc.put("MLB_SP_OUT", "3.0 points");
        impactCalc.put("NHL_GOALIE_OUT", "4.0 points");
        info.put("positionImpacts", impactCalc);

        Map<String, String> severityLevels = new LinkedHashMap<>();
        severityLevels.put("CRITICAL", "7+ points of combined injuries");
        severityLevels.put("SEVERE", "4-7 points");
        severityLevels.put("MODERATE", "2-4 points");
        severityLevels.put("MINOR", "1-2 points");
        severityLevels.put("NONE", "< 1 point");
        info.put("severityLevels", severityLevels);

        Map<String, Double> statusMultipliers = new LinkedHashMap<>();
        statusMultipliers.put("OUT", 1.0);
        statusMultipliers.put("DOUBTFUL", 0.75);
        statusMultipliers.put("QUESTIONABLE", 0.25);
        statusMultipliers.put("PROBABLE", 0.1);
        info.put("statusMultipliers", statusMultipliers);

        List<String> endpoints = List.of(
            "GET /api/test/injuries/{sport} - All injuries for a sport",
            "GET /api/test/injuries/{sport}/team/{name} - Team injury analysis",
            "GET /api/test/injuries/{sport}/matchup?homeTeam=X&awayTeam=Y - Matchup comparison",
            "GET /api/test/injuries/info - This info page"
        );
        info.put("endpoints", endpoints);

        Map<String, String> examples = new LinkedHashMap<>();
        examples.put("All NFL injuries", "/api/test/injuries/NFL");
        examples.put("Chiefs injuries", "/api/test/injuries/NFL/team/Chiefs");
        examples.put("Matchup analysis", "/api/test/injuries/NFL/matchup?homeTeam=Chiefs&awayTeam=Bills");
        examples.put("NBA injuries", "/api/test/injuries/NBA");
        info.put("examples", examples);

        return ResponseEntity.ok(info);
    }

    @GetMapping("/{sport}/critical")
    public ResponseEntity<?> getCriticalInjuries(@PathVariable String sport) {
        List<PlayerInjury> injuries = espnInjuryService.getAllInjuries(sport);

        // Filter to only significant injuries (OUT/DOUBTFUL with high rating)
        List<PlayerInjury> critical = injuries.stream()
            .filter(i -> i.getStatus() != null &&
                        (i.getStatus().equalsIgnoreCase("OUT") ||
                         i.getStatus().equalsIgnoreCase("DOUBTFUL")))
            .filter(i -> i.getPlayerRating() != null && i.getPlayerRating() >= 75)
            .sorted((a, b) -> b.getPlayerRating().compareTo(a.getPlayerRating()))
            .collect(Collectors.toList());

        // Format for display
        List<Map<String, Object>> criticalList = critical.stream()
            .map(i -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("player", i.getPlayerName());
                m.put("team", i.getTeam());
                m.put("position", i.getPosition());
                m.put("status", i.getStatus());
                m.put("injury", i.getInjuryType());
                m.put("rating", i.getPlayerRating());
                return m;
            })
            .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sport", sport.toUpperCase());
        result.put("criticalInjuryCount", critical.size());
        result.put("criticalInjuries", criticalList);

        return ResponseEntity.ok(result);
    }
}
