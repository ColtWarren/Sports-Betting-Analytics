package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.model.college.*;
import com.coltwarren.sports_betting_analytics.service.college.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/college")
public class CollegeSportsController {

    private final CFBDataService cfbService;
    private final CBBDataService cbbService;

    public CollegeSportsController(CFBDataService cfbService, CBBDataService cbbService) {
        this.cfbService = cfbService;
        this.cbbService = cbbService;
    }

    // ═══════════════════════════════════════════
    // CFB ENDPOINTS
    // ═══════════════════════════════════════════

    @GetMapping("/cfb/team/{teamName}")
    public ResponseEntity<?> getCFBTeam(@PathVariable String teamName) {
        CFBTeamStats stats = cfbService.getTeamStats(teamName);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/cfb/matchup")
    public ResponseEntity<?> analyzeCFBMatchup(
            @RequestParam String homeTeam,
            @RequestParam String awayTeam,
            @RequestParam(required = false) Double spread,
            @RequestParam(required = false) Double total) {

        CFBMatchupAnalysis analysis = cfbService.analyzeMatchup(homeTeam, awayTeam, spread, total);
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/cfb/edges")
    public ResponseEntity<?> getCFBEdges() {
        List<CFBTeamStats> teams = cfbService.getTeamsWithEdges();
        return ResponseEntity.ok(Map.of(
            "description", "Teams with betting edges (hot ATS or mid-major value)",
            "count", teams.size(),
            "teams", teams
        ));
    }

    @GetMapping("/cfb/test")
    public ResponseEntity<?> testCFB() {
        List<Map<String, Object>> analyses = new ArrayList<>();

        // Test matchups with known edges
        Object[][] matchups = {
            {"Ohio State", "Michigan", -3.5, 45.5},           // Ranked matchup
            {"Army", "Navy", 3.0, 34.5},                       // Service Academy (UNDER!)
            {"Boise State", "San Diego State", -7.0, 52.0},   // Mid-major
            {"Alabama", "Georgia", -2.5, 48.5},               // Top ranked
            {"Appalachian State", "Coastal Carolina", -6.5, 55.0}  // Sun Belt
        };

        for (Object[] m : matchups) {
            CFBMatchupAnalysis analysis = cfbService.analyzeMatchup(
                (String) m[0], (String) m[1], (Double) m[2], (Double) m[3]);

            analyses.add(Map.of(
                "matchup", m[0] + " vs " + m[1],
                "spread", m[2],
                "total", m[3],
                "recommendation", analysis.getRecommendation(),
                "display", analysis.getRecommendationDisplay() != null ?
                    analysis.getRecommendationDisplay() : "N/A",
                "confidence", analysis.getConfidence(),
                "edges", analysis.getEdges(),
                "reason", analysis.getReason() != null ? analysis.getReason() : ""
            ));
        }

        return ResponseEntity.ok(Map.of(
            "description", "CFB Matchup Analysis with Edge Detection",
            "apiConfigured", cfbService.isApiConfigured(),
            "matchupsAnalyzed", analyses.size(),
            "results", analyses,
            "keyEdges", List.of(
                "Ranked matchups with spread <=8.5: Favorites cover 60%+",
                "Service Academy games: UNDER is 46-12-2 (79%!)",
                "Mid-major games: Less efficient market = more value"
            )
        ));
    }

    // ═══════════════════════════════════════════
    // CBB ENDPOINTS
    // ═══════════════════════════════════════════

    @GetMapping("/cbb/team/{teamName}")
    public ResponseEntity<?> getCBBTeam(@PathVariable String teamName) {
        CBBTeamStats stats = cbbService.getTeamStats(teamName);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/cbb/matchup")
    public ResponseEntity<?> analyzeCBBMatchup(
            @RequestParam String homeTeam,
            @RequestParam String awayTeam,
            @RequestParam(required = false) Double spread,
            @RequestParam(required = false) Double total) {

        CBBMatchupAnalysis analysis = cbbService.analyzeMatchup(
            homeTeam, awayTeam, spread, total, LocalDate.now());
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/cbb/edges")
    public ResponseEntity<?> getCBBEdges() {
        List<CBBTeamStats> teams = cbbService.getTeamsWithEdges();
        return ResponseEntity.ok(Map.of(
            "description", "Teams with betting edges (hot ATS or small conference value)",
            "count", teams.size(),
            "teams", teams
        ));
    }

    @GetMapping("/cbb/test")
    public ResponseEntity<?> testCBB() {
        List<Map<String, Object>> analyses = new ArrayList<>();

        // Test matchups
        Object[][] matchups = {
            {"Kentucky", "Duke", -2.5, 145.5},                    // Blue bloods (fade in March!)
            {"Gonzaga", "Saint Mary's", -8.0, 138.0},            // WCC matchup
            {"Purdue", "Michigan State", -5.5, 142.0},           // Big Ten
            {"Drake", "Loyola Chicago", -3.0, 128.0},            // Missouri Valley (small conf!)
            {"San Diego State", "Nevada", -4.5, 135.0}           // Mountain West
        };

        for (Object[] m : matchups) {
            CBBMatchupAnalysis analysis = cbbService.analyzeMatchup(
                (String) m[0], (String) m[1], (Double) m[2], (Double) m[3], LocalDate.now());

            analyses.add(Map.of(
                "matchup", m[0] + " vs " + m[1],
                "spread", m[2],
                "total", m[3],
                "recommendation", analysis.getRecommendation(),
                "display", analysis.getRecommendationDisplay() != null ?
                    analysis.getRecommendationDisplay() : "N/A",
                "confidence", analysis.getConfidence(),
                "edges", analysis.getEdges(),
                "reason", analysis.getReason() != null ? analysis.getReason() : ""
            ));
        }

        return ResponseEntity.ok(Map.of(
            "description", "CBB Matchup Analysis with Edge Detection",
            "apiConfigured", cbbService.isApiConfigured(),
            "currentMonth", LocalDate.now().getMonthValue(),
            "isEarlySeason", LocalDate.now().getMonthValue() == 11 || LocalDate.now().getMonthValue() == 12,
            "matchupsAnalyzed", analyses.size(),
            "results", analyses,
            "keyEdges", List.of(
                "Early season (Nov-Dec): Sportsbooks still learning = max inefficiency!",
                "Small conferences: Limited data = more value",
                "Blue bloods in March: Public overvalues Duke/Kentucky/UNC",
                "Tempo mismatches: Slow team at home controls pace"
            )
        ));
    }

    // ═══════════════════════════════════════════
    // COMBINED INFO
    // ═══════════════════════════════════════════

    @GetMapping("/info")
    public ResponseEntity<?> getInfo() {
        return ResponseEntity.ok(Map.of(
            "name", "College Sports Analytics",
            "description", "CFB and CBB advanced analytics for betting edges",
            "whyCollegeSports", List.of(
                "LESS EFFICIENT markets than NFL/NBA",
                "Sportsbooks can't price 350+ teams accurately",
                "Mid-major and small conference games = max value",
                "Early season (Nov-Dec for CBB) = sportsbooks still learning"
            ),
            "provenEdges", Map.of(
                "CFB_RankedMatchups", "Favorites <=8.5 cover 60%+ of the time",
                "CFB_ServiceAcademy", "Army/Navy/Air Force unders: 46-12-2 (79%!)",
                "CBB_EarlySeason", "November-December most inefficient betting period",
                "CBB_SmallConference", "Horizon, Big Sky, etc. have limited sportsbook data",
                "CBB_FadeBlueBlood", "Duke/Kentucky/UNC overvalued in March Madness"
            ),
            "cfbApiConfigured", cfbService.isApiConfigured(),
            "cbbApiConfigured", cbbService.isApiConfigured(),
            "endpoints", Map.of(
                "GET /api/college/cfb/team/{name}", "Get CFB team stats",
                "GET /api/college/cfb/matchup", "Analyze CFB matchup",
                "GET /api/college/cfb/edges", "Get CFB teams with edges",
                "GET /api/college/cfb/test", "Test CFB analysis",
                "GET /api/college/cbb/team/{name}", "Get CBB team stats",
                "GET /api/college/cbb/matchup", "Analyze CBB matchup",
                "GET /api/college/cbb/edges", "Get CBB teams with edges",
                "GET /api/college/cbb/test", "Test CBB analysis"
            )
        ));
    }
}
