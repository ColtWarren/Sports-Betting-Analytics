package com.coltwarren.sports_betting_analytics.model.xg;

import lombok.Data;

/**
 * Match xG Analysis
 *
 * Analyzes a specific match using xG data from both teams
 * to generate betting recommendations based on regression theory.
 */
@Data
public class MatchXGAnalysis {
    private String homeTeam;
    private String awayTeam;
    private String league;

    // Team xG stats
    private TeamXGStats homeXGStats;
    private TeamXGStats awayXGStats;

    // Head-to-head xG comparison
    private Double homeXGAdvantage;     // Home xG/game - Away xGA/game
    private Double awayXGAdvantage;     // Away xG/game - Home xGA/game
    private Double projectedHomeGoals;
    private Double projectedAwayGoals;
    private Double projectedTotal;

    // Regression factors
    private String homeRegressionSignal;  // "POSITIVE", "NEGATIVE", "NEUTRAL"
    private String awayRegressionSignal;
    private Double homeRegressionStrength; // 0-10
    private Double awayRegressionStrength;

    // Betting recommendations
    private String xgRecommendation;      // "HOME", "AWAY", "DRAW", "OVER", "UNDER"
    private String xgReason;
    private Double xgConfidence;          // 1-10
    private String alertLevel;            // "LOW", "MEDIUM", "HIGH"

    public void analyze() {
        if (homeXGStats == null || awayXGStats == null) {
            this.xgRecommendation = "INSUFFICIENT_DATA";
            this.xgConfidence = 0.0;
            return;
        }

        // Calculate advantages
        this.homeXGAdvantage = (homeXGStats.getXGPerGame() != null ? homeXGStats.getXGPerGame() : 0)
                            - (awayXGStats.getXGAgainstPerGame() != null ? awayXGStats.getXGAgainstPerGame() : 0);
        this.awayXGAdvantage = (awayXGStats.getXGPerGame() != null ? awayXGStats.getXGPerGame() : 0)
                            - (homeXGStats.getXGAgainstPerGame() != null ? homeXGStats.getXGAgainstPerGame() : 0);

        // Project goals (simplified model)
        this.projectedHomeGoals = homeXGAdvantage + 1.3; // Home advantage ~0.3 goals
        this.projectedAwayGoals = awayXGAdvantage + 1.0;
        this.projectedTotal = projectedHomeGoals + projectedAwayGoals;

        // Analyze regression
        analyzeRegression();

        // Generate recommendation
        generateRecommendation();
    }

    private void analyzeRegression() {
        // Home team regression
        if ("OVERPERFORMING".equals(homeXGStats.getPerformanceStatus())) {
            this.homeRegressionSignal = "NEGATIVE";
            this.homeRegressionStrength = Math.min(10, homeXGStats.getRegressionLikelihood() / 10);
        } else if ("UNDERPERFORMING".equals(homeXGStats.getPerformanceStatus())) {
            this.homeRegressionSignal = "POSITIVE";
            this.homeRegressionStrength = Math.min(10, homeXGStats.getRegressionLikelihood() / 10);
        } else {
            this.homeRegressionSignal = "NEUTRAL";
            this.homeRegressionStrength = 0.0;
        }

        // Away team regression
        if ("OVERPERFORMING".equals(awayXGStats.getPerformanceStatus())) {
            this.awayRegressionSignal = "NEGATIVE";
            this.awayRegressionStrength = Math.min(10, awayXGStats.getRegressionLikelihood() / 10);
        } else if ("UNDERPERFORMING".equals(awayXGStats.getPerformanceStatus())) {
            this.awayRegressionSignal = "POSITIVE";
            this.awayRegressionStrength = Math.min(10, awayXGStats.getRegressionLikelihood() / 10);
        } else {
            this.awayRegressionSignal = "NEUTRAL";
            this.awayRegressionStrength = 0.0;
        }
    }

    private void generateRecommendation() {
        double confidence = 5.0;
        String recommendation = "NEUTRAL";
        StringBuilder reason = new StringBuilder();

        // Factor 1: xG advantage
        double xgDiff = homeXGAdvantage - awayXGAdvantage;
        if (Math.abs(xgDiff) > 0.5) {
            if (xgDiff > 0) {
                recommendation = "HOME";
                reason.append(String.format("Home has %.2f xG advantage. ", xgDiff));
            } else {
                recommendation = "AWAY";
                reason.append(String.format("Away has %.2f xG advantage. ", -xgDiff));
            }
            confidence += Math.min(2, Math.abs(xgDiff));
        }

        // Factor 2: Regression
        if ("POSITIVE".equals(homeRegressionSignal) && homeRegressionStrength > 5) {
            if (!"HOME".equals(recommendation)) {
                recommendation = "HOME";
            }
            confidence += homeRegressionStrength / 3;
            reason.append(String.format("Home underperforming xG by %.1f - due for positive regression. ",
                Math.abs(homeXGStats.getGoalsVsXG())));
        }
        if ("NEGATIVE".equals(homeRegressionSignal) && homeRegressionStrength > 5) {
            if ("HOME".equals(recommendation)) {
                recommendation = "AWAY";
                confidence -= 1;
            }
            reason.append(String.format("Home overperforming xG by %.1f - due for regression. ",
                homeXGStats.getGoalsVsXG()));
        }

        if ("POSITIVE".equals(awayRegressionSignal) && awayRegressionStrength > 5) {
            if (!"AWAY".equals(recommendation)) {
                recommendation = "AWAY";
            }
            confidence += awayRegressionStrength / 3;
            reason.append("Away underperforming xG - due for positive regression. ");
        }
        if ("NEGATIVE".equals(awayRegressionSignal) && awayRegressionStrength > 5) {
            if ("AWAY".equals(recommendation)) {
                recommendation = "HOME";
                confidence -= 1;
            }
            reason.append("Away overperforming xG - due for regression. ");
        }

        // Factor 3: Totals
        if (projectedTotal > 2.8) {
            reason.append(String.format("Projected %.1f total goals - lean OVER. ", projectedTotal));
        } else if (projectedTotal < 2.2) {
            reason.append(String.format("Projected %.1f total goals - lean UNDER. ", projectedTotal));
        }

        this.xgRecommendation = recommendation;
        this.xgReason = reason.toString().trim();
        this.xgConfidence = Math.min(10, Math.max(1, confidence));
        this.alertLevel = confidence >= 7 ? "HIGH" : confidence >= 5 ? "MEDIUM" : "LOW";
    }
}
