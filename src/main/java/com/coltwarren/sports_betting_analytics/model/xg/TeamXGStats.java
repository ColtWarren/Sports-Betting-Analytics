package com.coltwarren.sports_betting_analytics.model.xg;

import lombok.Data;
import java.time.LocalDate;

/**
 * Team Expected Goals (xG) Statistics
 *
 * xG measures shot quality - teams over/under-performing their xG tend to regress.
 * This is a proven edge in soccer analytics for betting.
 */
@Data
public class TeamXGStats {
    private String teamName;
    private String league;
    private String season;

    // Goals
    private Integer matchesPlayed;
    private Integer goalsScored;
    private Integer goalsConceded;

    // Expected Goals
    private Double xGFor;           // Expected goals scored
    private Double xGAgainst;       // Expected goals conceded

    // Differentials (key for betting!)
    private Double xGDiff;          // xGFor - xGAgainst (overall quality)
    private Double goalsVsXG;       // Actual goals - xG (luck factor)
    private Double concededVsXGA;   // Actual conceded - xGA

    // Per game averages
    private Double xGPerGame;
    private Double xGAgainstPerGame;
    private Double goalsPerGame;
    private Double concededPerGame;

    // Form (last 5 games)
    private Double recentXG;        // xG in last 5 games
    private Double recentGoals;     // Goals in last 5 games
    private Double recentXGDiff;    // Recent xG differential

    // Betting signals
    private String performanceStatus;  // "OVERPERFORMING", "UNDERPERFORMING", "NEUTRAL"
    private Double regressionLikelihood; // 0-100% chance of regression
    private String bettingSignal;      // "BET_FOR", "FADE", "NEUTRAL"

    private LocalDate lastUpdated;

    public void calculateDifferentials() {
        if (xGFor != null && goalsScored != null) {
            this.goalsVsXG = goalsScored - xGFor;
        }
        if (xGAgainst != null && goalsConceded != null) {
            this.concededVsXGA = goalsConceded - xGAgainst;
        }
        if (xGFor != null && xGAgainst != null) {
            this.xGDiff = xGFor - xGAgainst;
        }
        if (matchesPlayed != null && matchesPlayed > 0) {
            this.xGPerGame = xGFor / matchesPlayed;
            this.xGAgainstPerGame = xGAgainst / matchesPlayed;
            this.goalsPerGame = (double) goalsScored / matchesPlayed;
            this.concededPerGame = (double) goalsConceded / matchesPlayed;
        }

        determinePerformanceStatus();
    }

    private void determinePerformanceStatus() {
        if (goalsVsXG == null) {
            this.performanceStatus = "NEUTRAL";
            this.bettingSignal = "NEUTRAL";
            this.regressionLikelihood = 50.0;
            return;
        }

        // Significant over/under performance threshold: 0.3 goals per game
        double threshold = matchesPlayed != null ? matchesPlayed * 0.3 : 3.0;

        if (goalsVsXG > threshold) {
            this.performanceStatus = "OVERPERFORMING";
            this.bettingSignal = "FADE";  // They're due for regression down
            this.regressionLikelihood = Math.min(90, 50 + (goalsVsXG * 10));
        } else if (goalsVsXG < -threshold) {
            this.performanceStatus = "UNDERPERFORMING";
            this.bettingSignal = "BET_FOR";  // They're due for regression up
            this.regressionLikelihood = Math.min(90, 50 + (Math.abs(goalsVsXG) * 10));
        } else {
            this.performanceStatus = "NEUTRAL";
            this.bettingSignal = "NEUTRAL";
            this.regressionLikelihood = 50.0;
        }
    }
}
