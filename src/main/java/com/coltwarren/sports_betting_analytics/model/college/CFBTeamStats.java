package com.coltwarren.sports_betting_analytics.model.college;

import lombok.Data;
import java.util.List;

@Data
public class CFBTeamStats {
    private String teamName;
    private String conference;
    private String division;          // FBS or FCS
    private Integer wins;
    private Integer losses;

    // Efficiency Ratings (like KenPom for basketball)
    private Double overallRating;
    private Double offenseRating;
    private Double defenseRating;
    private Double specialTeamsRating;

    // Scoring
    private Double pointsPerGame;
    private Double pointsAllowedPerGame;
    private Double pointDifferential;

    // Advanced Stats
    private Double yardsPerPlay;
    private Double yardsAllowedPerPlay;
    private Double turnoversPerGame;
    private Double turnoversForced;

    // Tempo
    private Double playsPerGame;       // Fast or slow team?
    private Double timeOfPossession;

    // Strength of Schedule
    private Double sosRating;          // Strength of schedule
    private Double sosRank;

    // Betting Trends
    private Integer atsWins;           // Against the spread
    private Integer atsLosses;
    private Integer atsPushes;
    private Double atsPercentage;
    private Integer overHits;
    private Integer underHits;

    // Home/Away Splits
    private Double homePointsPerGame;
    private Double awayPointsPerGame;
    private Double homeAtsPercentage;
    private Double awayAtsPercentage;

    // Conference Performance
    private Double conferenceAtsPercentage;
    private Double nonConferenceAtsPercentage;

    // Rankings
    private Integer apRank;
    private Integer cfpRank;           // College Football Playoff ranking
    private Integer coachesRank;

    // Betting Value Indicators
    private String bettingTrend;       // "HOT", "COLD", "NEUTRAL"
    private Double publicBettingBias;  // How much public overvalues them
    private List<String> keyInsights;

    public void calculateAtsPercentage() {
        int total = atsWins + atsLosses;
        if (total > 0) {
            this.atsPercentage = (double) atsWins / total * 100;
        }
    }

    public boolean isRanked() {
        return (apRank != null && apRank <= 25) ||
               (cfpRank != null && cfpRank <= 25);
    }

    public boolean isMidMajor() {
        List<String> powerConferences = List.of(
            "SEC", "Big Ten", "Big 12", "ACC", "Pac-12"
        );
        return !powerConferences.contains(conference);
    }
}
