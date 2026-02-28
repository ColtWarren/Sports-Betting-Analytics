package com.coltwarren.sports_betting_analytics.service.analyzer;

import com.coltwarren.sports_betting_analytics.model.soccer.SoccerFixture;
import com.coltwarren.sports_betting_analytics.model.xg.MatchXGAnalysis;
import com.coltwarren.sports_betting_analytics.service.soccer.SoccerDataAggregatorService;
import com.coltwarren.sports_betting_analytics.service.xg.XGDataService;
import com.coltwarren.sports_betting_analytics.util.OddsConversionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Analyzer for soccer bets using 3-way betting (Home/Draw/Away).
 * Uses xG-based Poisson model for probability estimation.
 */
@Service
@Slf4j
public class SoccerBetAnalyzer {

    private final SoccerDataAggregatorService soccerDataAggregator;
    private final XGDataService xgDataService;

    public SoccerBetAnalyzer(SoccerDataAggregatorService soccerDataAggregator,
                             XGDataService xgDataService) {
        this.soccerDataAggregator = soccerDataAggregator;
        this.xgDataService = xgDataService;
    }

    /**
     * Analyze all soccer games across leagues.
     */
    public List<Map<String, Object>> analyze() {
        List<Map<String, Object>> soccerBets = new ArrayList<>();

        try {
            log.info("Analyzing soccer games (3-way betting)...");

            Map<String, List<SoccerFixture>> allFixtures = soccerDataAggregator.getAllFixturesFromOddsOnly();

            if (allFixtures.isEmpty()) {
                log.info("No soccer fixtures found");
                return soccerBets;
            }

            int totalFixtures = allFixtures.values().stream().mapToInt(List::size).sum();
            log.info("Found {} soccer fixtures across {} leagues", totalFixtures, allFixtures.size());

            int gamesAnalyzed = 0;
            int maxGamesPerLeague = 3;

            for (Map.Entry<String, List<SoccerFixture>> entry : allFixtures.entrySet()) {
                String league = entry.getKey();
                List<SoccerFixture> fixtures = entry.getValue();

                int leagueGames = 0;
                for (SoccerFixture fixture : fixtures) {
                    if (leagueGames >= maxGamesPerLeague) break;

                    try {
                        analyzeOutcome(fixture, "HOME_WIN", fixture.getHomeWinOdds(),
                            fixture.getBestHomeBook(), league, soccerBets);
                        analyzeOutcome(fixture, "DRAW", fixture.getDrawOdds(),
                            fixture.getBestDrawBook(), league, soccerBets);
                        analyzeOutcome(fixture, "AWAY_WIN", fixture.getAwayWinOdds(),
                            fixture.getBestAwayBook(), league, soccerBets);

                        leagueGames++;
                        gamesAnalyzed++;

                    } catch (Exception e) {
                        log.error("Error analyzing soccer fixture: {}", e.getMessage());
                    }
                }
            }

            log.info("Analyzed {} soccer games, found {} quality bets", gamesAnalyzed, soccerBets.size());

        } catch (Exception e) {
            log.error("Error analyzing soccer: {}", e.getMessage());
        }

        return soccerBets;
    }

    private void analyzeOutcome(SoccerFixture fixture, String outcome, Double odds,
                                String sportsbook, String league, List<Map<String, Object>> allBets) {
        if (odds == null) return;

        try {
            MatchXGAnalysis xgAnalysis = null;
            try {
                xgAnalysis = xgDataService.analyzeMatch(
                    fixture.getHomeTeam(), fixture.getAwayTeam(), league);
            } catch (Exception e) {
                log.warn("xG analysis unavailable: {}", e.getMessage());
            }

            double winProbability = getModelProbability(fixture, outcome, xgAnalysis);
            double decimalOdds = OddsConversionUtils.americanToDecimal(odds.intValue());

            // Calculate Expected Value (capped at realistic max)
            double rawExpectedValue = (winProbability * decimalOdds) - 1.0;
            double expectedValue = Math.min(rawExpectedValue, 0.25);
            if (rawExpectedValue > 0.25) {
                log.warn("EV capped: {}% -> 25% for {} vs {} ({})",
                    String.format("%.1f", rawExpectedValue * 100),
                    fixture.getHomeTeam(), fixture.getAwayTeam(), outcome);
            }

            if (expectedValue < -0.02) return;

            // Kelly Criterion for 3-way betting
            double kellyFraction = (winProbability * decimalOdds - 1) / (decimalOdds - 1);
            double kellyPercentage = Math.max(0, Math.min(kellyFraction * 25, 5.0));

            // Calculate confidence from edge
            double impliedProbability = 1.0 / decimalOdds;
            double edge = winProbability - impliedProbability;
            double confidence = 5.0 + (edge * 100);
            if (odds.intValue() > 400) {
                confidence = Math.min(confidence, 8.0);
            } else if (odds.intValue() > 200) {
                confidence = Math.min(confidence, 9.0);
            }
            confidence = Math.min(Math.max(confidence, 1.0), 10.0);

            if (confidence < 6.0) return;

            String recommendation = formatPick(outcome, fixture);

            Map<String, Object> bet = new HashMap<>();
            bet.put("sport", "SOCCER");
            bet.put("league", league);
            bet.put("homeTeam", fixture.getHomeTeam());
            bet.put("awayTeam", fixture.getAwayTeam());
            bet.put("gameTime", fixture.getKickoffTime());
            bet.put("recommendation", recommendation);
            bet.put("confidence", confidence);
            bet.put("kellyPercent", kellyPercentage);
            bet.put("isLocalTeam", fixture.isLocalTeam());

            // Best odds map with all 3 outcomes
            Map<String, Object> bestOdds = new HashMap<>();
            bestOdds.put("odds", odds.intValue());
            bestOdds.put("book", sportsbook);
            bestOdds.put("outcome", outcome);
            if (fixture.getHomeWinOdds() != null) {
                bestOdds.put("homeWinOdds", fixture.getHomeWinOdds().intValue());
                bestOdds.put("homeWinBook", fixture.getBestHomeBook());
            }
            if (fixture.getDrawOdds() != null) {
                bestOdds.put("drawOdds", fixture.getDrawOdds().intValue());
                bestOdds.put("drawBook", fixture.getBestDrawBook());
            }
            if (fixture.getAwayWinOdds() != null) {
                bestOdds.put("awayWinOdds", fixture.getAwayWinOdds().intValue());
                bestOdds.put("awayWinBook", fixture.getBestAwayBook());
            }
            bet.put("bestOdds", bestOdds);

            // xG analysis for display
            if (xgAnalysis != null) {
                bet.put("xgAnalysis", buildXGData(xgAnalysis));
            }

            // Analysis text
            String analysis = String.format(
                "RECOMMENDATION: %s | CONFIDENCE: %.1f | REASON: %s with %.1f%% implied probability. EV: %.2f%%.",
                recommendation, confidence, outcome,
                impliedProbability * 100, expectedValue * 100
            );
            bet.put("analysis", analysis);

            allBets.add(bet);

        } catch (Exception e) {
            log.error("Error analyzing soccer outcome: {}", e.getMessage());
        }
    }

    private double getModelProbability(SoccerFixture fixture, String outcome, MatchXGAnalysis xgAnalysis) {
        Double odds = switch (outcome) {
            case "HOME_WIN" -> fixture.getHomeWinOdds();
            case "DRAW" -> fixture.getDrawOdds();
            case "AWAY_WIN" -> fixture.getAwayWinOdds();
            default -> null;
        };

        if (odds == null) return 0.0;

        double impliedProb = OddsConversionUtils.oddsToImpliedProbability(odds.intValue());

        // Use xG-based Poisson model when available
        if (xgAnalysis != null && xgAnalysis.getProjectedHomeGoals() != null
                && xgAnalysis.getProjectedAwayGoals() != null) {
            double poissonProb = calculatePoissonOutcome(
                xgAnalysis.getProjectedHomeGoals(),
                xgAnalysis.getProjectedAwayGoals(),
                outcome);

            // Dynamic blend: trust market more for longshots
            double modelWeight = 0.30;
            double marketWeight = 0.70;
            if (odds.intValue() > 600) {
                modelWeight = 0.15;
                marketWeight = 0.85;
            } else if (odds.intValue() > 400) {
                modelWeight = 0.20;
                marketWeight = 0.80;
            }
            double blendedProb = (poissonProb * modelWeight) + (impliedProb * marketWeight);
            return Math.max(0.05, Math.min(0.80, blendedProb));
        }

        // Fallback: implied probability with home advantage
        if ("HOME_WIN".equals(outcome)) {
            impliedProb += impliedProb * 0.10;
        }
        return Math.max(0.05, Math.min(0.80, impliedProb));
    }

    private double calculatePoissonOutcome(double homeXG, double awayXG, String outcome) {
        int maxGoals = 8;
        double prob = 0.0;

        for (int h = 0; h <= maxGoals; h++) {
            for (int a = 0; a <= maxGoals; a++) {
                double matchProb = poissonProbability(homeXG, h) * poissonProbability(awayXG, a);
                switch (outcome) {
                    case "HOME_WIN" -> { if (h > a) prob += matchProb; }
                    case "DRAW" -> { if (h == a) prob += matchProb; }
                    case "AWAY_WIN" -> { if (a > h) prob += matchProb; }
                }
            }
        }
        return prob;
    }

    private double poissonProbability(double lambda, int k) {
        double factorial = 1;
        for (int i = 2; i <= k; i++) factorial *= i;
        return Math.exp(-lambda) * Math.pow(lambda, k) / factorial;
    }

    private String formatPick(String outcome, SoccerFixture fixture) {
        return switch (outcome) {
            case "HOME_WIN" -> fixture.getHomeTeam() + " to Win";
            case "DRAW" -> "Draw";
            case "AWAY_WIN" -> fixture.getAwayTeam() + " to Win";
            default -> outcome;
        };
    }

    private Map<String, Object> buildXGData(MatchXGAnalysis xgAnalysis) {
        Map<String, Object> xgData = new HashMap<>();
        xgData.put("projectedTotal", xgAnalysis.getProjectedTotal());
        xgData.put("xgRecommendation", xgAnalysis.getXgRecommendation());
        xgData.put("xgConfidence", xgAnalysis.getXgConfidence());
        xgData.put("xgReason", xgAnalysis.getXgReason());

        if (xgAnalysis.getHomeXGStats() != null) {
            Map<String, Object> homeXG = new HashMap<>();
            homeXG.put("xGPerGame", xgAnalysis.getHomeXGStats().getXGPerGame());
            homeXG.put("goalsVsXG", xgAnalysis.getHomeXGStats().getGoalsVsXG());
            homeXG.put("performanceStatus", xgAnalysis.getHomeXGStats().getPerformanceStatus());
            homeXG.put("bettingSignal", xgAnalysis.getHomeXGStats().getBettingSignal());
            homeXG.put("regressionLikelihood", xgAnalysis.getHomeXGStats().getRegressionLikelihood());
            xgData.put("homeXGStats", homeXG);
        }

        if (xgAnalysis.getAwayXGStats() != null) {
            Map<String, Object> awayXG = new HashMap<>();
            awayXG.put("xGPerGame", xgAnalysis.getAwayXGStats().getXGPerGame());
            awayXG.put("goalsVsXG", xgAnalysis.getAwayXGStats().getGoalsVsXG());
            awayXG.put("performanceStatus", xgAnalysis.getAwayXGStats().getPerformanceStatus());
            awayXG.put("bettingSignal", xgAnalysis.getAwayXGStats().getBettingSignal());
            awayXG.put("regressionLikelihood", xgAnalysis.getAwayXGStats().getRegressionLikelihood());
            xgData.put("awayXGStats", awayXG);
        }

        return xgData;
    }
}
