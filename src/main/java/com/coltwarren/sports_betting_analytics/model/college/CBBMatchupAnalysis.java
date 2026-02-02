package com.coltwarren.sports_betting_analytics.model.college;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;

@Data
public class CBBMatchupAnalysis {
    private String homeTeam;
    private String awayTeam;
    private LocalDate gameDate;

    private CBBTeamStats homeStats;
    private CBBTeamStats awayStats;

    // Spread and total
    private Double spread;
    private Double overUnder;

    // Efficiency analysis
    private Double homeProjectedScore;
    private Double awayProjectedScore;
    private Double projectedTotal;
    private Double projectedSpread;

    // Line value
    private Double spreadValue;
    private Double totalValue;

    // Edge detection
    private Boolean earlySeasonEdge;           // Nov-Dec most inefficient
    private Boolean smallConferenceEdge;       // Less data = more edges
    private Boolean marchMadnessUnderdogEdge;  // Fade Duke/Kentucky/UNC
    private Boolean homeCourtEdge;             // Bigger in college
    private Boolean tempoMismatchEdge;         // Fast vs slow
    private Boolean publicFadeEdge;

    // Recommendation
    private String recommendation;
    private String recommendationDisplay;
    private Double confidence;
    private Double expectedValue;
    private String reason;
    private List<String> edges;

    public CBBMatchupAnalysis() {
        this.edges = new ArrayList<>();
    }

    public void analyze() {
        if (homeStats == null || awayStats == null) {
            this.recommendation = "INSUFFICIENT_DATA";
            this.confidence = 0.0;
            return;
        }

        // Project scores using tempo and efficiency
        Double homeTempo = homeStats.getAdjTempo() != null ? homeStats.getAdjTempo() : 68.0;
        Double awayTempo = awayStats.getAdjTempo() != null ? awayStats.getAdjTempo() : 68.0;
        Double avgTempo = (homeTempo + awayTempo) / 2;

        // Possessions estimate
        Double possessions = avgTempo;

        // Project scores
        if (homeStats.getAdjOffense() != null && awayStats.getAdjDefense() != null) {
            Double homeOffEff = homeStats.getAdjOffense();
            Double awayDefEff = awayStats.getAdjDefense();
            this.homeProjectedScore = (homeOffEff + (100 - awayDefEff)) / 200 * possessions + 3; // +3 home court
        }
        if (awayStats.getAdjOffense() != null && homeStats.getAdjDefense() != null) {
            Double awayOffEff = awayStats.getAdjOffense();
            Double homeDefEff = homeStats.getAdjDefense();
            this.awayProjectedScore = (awayOffEff + (100 - homeDefEff)) / 200 * possessions;
        }

        if (homeProjectedScore != null && awayProjectedScore != null) {
            this.projectedSpread = awayProjectedScore - homeProjectedScore;
            this.projectedTotal = homeProjectedScore + awayProjectedScore;

            if (spread != null) {
                this.spreadValue = spread - projectedSpread;
            }
            if (overUnder != null) {
                this.totalValue = projectedTotal - overUnder;
            }
        }

        checkEdges();
        generateRecommendation();
    }

    private void checkEdges() {
        // 1. Early season edge (November-December)
        if (gameDate != null) {
            int month = gameDate.getMonthValue();
            if (month == 11 || month == 12) {
                this.earlySeasonEdge = true;
                edges.add("Early season (Nov-Dec) - sportsbooks still learning teams, max inefficiency!");
            }
        }

        // 2. Small conference edge
        if (homeStats.isSmallConference() || awayStats.isSmallConference()) {
            this.smallConferenceEdge = true;
            edges.add("Small conference game - limited sportsbook data = more value");
        }

        // 3. March Madness underdog edge
        if (gameDate != null && gameDate.getMonthValue() == 3) {
            List<String> overvaluedPrograms = List.of(
                "Kentucky", "Duke", "North Carolina", "Kansas", "UCLA"
            );
            if (overvaluedPrograms.contains(homeTeam) || overvaluedPrograms.contains(awayTeam)) {
                this.marchMadnessUnderdogEdge = true;
                edges.add("March Madness - blue blood program often overvalued by public");
            }
        }

        // 4. Home court edge (bigger in college than pros)
        if (spread != null && spread > 0 && spread <= 5) {
            this.homeCourtEdge = true;
            edges.add("Home underdog - home court matters more in college");
        }

        // 5. Tempo mismatch
        if (homeStats.getAdjTempo() != null && awayStats.getAdjTempo() != null) {
            double tempoDiff = Math.abs(homeStats.getAdjTempo() - awayStats.getAdjTempo());
            if (tempoDiff > 6) {
                this.tempoMismatchEdge = true;
                if (homeStats.getAdjTempo() < awayStats.getAdjTempo()) {
                    edges.add("Tempo mismatch - slow team at home can control pace, lean UNDER");
                } else {
                    edges.add("Tempo mismatch - fast team at home can push pace, lean OVER");
                }
            }
        }

        // 6. Public fade
        if (homeStats.getPublicBettingBias() != null && homeStats.getPublicBettingBias() > 65) {
            this.publicFadeEdge = true;
            edges.add("Public heavy on " + homeTeam + " - contrarian value on " + awayTeam);
        } else if (awayStats.getPublicBettingBias() != null && awayStats.getPublicBettingBias() > 65) {
            this.publicFadeEdge = true;
            edges.add("Public heavy on " + awayTeam + " - contrarian value on " + homeTeam);
        }
    }

    private void generateRecommendation() {
        double confidence = 5.0;
        String rec = "NO_BET";
        StringBuilder reason = new StringBuilder();

        // Strong spread value
        if (spreadValue != null && Math.abs(spreadValue) >= 3) {
            if (spreadValue > 0) {
                rec = "HOME_SPREAD";
                this.recommendationDisplay = homeTeam + " +" + Math.abs(spread);
            } else {
                rec = "AWAY_SPREAD";
                this.recommendationDisplay = awayTeam + " " + spread;
            }
            confidence = 6.0 + Math.min(2, Math.abs(spreadValue) / 2);
            reason.append(String.format("Spread value of %.1f points. ", spreadValue));
        }

        // Total value with tempo mismatch
        if (tempoMismatchEdge != null && tempoMismatchEdge && totalValue != null) {
            if (totalValue < -3) {
                rec = "UNDER";
                this.recommendationDisplay = "Under " + overUnder;
                confidence = 6.5;
                reason.append("Tempo mismatch + total value suggests UNDER. ");
            } else if (totalValue > 3) {
                rec = "OVER";
                this.recommendationDisplay = "Over " + overUnder;
                confidence = 6.5;
                reason.append("Tempo mismatch + total value suggests OVER. ");
            }
        }

        // Edge bonuses
        if (earlySeasonEdge != null && earlySeasonEdge) confidence += 1.0;
        if (smallConferenceEdge != null && smallConferenceEdge) confidence += 0.5;
        if (marchMadnessUnderdogEdge != null && marchMadnessUnderdogEdge) confidence += 0.5;
        if (homeCourtEdge != null && homeCourtEdge) confidence += 0.5;
        if (publicFadeEdge != null && publicFadeEdge) confidence += 1.0;

        this.recommendation = rec;
        if (recommendationDisplay == null) {
            this.recommendationDisplay = rec;
        }
        this.confidence = Math.min(10, confidence);
        this.reason = reason.toString() + String.join(" ", edges);
    }
}
