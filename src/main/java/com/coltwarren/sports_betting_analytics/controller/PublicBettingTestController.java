package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.model.betting.PublicBettingData;
import com.coltwarren.sports_betting_analytics.model.betting.ContrarianValue;
import com.coltwarren.sports_betting_analytics.service.betting.PublicBettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Public Betting Test Controller
 *
 * Test endpoint for public betting percentages and contrarian analysis.
 * Usage: GET /api/test/public-betting/{sport}/{homeTeam}/{awayTeam}
 */
@RestController
@RequestMapping("/api/test/public-betting")
public class PublicBettingTestController {

    @Autowired
    private PublicBettingService publicBettingService;

    @GetMapping("/{sport}/{homeTeam}/{awayTeam}")
    public ResponseEntity<?> testPublicBetting(
            @PathVariable String sport,
            @PathVariable String homeTeam,
            @PathVariable String awayTeam,
            @RequestParam(defaultValue = "true") boolean isHomeFavorite) {

        String gameId = sport + "_" + homeTeam + "_" + awayTeam;

        // Get public betting data
        PublicBettingData publicData = publicBettingService.getPublicBettingData(
            gameId, sport.toUpperCase(), homeTeam, awayTeam, isHomeFavorite
        );

        // Analyze contrarian opportunities
        ContrarianValue spreadContrarian = publicBettingService.analyzeContrarianValue(
            publicData, homeTeam, awayTeam
        );

        ContrarianValue totalsContrarian = publicBettingService.analyzeTotalsContrarian(publicData);

        // Build response
        Map<String, Object> result = new HashMap<>();
        result.put("gameId", gameId);
        result.put("sport", sport.toUpperCase());
        result.put("matchup", awayTeam + " @ " + homeTeam);
        result.put("isHomeFavorite", isHomeFavorite);

        // Public betting percentages
        Map<String, Object> publicBetting = new HashMap<>();
        publicBetting.put("spreadHome", publicData.getSpreadHomePercent() + "%");
        publicBetting.put("spreadAway", publicData.getSpreadAwayPercent() + "%");
        publicBetting.put("moneylineHome", publicData.getMoneylineHomePercent() + "%");
        publicBetting.put("moneylineAway", publicData.getMoneylineAwayPercent() + "%");
        publicBetting.put("over", publicData.getOverPercent() + "%");
        publicBetting.put("under", publicData.getUnderPercent() + "%");
        publicBetting.put("ticketPercent", publicData.getTicketPercent() + "%");
        publicBetting.put("moneyPercent", publicData.getMoneyPercent() + "%");
        publicBetting.put("sharpDivergence", publicData.getSharpDivergence() + "%");
        publicBetting.put("hasSignificantLean", publicData.hasSignificantLean());
        result.put("publicBetting", publicBetting);

        // Spread contrarian analysis
        Map<String, Object> spreadAnalysis = new HashMap<>();
        spreadAnalysis.put("hasContrarianValue", spreadContrarian.getHasContrarianValue());
        spreadAnalysis.put("alertLevel", spreadContrarian.getAlertLevel());
        spreadAnalysis.put("publicPercent", spreadContrarian.getPublicPercent());
        spreadAnalysis.put("contrarianSide", spreadContrarian.getContrarianSide());
        spreadAnalysis.put("recommendation", spreadContrarian.getRecommendation());
        spreadAnalysis.put("historicalEdge", spreadContrarian.getHistoricalEdge());
        spreadAnalysis.put("confidence", spreadContrarian.getConfidence());
        spreadAnalysis.put("reason", spreadContrarian.getReason());
        spreadAnalysis.put("displayText", spreadContrarian.getDisplayText());
        result.put("spreadContrarian", spreadAnalysis);

        // Totals contrarian analysis
        Map<String, Object> totalsAnalysis = new HashMap<>();
        totalsAnalysis.put("hasContrarianValue", totalsContrarian.getHasContrarianValue());
        totalsAnalysis.put("alertLevel", totalsContrarian.getAlertLevel());
        totalsAnalysis.put("publicPercent", totalsContrarian.getPublicPercent());
        totalsAnalysis.put("contrarianSide", totalsContrarian.getContrarianSide());
        totalsAnalysis.put("recommendation", totalsContrarian.getRecommendation());
        totalsAnalysis.put("historicalEdge", totalsContrarian.getHistoricalEdge());
        totalsAnalysis.put("confidence", totalsContrarian.getConfidence());
        totalsAnalysis.put("reason", totalsContrarian.getReason());
        totalsAnalysis.put("displayText", totalsContrarian.getDisplayText());
        result.put("totalsContrarian", totalsAnalysis);

        // Summary
        boolean hasAnyContrarian = Boolean.TRUE.equals(spreadContrarian.getHasContrarianValue()) ||
                                   Boolean.TRUE.equals(totalsContrarian.getHasContrarianValue());
        result.put("hasAnyContrarianValue", hasAnyContrarian);

        if (hasAnyContrarian) {
            result.put("summary", "CONTRARIAN OPPORTUNITY FOUND - Fade the public!");
        } else {
            result.put("summary", "No significant contrarian value - Public is not heavily weighted on either side");
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/info")
    public ResponseEntity<?> getPublicBettingInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("description", "Public Betting Percentages and Contrarian Analysis");
        info.put("strategy", "Fade the public - When 65%+ of public is on one side, bet the other side");

        Map<String, Object> thresholds = new HashMap<>();
        thresholds.put("MILD", Map.of("percent", "65%+", "historicalEdge", "52.5%"));
        thresholds.put("STRONG", Map.of("percent", "70%+", "historicalEdge", "54.0%"));
        thresholds.put("EXTREME", Map.of("percent", "75%+", "historicalEdge", "56.5%"));
        info.put("thresholds", thresholds);

        info.put("sharpMoneyNote", "When tickets % diverges from money % by 15%+, sharp bettors may be involved");

        Map<String, String> popularTeams = new HashMap<>();
        popularTeams.put("NFL", "Chiefs, Cowboys, Patriots, Packers, 49ers, Eagles, Bills");
        popularTeams.put("NBA", "Lakers, Warriors, Celtics, Knicks, Heat, Nets");
        popularTeams.put("MLB", "Yankees, Dodgers, Red Sox, Cubs");
        popularTeams.put("College", "Alabama, Ohio State, Georgia, Michigan, Texas");
        popularTeams.put("Soccer", "Real Madrid, Barcelona, Manchester United, Liverpool");
        info.put("publicFavoriteTeams", popularTeams);

        info.put("usage", "GET /api/test/public-betting/{sport}/{homeTeam}/{awayTeam}?isHomeFavorite=true");
        info.put("examples", Map.of(
            "NFL", "/api/test/public-betting/NFL/Chiefs/Raiders",
            "NBA", "/api/test/public-betting/NBA/Lakers/Celtics",
            "MLB", "/api/test/public-betting/MLB/Cardinals/Cubs"
        ));

        return ResponseEntity.ok(info);
    }
}
