package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.model.pattern.BettingPattern;
import com.coltwarren.sports_betting_analytics.model.pattern.PatternMatch;
import com.coltwarren.sports_betting_analytics.service.pattern.PatternDiscoveryService;
import com.coltwarren.sports_betting_analytics.service.pattern.PatternMatchingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pattern Controller
 *
 * REST endpoints for pattern discovery and matching.
 * Discovers profitable betting patterns from historical data
 * and flags today's games that match those patterns.
 */
@RestController
@RequestMapping("/api/patterns")
@Slf4j
public class PatternController {

    @Autowired
    private PatternDiscoveryService patternDiscoveryService;

    @Autowired
    private PatternMatchingService patternMatchingService;

    /**
     * Discover all profitable patterns for a league
     * Example: GET /api/patterns/discover/EPL
     */
    @GetMapping("/discover/{league}")
    public ResponseEntity<Map<String, Object>> discoverPatterns(@PathVariable String league) {
        log.info("Discovering patterns for {}", league.toUpperCase());

        List<BettingPattern> patterns = patternDiscoveryService.discoverPatterns(league.toUpperCase());

        // Summary
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("league", league.toUpperCase());
        result.put("patternsFound", patterns.size());
        result.put("topPatterns", patterns.stream()
            .limit(10)
            .map(p -> {
                Map<String, Object> pMap = new LinkedHashMap<>();
                pMap.put("name", p.getName());
                pMap.put("roi", String.format("%.1f%%", p.getRoi()));
                pMap.put("winRate", String.format("%.1f%%", p.getWinRate()));
                pMap.put("sampleSize", p.getSampleSize());
                pMap.put("confidence", p.getConfidence());
                pMap.put("betType", p.getBetType());
                return pMap;
            })
            .collect(Collectors.toList()));
        result.put("allPatterns", patterns);

        return ResponseEntity.ok(result);
    }

    /**
     * Find pattern matches for today's games
     * Example: POST /api/patterns/match/EPL
     */
    @PostMapping("/match/{league}")
    public ResponseEntity<Map<String, Object>> findMatches(
            @PathVariable String league,
            @RequestBody List<PatternMatchingService.GameOdds> games) {

        List<PatternMatch> matches = patternMatchingService.findPatternMatches(
            league.toUpperCase(), games);

        return ResponseEntity.ok(Map.of(
            "league", league.toUpperCase(),
            "gamesChecked", games.size(),
            "patternsMatched", matches.size(),
            "matches", matches
        ));
    }

    /**
     * Test with sample game
     * Example: GET /api/patterns/test/EPL
     */
    @GetMapping("/test/{league}")
    public ResponseEntity<Map<String, Object>> testPatterns(@PathVariable String league) {
        // Create sample games
        List<PatternMatchingService.GameOdds> sampleGames = new ArrayList<>();

        PatternMatchingService.GameOdds game1 = new PatternMatchingService.GameOdds();
        game1.setGameId("test1");
        game1.setHomeTeam("Arsenal");
        game1.setAwayTeam("Chelsea");
        game1.setGameTime(LocalDateTime.now().plusDays(1));
        game1.setHomeOdds(2.10);
        game1.setDrawOdds(3.40);
        game1.setAwayOdds(3.50);
        sampleGames.add(game1);

        PatternMatchingService.GameOdds game2 = new PatternMatchingService.GameOdds();
        game2.setGameId("test2");
        game2.setHomeTeam("Everton");
        game2.setAwayTeam("Man City");
        game2.setGameTime(LocalDateTime.now().plusDays(1));
        game2.setHomeOdds(6.50);
        game2.setDrawOdds(4.20);
        game2.setAwayOdds(1.45);
        sampleGames.add(game2);

        PatternMatchingService.GameOdds game3 = new PatternMatchingService.GameOdds();
        game3.setGameId("test3");
        game3.setHomeTeam("Brighton");
        game3.setAwayTeam("Fulham");
        game3.setGameTime(LocalDateTime.now().plusDays(1));
        game3.setHomeOdds(2.30);
        game3.setDrawOdds(3.30);
        game3.setAwayOdds(3.10);
        sampleGames.add(game3);

        List<PatternMatch> matches = patternMatchingService.findPatternMatches(
            league.toUpperCase(), sampleGames);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("description", "Testing pattern matching with sample EPL games");
        result.put("sampleGames", sampleGames.stream().map(g -> Map.of(
            "gameId", g.getGameId(),
            "match", g.getHomeTeam() + " vs " + g.getAwayTeam(),
            "homeOdds", g.getHomeOdds(),
            "drawOdds", g.getDrawOdds(),
            "awayOdds", g.getAwayOdds()
        )).collect(Collectors.toList()));
        result.put("patternMatches", matches.stream().map(m -> Map.of(
            "game", m.getHomeTeam() + " vs " + m.getAwayTeam(),
            "recommendedBet", m.getRecommendedBet(),
            "odds", m.getCurrentOdds(),
            "expectedValue", String.format("%.1f%%", m.getExpectedValue()),
            "suggestedKelly", String.format("%.2f%%", m.getSuggestedKelly() * 100),
            "pattern", m.getPattern().getName(),
            "patternROI", String.format("%.1f%%", m.getPattern().getRoi()),
            "confidence", m.getCombinedConfidence()
        )).collect(Collectors.toList()));
        result.put("summary", matches.isEmpty() ?
            "No patterns matched - try importing historical data first with POST /api/backtest/import/EPL" :
            String.format("%d pattern matches found!", matches.size()));

        return ResponseEntity.ok(result);
    }

    /**
     * Refresh pattern cache
     * Example: POST /api/patterns/refresh/EPL
     */
    @PostMapping("/refresh/{league}")
    public ResponseEntity<Map<String, Object>> refreshPatterns(@PathVariable String league) {
        patternMatchingService.refreshPatterns(league.toUpperCase());
        List<BettingPattern> patterns = patternDiscoveryService.discoverPatterns(league.toUpperCase());

        return ResponseEntity.ok(Map.of(
            "message", "Pattern cache refreshed",
            "league", league.toUpperCase(),
            "patternsFound", patterns.size()
        ));
    }

    /**
     * Get API info
     * Example: GET /api/patterns/info
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("description", "Pattern Recognition Engine");
        info.put("features", List.of(
            "Automatic pattern discovery from historical data",
            "Odds range analysis (find profitable odds brackets)",
            "Draw pattern detection",
            "Home underdog value finding",
            "Away favorite fade detection",
            "Seasonal/monthly trend analysis",
            "Over/Under goal patterns"
        ));
        info.put("requirements", "Historical data must be imported first (POST /api/backtest/import/{league})");
        info.put("endpoints", Map.of(
            "GET /api/patterns/discover/{league}", "Discover all profitable patterns",
            "POST /api/patterns/match/{league}", "Find pattern matches for games",
            "GET /api/patterns/test/{league}", "Test with sample games",
            "POST /api/patterns/refresh/{league}", "Refresh pattern cache"
        ));

        return ResponseEntity.ok(info);
    }

    /**
     * Get summary of discovered patterns across all leagues
     * Example: GET /api/patterns/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        String[] leagues = {"EPL", "LA_LIGA", "BUNDESLIGA", "SERIE_A", "LIGUE_1"};
        List<Map<String, Object>> leagueSummaries = new ArrayList<>();

        for (String league : leagues) {
            try {
                List<BettingPattern> patterns = patternDiscoveryService.discoverPatterns(league);
                if (!patterns.isEmpty()) {
                    Map<String, Object> leagueSummary = new LinkedHashMap<>();
                    leagueSummary.put("league", league);
                    leagueSummary.put("patternsFound", patterns.size());
                    leagueSummary.put("bestPattern", patterns.get(0).getName());
                    leagueSummary.put("bestROI", String.format("%.1f%%", patterns.get(0).getRoi()));
                    leagueSummaries.add(leagueSummary);
                }
            } catch (Exception e) {
                log.debug("No data for league {}", league);
            }
        }

        summary.put("leagueSummaries", leagueSummaries);
        summary.put("totalLeagues", leagueSummaries.size());

        return ResponseEntity.ok(summary);
    }
}
