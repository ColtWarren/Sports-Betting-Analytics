package com.coltwarren.sports_betting_analytics.model.college;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class CFBMatchupAnalysis {
    private CFBGame game;
    private CFBTeamStats homeStats;
    private CFBTeamStats awayStats;

    // Efficiency comparison
    private Double homeOffenseVsAwayDefense;
    private Double awayOffenseVsHomeDefense;
    private Double projectedHomeScore;
    private Double projectedAwayScore;
    private Double projectedSpread;
    private Double projectedTotal;

    // Edge detection
    private Double lineValue;          // Our projected spread vs actual spread
    private Double totalValue;         // Our projected total vs actual total

    // Pattern-based edges
    private Boolean rankedMatchupEdge;         // Favorites 8.5 or less cover 60%+
    private Boolean midMajorEdge;              // Less efficient market
    private Boolean serviceAcademyUnderEdge;   // 79% win rate on unders
    private Boolean homeUnderdogEdge;
    private Boolean publicFadeEdge;

    // Betting recommendation
    private String recommendation;     // "HOME_SPREAD", "AWAY_SPREAD", "OVER", "UNDER", "NO_BET"
    private String recommendationDisplay;
    private Double confidence;         // 1-10
    private Double expectedValue;
    private String reason;
    private List<String> edges;

    public CFBMatchupAnalysis() {
        this.edges = new ArrayList<>();
    }

    public void analyze() {
        if (homeStats == null || awayStats == null) {
            this.recommendation = "INSUFFICIENT_DATA";
            this.confidence = 0.0;
            return;
        }

        // Project scores based on efficiency
        if (homeStats.getOffenseRating() != null && awayStats.getDefenseRating() != null) {
            this.homeOffenseVsAwayDefense = homeStats.getOffenseRating() - awayStats.getDefenseRating();
            this.projectedHomeScore = 28 + homeOffenseVsAwayDefense * 3; // Base 28 + adjustment
        }
        if (awayStats.getOffenseRating() != null && homeStats.getDefenseRating() != null) {
            this.awayOffenseVsHomeDefense = awayStats.getOffenseRating() - homeStats.getDefenseRating();
            this.projectedAwayScore = 24 + awayOffenseVsHomeDefense * 3; // Base 24 (no home advantage)
        }

        if (projectedHomeScore != null && projectedAwayScore != null) {
            this.projectedSpread = projectedAwayScore - projectedHomeScore; // Positive = home favorite
            this.projectedTotal = projectedHomeScore + projectedAwayScore;
        }

        // Calculate line value
        if (projectedSpread != null && game.getSpread() != null) {
            this.lineValue = game.getSpread() - projectedSpread;
        }
        if (projectedTotal != null && game.getOverUnder() != null) {
            this.totalValue = projectedTotal - game.getOverUnder();
        }

        // Check for pattern edges
        checkPatternEdges();

        // Generate recommendation
        generateRecommendation();
    }

    private void checkPatternEdges() {
        // 1. Ranked matchup edge (favorites 8.5 or less cover 60%+)
        if (game.getIsRankedMatchup() != null && game.getIsRankedMatchup() &&
            game.getSpread() != null && Math.abs(game.getSpread()) <= 8.5) {
            this.rankedMatchupEdge = true;
            edges.add("Ranked matchup with spread <=8.5 - favorites cover 60%+ historically");
        }

        // 2. Mid-major edge
        if ((homeStats.isMidMajor() || awayStats.isMidMajor()) &&
            (game.getIsRankedMatchup() == null || !game.getIsRankedMatchup())) {
            this.midMajorEdge = true;
            edges.add("Mid-major matchup - less efficient betting market");
        }

        // 3. Service Academy under edge
        List<String> serviceAcademies = List.of("Army", "Navy", "Air Force");
        if (serviceAcademies.contains(game.getHomeTeam()) || serviceAcademies.contains(game.getAwayTeam())) {
            this.serviceAcademyUnderEdge = true;
            edges.add("Service Academy game - UNDER is 46-12-2 (79%) historically!");
        }

        // 4. Home underdog edge
        if (game.getSpread() != null && game.getSpread() > 0 && game.getSpread() <= 7) {
            this.homeUnderdogEdge = true;
            edges.add("Home underdog of 7 or less - historically profitable");
        }

        // 5. Public fade edge
        if (homeStats.getPublicBettingBias() != null && homeStats.getPublicBettingBias() > 65) {
            this.publicFadeEdge = true;
            edges.add("Public heavy on " + game.getHomeTeam() + " - contrarian value on " + game.getAwayTeam());
        } else if (awayStats.getPublicBettingBias() != null && awayStats.getPublicBettingBias() > 65) {
            this.publicFadeEdge = true;
            edges.add("Public heavy on " + game.getAwayTeam() + " - contrarian value on " + game.getHomeTeam());
        }
    }

    private void generateRecommendation() {
        double confidence = 5.0;
        String rec = "NO_BET";
        StringBuilder reason = new StringBuilder();

        // Service Academy under is strongest edge
        if (serviceAcademyUnderEdge != null && serviceAcademyUnderEdge) {
            rec = "UNDER";
            this.recommendationDisplay = "UNDER " + game.getOverUnder();
            confidence = 8.5;
            reason.append("Service Academy matchup - 79% under rate historically. ");
        }

        // Ranked matchup favorite edge
        else if (rankedMatchupEdge != null && rankedMatchupEdge) {
            double spread = game.getSpread();
            if (spread < 0) { // Home is favorite
                rec = "HOME_SPREAD";
                this.recommendationDisplay = game.getHomeTeam() + " " + spread;
            } else { // Away is favorite
                rec = "AWAY_SPREAD";
                this.recommendationDisplay = game.getAwayTeam() + " -" + Math.abs(spread);
            }
            confidence = 7.0;
            reason.append("Ranked matchup, favorite by <=8.5 covers 60%+. ");
        }

        // Line value
        else if (lineValue != null && Math.abs(lineValue) >= 3) {
            if (lineValue > 0) {
                rec = "HOME_SPREAD";
                this.recommendationDisplay = game.getHomeTeam() + " " + game.getSpread();
            } else {
                rec = "AWAY_SPREAD";
                this.recommendationDisplay = game.getAwayTeam() + " " + (-game.getSpread());
            }
            confidence = 6.0 + Math.min(2, Math.abs(lineValue) / 2);
            reason.append(String.format("Line value of %.1f points. ", lineValue));
        }

        // Add edge bonuses to confidence
        if (midMajorEdge != null && midMajorEdge) confidence += 0.5;
        if (homeUnderdogEdge != null && homeUnderdogEdge) confidence += 0.5;
        if (publicFadeEdge != null && publicFadeEdge) confidence += 1.0;

        this.recommendation = rec;
        if (recommendationDisplay == null) {
            this.recommendationDisplay = rec;
        }
        this.confidence = Math.min(10, confidence);
        this.reason = reason.toString() + String.join(" ", edges);
    }
}
