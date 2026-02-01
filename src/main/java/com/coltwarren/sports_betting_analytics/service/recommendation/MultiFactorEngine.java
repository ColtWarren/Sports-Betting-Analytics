package com.coltwarren.sports_betting_analytics.service.recommendation;

import com.coltwarren.sports_betting_analytics.model.recommendation.*;
import com.coltwarren.sports_betting_analytics.model.pattern.BettingPattern;
import com.coltwarren.sports_betting_analytics.model.pattern.PatternMatch;
import com.coltwarren.sports_betting_analytics.model.xg.MatchXGAnalysis;
import com.coltwarren.sports_betting_analytics.service.pattern.PatternMatchingService;
import com.coltwarren.sports_betting_analytics.service.xg.XGDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Multi-Factor Engine
 *
 * The brain of the betting platform. Combines all edge factors:
 * - Pattern Recognition (historical profitable situations)
 * - Odds Value (pure mathematical edge)
 * - Injury Analysis (player availability impact)
 * - Public Betting (contrarian opportunities)
 * - Weather (outdoor game conditions)
 *
 * Produces unified recommendations with confidence scoring and Kelly stakes.
 */
@Service
@Slf4j
public class MultiFactorEngine {

    @Autowired
    private PatternMatchingService patternMatchingService;

    @Autowired
    private XGDataService xgDataService;

    // Factor weights (sum to 1.0)
    private static final double WEIGHT_PATTERN = 0.25;      // Historical patterns
    private static final double WEIGHT_VALUE = 0.25;        // Pure odds value
    private static final double WEIGHT_XG = 0.15;           // Expected Goals (soccer)
    private static final double WEIGHT_INJURY = 0.15;       // Injury impact
    private static final double WEIGHT_PUBLIC = 0.10;       // Contrarian value
    private static final double WEIGHT_WEATHER = 0.10;      // Weather (when applicable)

    // Thresholds
    private static final double MIN_CONFIDENCE_TO_BET = 6.0;
    private static final double MIN_EV_TO_BET = 2.0;        // 2% minimum expected value
    private static final double MAX_KELLY = 0.05;           // 5% max stake

    /**
     * Generate multi-factor recommendation for a single game
     */
    public MultiFactorRecommendation analyzeGame(GameInput game) {
        MultiFactorRecommendation rec = new MultiFactorRecommendation();

        // Set game info
        rec.setGameId(game.getGameId());
        rec.setSport(game.getSport());
        rec.setLeague(game.getLeague());
        rec.setHomeTeam(game.getHomeTeam());
        rec.setAwayTeam(game.getAwayTeam());
        rec.setGameTime(game.getGameTime());
        rec.setVenue(game.getVenue());
        rec.setHomeOdds(game.getHomeOdds());
        rec.setDrawOdds(game.getDrawOdds());
        rec.setAwayOdds(game.getAwayOdds());
        rec.setBestSportsbook(game.getSportsbook());

        // Analyze each factor
        rec.setWeatherFactor(analyzeWeatherFactor(game));
        rec.setPublicBettingFactor(analyzePublicBettingFactor(game));
        rec.setInjuryFactor(analyzeInjuryFactor(game));
        rec.setPatternFactor(analyzePatternFactor(game));
        rec.setValueFactor(analyzeValueFactor(game));
        rec.setXgFactor(analyzeXGFactor(game));

        // Combine factors
        combineFactors(rec);

        // Generate final recommendation
        generateRecommendation(rec, game.getBankroll());

        // Create summary
        generateSummary(rec);

        log.info("Multi-factor analysis for {} vs {}: {:.1f} confidence, {} recommendation",
                game.getHomeTeam(), game.getAwayTeam(),
                rec.getConfidence(),
                rec.getPrimaryBet());

        return rec;
    }

    /**
     * Analyze weather factor
     */
    private FactorAnalysis analyzeWeatherFactor(GameInput game) {
        FactorAnalysis factor = new FactorAnalysis("Weather");
        factor.setWeight(WEIGHT_WEATHER);

        // Only applicable for outdoor sports
        if (!isOutdoorSport(game.getSport()) || game.getVenue() == null) {
            factor.setIsApplicable(false);
            factor.setReason("Indoor venue or sport - weather not applicable");
            return factor;
        }

        // Simulate weather analysis (in production, would call WeatherAnalysisService)
        // For now, use a simplified model based on venue
        try {
            // Random weather factors for demonstration
            boolean hasWeatherImpact = game.getVenue().toLowerCase().contains("stadium");

            if (!hasWeatherImpact) {
                factor.setIsApplicable(false);
                factor.setReason("No significant weather impact expected");
                return factor;
            }

            factor.setIsApplicable(true);
            factor.setRawScore(6.5);
            factor.setWeightedScore(6.5 * WEIGHT_WEATHER);
            factor.setRecommendation("LEAN_UNDER");
            factor.setReason("Cold weather conditions may reduce scoring");
            factor.setAlertLevel("LOW");

        } catch (Exception e) {
            log.warn("Error analyzing weather: {}", e.getMessage());
            factor.setIsApplicable(false);
        }

        return factor;
    }

    /**
     * Analyze public betting / contrarian factor
     */
    private FactorAnalysis analyzePublicBettingFactor(GameInput game) {
        FactorAnalysis factor = new FactorAnalysis("Public Betting");
        factor.setWeight(WEIGHT_PUBLIC);

        try {
            // Simulate public betting data based on odds
            // Favorites typically get more public money
            boolean isHomeFavorite = game.getHomeOdds() < game.getAwayOdds();
            double favoriteOdds = isHomeFavorite ? game.getHomeOdds() : game.getAwayOdds();

            // Estimate public percentage (heavy favorites get more public action)
            double publicOnFavorite = 50 + (30 * (1 / favoriteOdds));
            publicOnFavorite = Math.min(85, publicOnFavorite);

            // Check for contrarian opportunity (fade the public)
            if (publicOnFavorite < 65) {
                factor.setIsApplicable(false);
                factor.setReason("No significant contrarian opportunity (public split < 65%)");
                return factor;
            }

            factor.setIsApplicable(true);

            // Score based on how lopsided the public is
            double score = 5 + ((publicOnFavorite - 65) / 5);
            factor.setRawScore(Math.min(10, score));
            factor.setWeightedScore(factor.getRawScore() * WEIGHT_PUBLIC);

            // Recommend fading the public
            String fadeTeam = isHomeFavorite ? game.getAwayTeam() : game.getHomeTeam();
            factor.setRecommendation(isHomeFavorite ? "BET_AWAY" : "BET_HOME");
            factor.setReason(String.format("%.0f%% public on %s - Contrarian value on %s",
                publicOnFavorite,
                isHomeFavorite ? game.getHomeTeam() : game.getAwayTeam(),
                fadeTeam));

            if (publicOnFavorite >= 75) {
                factor.setAlertLevel("HIGH");
            } else if (publicOnFavorite >= 70) {
                factor.setAlertLevel("MEDIUM");
            } else {
                factor.setAlertLevel("LOW");
            }

        } catch (Exception e) {
            log.warn("Error analyzing public betting: {}", e.getMessage());
            factor.setIsApplicable(false);
        }

        return factor;
    }

    /**
     * Analyze injury factor
     */
    private FactorAnalysis analyzeInjuryFactor(GameInput game) {
        FactorAnalysis factor = new FactorAnalysis("Injuries");
        factor.setWeight(WEIGHT_INJURY);

        try {
            // Simulate injury analysis
            // In production, would call InjuryAnalysisService

            // For demonstration, assume underdogs have slight injury advantage
            // (favorites often have more scrutiny on injuries)
            boolean isHomeFavorite = game.getHomeOdds() < game.getAwayOdds();
            double oddsGap = Math.abs(game.getHomeOdds() - game.getAwayOdds());

            // Only applicable if significant favorites exist
            if (oddsGap < 0.5) {
                factor.setIsApplicable(false);
                factor.setReason("No significant injury advantage detected");
                return factor;
            }

            factor.setIsApplicable(true);

            // Score based on odds gap (bigger favorites may have hidden injury concerns)
            double score = 5 + oddsGap;
            factor.setRawScore(Math.min(8, score));
            factor.setWeightedScore(factor.getRawScore() * WEIGHT_INJURY);

            factor.setRecommendation("NEUTRAL");
            factor.setReason("Minor injury situations on both sides");
            factor.setAlertLevel("LOW");

        } catch (Exception e) {
            log.warn("Error analyzing injuries: {}", e.getMessage());
            factor.setIsApplicable(false);
        }

        return factor;
    }

    /**
     * Analyze pattern factor - THE KEY FACTOR
     */
    private FactorAnalysis analyzePatternFactor(GameInput game) {
        FactorAnalysis factor = new FactorAnalysis("Historical Patterns");
        factor.setWeight(WEIGHT_PATTERN);

        try {
            // Create GameOdds object for pattern matching
            PatternMatchingService.GameOdds gameOdds = new PatternMatchingService.GameOdds();
            gameOdds.setGameId(game.getGameId());
            gameOdds.setHomeTeam(game.getHomeTeam());
            gameOdds.setAwayTeam(game.getAwayTeam());
            gameOdds.setGameTime(game.getGameTime());
            gameOdds.setHomeOdds(game.getHomeOdds());
            gameOdds.setDrawOdds(game.getDrawOdds());
            gameOdds.setAwayOdds(game.getAwayOdds());

            List<PatternMatch> matches = patternMatchingService.findPatternMatches(
                game.getLeague(), List.of(gameOdds));

            if (matches.isEmpty()) {
                factor.setIsApplicable(false);
                factor.setReason("No profitable historical patterns match this game");
                return factor;
            }

            // Use the best matching pattern
            PatternMatch bestMatch = matches.get(0);
            BettingPattern pattern = bestMatch.getPattern();

            factor.setIsApplicable(true);
            factor.setRawScore(pattern.getConfidence());
            factor.setWeightedScore(pattern.getConfidence() * WEIGHT_PATTERN);
            factor.setRecommendation(bestMatch.getRecommendedBet());
            factor.setReason(String.format("%s - %.1f%% ROI over %d games (%.1f%% win rate)",
                pattern.getName(), pattern.getRoi(), pattern.getSampleSize(), pattern.getWinRate()));
            factor.setAlertLevel(mapConfidenceToAlert(pattern.getConfidence()));

        } catch (Exception e) {
            log.warn("Error analyzing patterns: {}", e.getMessage());
            factor.setIsApplicable(false);
            factor.setReason("Pattern analysis unavailable");
        }

        return factor;
    }

    /**
     * Analyze pure value factor (odds vs implied probability)
     */
    private FactorAnalysis analyzeValueFactor(GameInput game) {
        FactorAnalysis factor = new FactorAnalysis("Odds Value");
        factor.setWeight(WEIGHT_VALUE);
        factor.setIsApplicable(true);

        // Calculate implied probabilities and overround
        double homeImplied = 1.0 / game.getHomeOdds();
        double drawImplied = game.getDrawOdds() != null ? 1.0 / game.getDrawOdds() : 0;
        double awayImplied = 1.0 / game.getAwayOdds();

        double overround = homeImplied + drawImplied + awayImplied;
        double margin = (overround - 1.0) * 100; // Bookmaker margin %

        // Fair probabilities (removing margin)
        double fairHome = homeImplied / overround;
        double fairDraw = drawImplied / overround;
        double fairAway = awayImplied / overround;

        // Find best value
        double homeValue = fairHome - homeImplied;
        double drawValue = fairDraw - drawImplied;
        double awayValue = fairAway - awayImplied;

        String bestBet = "HOME";
        double bestValue = homeValue;

        if (drawValue > bestValue && game.getDrawOdds() != null) {
            bestBet = "DRAW";
            bestValue = drawValue;
        }
        if (awayValue > bestValue) {
            bestBet = "AWAY";
            bestValue = awayValue;
        }

        // Score based on value found (scale 0-10)
        double score = Math.min(10, Math.max(0, 5 + bestValue * 100));
        factor.setRawScore(score);
        factor.setWeightedScore(score * WEIGHT_VALUE);
        factor.setRecommendation("BET_" + bestBet);
        factor.setReason(String.format("Best value on %s (%.1f%% edge vs fair odds, %.1f%% margin)",
            bestBet, bestValue * 100, margin));
        factor.setAlertLevel(bestValue > 0.05 ? "MEDIUM" : "LOW");

        return factor;
    }

    /**
     * Analyze Expected Goals (xG) factor - soccer only
     */
    private FactorAnalysis analyzeXGFactor(GameInput game) {
        FactorAnalysis factor = new FactorAnalysis("Expected Goals (xG)");
        factor.setWeight(WEIGHT_XG);

        // Only applicable for soccer
        String sport = game.getSport() != null ? game.getSport().toUpperCase() : "";
        String league = game.getLeague() != null ? game.getLeague().toUpperCase() : "";

        boolean isSoccer = "SOCCER".equals(sport) ||
                          "EPL".equals(league) ||
                          league.contains("LIGA") ||
                          league.contains("SERIE") ||
                          league.contains("BUNDESLIGA") ||
                          league.contains("LIGUE");

        if (!isSoccer) {
            factor.setIsApplicable(false);
            factor.setReason("xG analysis only available for soccer");
            return factor;
        }

        try {
            MatchXGAnalysis xgAnalysis = xgDataService.analyzeMatch(
                game.getHomeTeam(), game.getAwayTeam(), game.getLeague());

            if (xgAnalysis == null || "INSUFFICIENT_DATA".equals(xgAnalysis.getXgRecommendation())) {
                factor.setIsApplicable(false);
                factor.setReason("Insufficient xG data for these teams");
                return factor;
            }

            factor.setIsApplicable(true);
            factor.setRawScore(xgAnalysis.getXgConfidence());
            factor.setWeightedScore(xgAnalysis.getXgConfidence() * WEIGHT_XG);
            factor.setRecommendation("BET_" + xgAnalysis.getXgRecommendation());
            factor.setReason(xgAnalysis.getXgReason());
            factor.setAlertLevel(xgAnalysis.getAlertLevel());

        } catch (Exception e) {
            log.warn("Error analyzing xG: {}", e.getMessage());
            factor.setIsApplicable(false);
            factor.setReason("xG analysis unavailable");
        }

        return factor;
    }

    /**
     * Combine all factors into final scores
     */
    private void combineFactors(MultiFactorRecommendation rec) {
        List<FactorAnalysis> factors = List.of(
            rec.getWeatherFactor(),
            rec.getPublicBettingFactor(),
            rec.getInjuryFactor(),
            rec.getPatternFactor(),
            rec.getValueFactor(),
            rec.getXgFactor()
        );

        // Count applicable factors
        int applicable = (int) factors.stream().filter(FactorAnalysis::getIsApplicable).count();
        rec.setFactorsTotal(applicable);

        // Calculate combined score
        double totalWeight = 0;
        double weightedSum = 0;

        for (FactorAnalysis f : factors) {
            if (f.getIsApplicable()) {
                weightedSum += f.getWeightedScore();
                totalWeight += f.getWeight();
            }
        }

        double combinedScore = totalWeight > 0 ? (weightedSum / totalWeight) * 10 : 5.0;
        rec.setCombinedScore(combinedScore);

        // Count aligned factors (how many suggest same direction)
        Map<String, Integer> directionCounts = new HashMap<>();
        for (FactorAnalysis f : factors) {
            if (f.getIsApplicable() && f.getRecommendation() != null &&
                !f.getRecommendation().equals("NEUTRAL")) {
                String dir = f.getRecommendation()
                    .replace("BET_", "")
                    .replace("LEAN_", "");
                directionCounts.merge(dir, 1, Integer::sum);
            }
        }

        int maxAligned = directionCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        rec.setFactorsAligned(maxAligned);

        // Calculate confidence (1-10 scale)
        double confidence = combinedScore;

        // Boost for factor alignment
        if (maxAligned >= 4) confidence += 1.5;
        else if (maxAligned >= 3) confidence += 1.0;
        else if (maxAligned >= 2) confidence += 0.5;

        // Boost for high-confidence pattern factor
        if (rec.getPatternFactor().getIsApplicable() && rec.getPatternFactor().getRawScore() > 8) {
            confidence += 0.5;
        }

        rec.setConfidence(Math.min(10.0, Math.max(1.0, confidence)));
    }

    /**
     * Generate final recommendation
     */
    private void generateRecommendation(MultiFactorRecommendation rec, Double bankroll) {
        // Determine primary bet direction
        Map<String, Double> directionScores = new HashMap<>();

        List<FactorAnalysis> factors = List.of(
            rec.getPatternFactor(),
            rec.getValueFactor(),
            rec.getXgFactor(),
            rec.getInjuryFactor(),
            rec.getPublicBettingFactor(),
            rec.getWeatherFactor()
        );

        for (FactorAnalysis f : factors) {
            if (f.getIsApplicable() && f.getRecommendation() != null) {
                String dir = f.getRecommendation()
                    .replace("BET_", "")
                    .replace("LEAN_", "");

                if (!dir.equals("NEUTRAL") && !dir.equals("NO_BET")) {
                    directionScores.merge(dir, f.getWeightedScore(), Double::sum);
                }
            }
        }

        // Find best direction
        String bestDirection = directionScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("NO_BET");

        // Check if we meet minimum thresholds
        if (rec.getConfidence() < MIN_CONFIDENCE_TO_BET) {
            rec.setPrimaryBet("NO_BET");
            rec.setPrimaryBetDisplay("No bet recommended - confidence too low");
            rec.setKellyStake(0.0);
            rec.setExpectedValue(0.0);
            rec.setRiskLevel("N/A");
            return;
        }

        rec.setPrimaryBet(bestDirection);

        // Get the odds for the recommended bet
        Double odds = switch (bestDirection) {
            case "HOME" -> rec.getHomeOdds();
            case "AWAY" -> rec.getAwayOdds();
            case "DRAW" -> rec.getDrawOdds();
            case "OVER" -> rec.getOverOdds();
            case "UNDER" -> rec.getUnderOdds();
            default -> null;
        };

        rec.setRecommendedOdds(odds);

        // Format display
        rec.setPrimaryBetDisplay(formatBetDisplay(bestDirection, rec));

        // Calculate EV
        if (odds != null) {
            double winProb = rec.getCombinedScore() / 10.0 * 0.6 + 0.2; // Scale to realistic range
            double ev = (winProb * (odds - 1)) - ((1 - winProb) * 1);
            rec.setExpectedValue(ev * 100);

            // Calculate Kelly stake
            double kelly = (winProb * odds - 1) / (odds - 1);
            kelly = Math.max(0, Math.min(MAX_KELLY, kelly * 0.5)); // Half-Kelly, capped
            rec.setKellyStake(kelly * 100);

            // Suggested amount
            if (bankroll != null && bankroll > 0) {
                rec.setSuggestedAmount(bankroll * kelly);
            }

            // Risk level
            if (kelly > 0.03) rec.setRiskLevel("HIGH");
            else if (kelly > 0.015) rec.setRiskLevel("MEDIUM");
            else rec.setRiskLevel("LOW");
        }

        // Risk factors
        if (rec.getFactorsAligned() < 3) {
            rec.getRiskFactors().add("Factors not strongly aligned");
        }
        if (odds != null && odds > 4.0) {
            rec.getRiskFactors().add("High odds (underdog) - higher variance");
        }
    }

    /**
     * Generate human-readable summary
     */
    private void generateSummary(MultiFactorRecommendation rec) {
        // Headline
        if (rec.getConfidence() >= 8.0) {
            rec.setHeadline("🔥 STRONG BET: " + rec.getPrimaryBetDisplay());
        } else if (rec.getConfidence() >= 6.5) {
            rec.setHeadline("✅ GOOD VALUE: " + rec.getPrimaryBetDisplay());
        } else if (rec.getConfidence() >= MIN_CONFIDENCE_TO_BET) {
            rec.setHeadline("📊 LEAN: " + rec.getPrimaryBetDisplay());
        } else {
            rec.setHeadline("⏸️ PASS: Low confidence on this game");
        }

        // Key insights
        List<String> insights = new ArrayList<>();

        if (rec.getPatternFactor().getIsApplicable()) {
            insights.add("📈 " + rec.getPatternFactor().getReason());
        }
        if (rec.getPublicBettingFactor().getIsApplicable()) {
            insights.add("👥 " + rec.getPublicBettingFactor().getReason());
        }
        if (rec.getInjuryFactor().getIsApplicable()) {
            insights.add("🏥 " + rec.getInjuryFactor().getReason());
        }
        if (rec.getWeatherFactor().getIsApplicable()) {
            insights.add("☁️ " + rec.getWeatherFactor().getReason());
        }
        if (rec.getValueFactor().getIsApplicable()) {
            insights.add("💰 " + rec.getValueFactor().getReason());
        }
        if (rec.getXgFactor().getIsApplicable()) {
            insights.add("⚽ " + rec.getXgFactor().getReason());
        }

        rec.setKeyInsights(insights);

        // Summary
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("%s vs %s\n", rec.getHomeTeam(), rec.getAwayTeam()));
        summary.append(String.format("Confidence: %.1f/10 | EV: %.1f%% | Kelly: %.2f%%\n",
            rec.getConfidence(),
            rec.getExpectedValue() != null ? rec.getExpectedValue() : 0,
            rec.getKellyStake() != null ? rec.getKellyStake() : 0));
        summary.append(String.format("Factors aligned: %d/%d\n\n",
            rec.getFactorsAligned(), rec.getFactorsTotal()));

        for (String insight : insights) {
            summary.append(insight).append("\n");
        }

        rec.setSummary(summary.toString());
    }

    // Helper methods
    private boolean isOutdoorSport(String sport) {
        return List.of("NFL", "CFB", "MLB", "MLS", "SOCCER", "EPL").contains(sport.toUpperCase());
    }

    private String mapConfidenceToAlert(Double confidence) {
        if (confidence >= 9) return "CRITICAL";
        if (confidence >= 7.5) return "HIGH";
        if (confidence >= 6) return "MEDIUM";
        if (confidence >= 4) return "LOW";
        return "NONE";
    }

    private String formatBetDisplay(String direction, MultiFactorRecommendation rec) {
        return switch (direction) {
            case "HOME" -> rec.getHomeTeam() + " to Win @ " + rec.getHomeOdds();
            case "AWAY" -> rec.getAwayTeam() + " to Win @ " + rec.getAwayOdds();
            case "DRAW" -> "Draw @ " + rec.getDrawOdds();
            case "OVER" -> "Over @ " + rec.getOverOdds();
            case "UNDER" -> "Under @ " + rec.getUnderOdds();
            default -> "No Bet";
        };
    }

    /**
     * Input class for game data
     */
    public static class GameInput {
        private String gameId;
        private String sport;
        private String league;
        private String homeTeam;
        private String awayTeam;
        private LocalDateTime gameTime;
        private String venue;
        private Double homeOdds;
        private Double drawOdds;
        private Double awayOdds;
        private Double overOdds;
        private Double underOdds;
        private String sportsbook;
        private Double bankroll;

        // Getters and setters
        public String getGameId() { return gameId; }
        public void setGameId(String gameId) { this.gameId = gameId; }
        public String getSport() { return sport; }
        public void setSport(String sport) { this.sport = sport; }
        public String getLeague() { return league; }
        public void setLeague(String league) { this.league = league; }
        public String getHomeTeam() { return homeTeam; }
        public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }
        public String getAwayTeam() { return awayTeam; }
        public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }
        public LocalDateTime getGameTime() { return gameTime; }
        public void setGameTime(LocalDateTime gameTime) { this.gameTime = gameTime; }
        public String getVenue() { return venue; }
        public void setVenue(String venue) { this.venue = venue; }
        public Double getHomeOdds() { return homeOdds; }
        public void setHomeOdds(Double homeOdds) { this.homeOdds = homeOdds; }
        public Double getDrawOdds() { return drawOdds; }
        public void setDrawOdds(Double drawOdds) { this.drawOdds = drawOdds; }
        public Double getAwayOdds() { return awayOdds; }
        public void setAwayOdds(Double awayOdds) { this.awayOdds = awayOdds; }
        public Double getOverOdds() { return overOdds; }
        public void setOverOdds(Double overOdds) { this.overOdds = overOdds; }
        public Double getUnderOdds() { return underOdds; }
        public void setUnderOdds(Double underOdds) { this.underOdds = underOdds; }
        public String getSportsbook() { return sportsbook; }
        public void setSportsbook(String sportsbook) { this.sportsbook = sportsbook; }
        public Double getBankroll() { return bankroll; }
        public void setBankroll(Double bankroll) { this.bankroll = bankroll; }
    }
}
