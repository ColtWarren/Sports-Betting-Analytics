package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.model.recommendation.*;
import com.coltwarren.sports_betting_analytics.service.recommendation.MultiFactorEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommendation Controller
 *
 * REST endpoints for multi-factor betting recommendations.
 * Combines all edge factors into unified betting advice.
 */
@RestController
@RequestMapping("/api/recommendations")
@Slf4j
public class RecommendationController {

    private final MultiFactorEngine multiFactorEngine;

    public RecommendationController(MultiFactorEngine multiFactorEngine) {
        this.multiFactorEngine = multiFactorEngine;
    }

    /**
     * Analyze a single game
     * Example: POST /api/recommendations/analyze
     */
    @PostMapping("/analyze")
    public ResponseEntity<MultiFactorRecommendation> analyzeGame(
            @RequestBody MultiFactorEngine.GameInput game) {
        MultiFactorRecommendation rec = multiFactorEngine.analyzeGame(game);
        return ResponseEntity.ok(rec);
    }

    /**
     * Analyze multiple games
     * Example: POST /api/recommendations/analyze/batch
     */
    @PostMapping("/analyze/batch")
    public ResponseEntity<DailyRecommendations> analyzeGames(
            @RequestBody List<MultiFactorEngine.GameInput> games) {

        List<MultiFactorRecommendation> recommendations = games.stream()
            .map(multiFactorEngine::analyzeGame)
            .sorted(Comparator.comparing(MultiFactorRecommendation::getConfidence).reversed())
            .collect(Collectors.toList());

        // Create daily summary
        DailyRecommendations daily = new DailyRecommendations();
        daily.setTotalGamesAnalyzed(games.size());

        for (MultiFactorRecommendation rec : recommendations) {
            if (rec.getConfidence() >= 8.0) {
                daily.getHighConfidence().add(rec);
            } else if (rec.getConfidence() >= 6.0) {
                daily.getMediumConfidence().add(rec);
            } else if (!"NO_BET".equals(rec.getPrimaryBet())) {
                daily.getLowConfidence().add(rec);
            } else {
                daily.getNoBets().add(rec);
            }
        }

        daily.setRecommendedBets(daily.getHighConfidence().size() + daily.getMediumConfidence().size());

        // Top picks
        if (!recommendations.isEmpty()) {
            daily.setTopPick(recommendations.get(0));
            daily.setTopFive(recommendations.stream().limit(5).collect(Collectors.toList()));
        }

        // Calculate totals
        double totalEV = recommendations.stream()
            .filter(r -> !"NO_BET".equals(r.getPrimaryBet()))
            .mapToDouble(r -> r.getExpectedValue() != null ? r.getExpectedValue() : 0)
            .sum();
        daily.setTotalExpectedValue(totalEV);

        double totalKelly = recommendations.stream()
            .filter(r -> !"NO_BET".equals(r.getPrimaryBet()))
            .mapToDouble(r -> r.getKellyStake() != null ? r.getKellyStake() : 0)
            .sum();
        daily.setTotalKellyExposure(totalKelly);

        return ResponseEntity.ok(daily);
    }

    /**
     * Test with sample EPL games
     * Example: GET /api/recommendations/test/epl?bankroll=1000
     */
    @GetMapping("/test/epl")
    public ResponseEntity<Map<String, Object>> testEPL(
            @RequestParam(defaultValue = "1000") Double bankroll) {

        List<MultiFactorEngine.GameInput> games = createSampleEPLGames(bankroll);

        List<MultiFactorRecommendation> recommendations = games.stream()
            .map(multiFactorEngine::analyzeGame)
            .sorted(Comparator.comparing(MultiFactorRecommendation::getConfidence).reversed())
            .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("description", "Multi-Factor Analysis of Sample EPL Games");
        result.put("bankroll", bankroll);
        result.put("gamesAnalyzed", games.size());
        result.put("recommendations", recommendations);
        result.put("topPick", recommendations.isEmpty() ? null : recommendations.get(0));

        return ResponseEntity.ok(result);
    }

    /**
     * Test with sample NFL games
     * Example: GET /api/recommendations/test/nfl?bankroll=1000
     */
    @GetMapping("/test/nfl")
    public ResponseEntity<Map<String, Object>> testNFL(
            @RequestParam(defaultValue = "1000") Double bankroll) {

        List<MultiFactorEngine.GameInput> games = createSampleNFLGames(bankroll);

        List<MultiFactorRecommendation> recommendations = games.stream()
            .map(multiFactorEngine::analyzeGame)
            .sorted(Comparator.comparing(MultiFactorRecommendation::getConfidence).reversed())
            .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("description", "Multi-Factor Analysis of Sample NFL Games");
        result.put("bankroll", bankroll);
        result.put("gamesAnalyzed", games.size());
        result.put("recommendations", recommendations);

        return ResponseEntity.ok(result);
    }

    /**
     * Get quick summary for a game
     * Example: GET /api/recommendations/quick-summary
     */
    @GetMapping("/quick-summary")
    public ResponseEntity<Map<String, Object>> getQuickSummary(
            @RequestParam String homeTeam,
            @RequestParam String awayTeam,
            @RequestParam Double homeOdds,
            @RequestParam Double awayOdds,
            @RequestParam(required = false) Double drawOdds,
            @RequestParam(defaultValue = "SOCCER") String sport,
            @RequestParam(defaultValue = "EPL") String league,
            @RequestParam(defaultValue = "1000") Double bankroll) {

        MultiFactorEngine.GameInput game = new MultiFactorEngine.GameInput();
        game.setGameId("quick-" + System.currentTimeMillis());
        game.setSport(sport);
        game.setLeague(league);
        game.setHomeTeam(homeTeam);
        game.setAwayTeam(awayTeam);
        game.setHomeOdds(homeOdds);
        game.setDrawOdds(drawOdds);
        game.setAwayOdds(awayOdds);
        game.setGameTime(LocalDateTime.now().plusHours(2));
        game.setBankroll(bankroll);

        MultiFactorRecommendation rec = multiFactorEngine.analyzeGame(game);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("game", homeTeam + " vs " + awayTeam);
        summary.put("headline", rec.getHeadline());
        summary.put("confidence", rec.getConfidence());
        summary.put("recommendedBet", rec.getPrimaryBetDisplay());
        summary.put("expectedValue", String.format("%.1f%%", rec.getExpectedValue()));
        summary.put("kellyStake", String.format("%.2f%%", rec.getKellyStake()));
        summary.put("suggestedAmount", rec.getSuggestedAmount() != null ?
            String.format("$%.2f", rec.getSuggestedAmount()) : "N/A");
        summary.put("keyInsights", rec.getKeyInsights());
        summary.put("factorsAligned", rec.getFactorsAligned() + "/" + rec.getFactorsTotal());

        return ResponseEntity.ok(summary);
    }

    private List<MultiFactorEngine.GameInput> createSampleEPLGames(Double bankroll) {
        List<MultiFactorEngine.GameInput> games = new ArrayList<>();

        // Game 1: Nottingham Forest vs Liverpool (underdog home - matches pattern!)
        MultiFactorEngine.GameInput game1 = new MultiFactorEngine.GameInput();
        game1.setGameId("epl-1");
        game1.setSport("SOCCER");
        game1.setLeague("EPL");
        game1.setHomeTeam("Nottingham Forest");
        game1.setAwayTeam("Liverpool");
        game1.setGameTime(LocalDateTime.now().plusDays(2));
        game1.setVenue("City Ground Stadium");
        game1.setHomeOdds(3.80);
        game1.setDrawOdds(3.60);
        game1.setAwayOdds(1.95);
        game1.setSportsbook("DraftKings");
        game1.setBankroll(bankroll);
        games.add(game1);

        // Game 2: Arsenal vs Chelsea (rivalry match)
        MultiFactorEngine.GameInput game2 = new MultiFactorEngine.GameInput();
        game2.setGameId("epl-2");
        game2.setSport("SOCCER");
        game2.setLeague("EPL");
        game2.setHomeTeam("Arsenal");
        game2.setAwayTeam("Chelsea");
        game2.setGameTime(LocalDateTime.now().plusDays(3));
        game2.setVenue("Emirates Stadium");
        game2.setHomeOdds(1.75);
        game2.setDrawOdds(3.80);
        game2.setAwayOdds(4.50);
        game2.setSportsbook("FanDuel");
        game2.setBankroll(bankroll);
        games.add(game2);

        // Game 3: Everton vs Man City (big underdog)
        MultiFactorEngine.GameInput game3 = new MultiFactorEngine.GameInput();
        game3.setGameId("epl-3");
        game3.setSport("SOCCER");
        game3.setLeague("EPL");
        game3.setHomeTeam("Everton");
        game3.setAwayTeam("Manchester City");
        game3.setGameTime(LocalDateTime.now().plusDays(4));
        game3.setVenue("Goodison Park Stadium");
        game3.setHomeOdds(6.00);
        game3.setDrawOdds(4.50);
        game3.setAwayOdds(1.50);
        game3.setSportsbook("BetMGM");
        game3.setBankroll(bankroll);
        games.add(game3);

        return games;
    }

    private List<MultiFactorEngine.GameInput> createSampleNFLGames(Double bankroll) {
        List<MultiFactorEngine.GameInput> games = new ArrayList<>();

        // Super Bowl! (example matchup)
        MultiFactorEngine.GameInput superbowl = new MultiFactorEngine.GameInput();
        superbowl.setGameId("superbowl-lx");
        superbowl.setSport("NFL");
        superbowl.setLeague("NFL");
        superbowl.setHomeTeam("49ers");
        superbowl.setAwayTeam("Chiefs");
        superbowl.setGameTime(LocalDateTime.of(2026, 2, 9, 18, 30));
        superbowl.setVenue("Levi's Stadium");
        superbowl.setHomeOdds(2.10);
        superbowl.setAwayOdds(1.80);
        superbowl.setSportsbook("Caesars");
        superbowl.setBankroll(bankroll);
        games.add(superbowl);

        return games;
    }

    /**
     * Get API info
     * Example: GET /api/recommendations/info
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", "Multi-Factor Recommendation Engine");
        info.put("version", "v2.6.0");
        info.put("factors", Map.of(
            "Pattern Recognition", "30% weight - Historical profitable patterns",
            "Odds Value", "25% weight - Pure mathematical edge",
            "Injury Analysis", "20% weight - Player availability impact",
            "Public Betting", "15% weight - Contrarian opportunities",
            "Weather", "10% weight - Outdoor game conditions"
        ));
        info.put("outputs", List.of(
            "Primary bet recommendation",
            "Confidence score (1-10)",
            "Expected Value percentage",
            "Kelly stake recommendation",
            "Risk assessment",
            "Factor-by-factor breakdown"
        ));
        info.put("endpoints", Map.of(
            "POST /api/recommendations/analyze", "Analyze single game",
            "POST /api/recommendations/analyze/batch", "Analyze multiple games",
            "GET /api/recommendations/test/epl", "Test with sample EPL games",
            "GET /api/recommendations/test/nfl", "Test with sample NFL games",
            "GET /api/recommendations/quick-summary", "Quick summary with parameters"
        ));

        return ResponseEntity.ok(info);
    }
}
