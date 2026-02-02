package com.coltwarren.sports_betting_analytics.model.college;

import lombok.Data;
import java.util.List;

@Data
public class CBBTeamStats {
    private String teamName;
    private String conference;
    private Integer wins;
    private Integer losses;

    // KenPom-style Ratings
    private Double adjEfficiency;      // Adjusted efficiency margin
    private Double adjOffense;         // Adjusted offensive efficiency (pts/100 poss)
    private Double adjDefense;         // Adjusted defensive efficiency
    private Double adjTempo;           // Possessions per 40 minutes

    // National Rankings
    private Integer kenPomRank;
    private Integer apRank;
    private Integer netRank;           // NCAA NET ranking

    // Scoring
    private Double pointsPerGame;
    private Double pointsAllowedPerGame;

    // Four Factors (Dean Oliver's)
    private Double effectiveFGPercent;
    private Double turnoverPercent;
    private Double offReboundPercent;
    private Double freeThrowRate;

    // Defensive Four Factors
    private Double oppEffectiveFGPercent;
    private Double oppTurnoverPercent;
    private Double defReboundPercent;
    private Double oppFreeThrowRate;

    // Strength of Schedule
    private Double sosRating;
    private Integer sosRank;

    // Betting Trends
    private Integer atsWins;
    private Integer atsLosses;
    private Double atsPercentage;
    private Integer overHits;
    private Integer underHits;

    // Splits
    private Double homeAtsPercentage;
    private Double awayAtsPercentage;
    private Double favoriteAtsPercentage;
    private Double underdogAtsPercentage;
    private Double conferenceAtsPercentage;

    // Early Season (Nov-Dec key for finding edges!)
    private Double earlySeasonAtsPercentage;

    // Betting Value Indicators
    private String bettingTrend;
    private Double publicBettingBias;
    private List<String> keyInsights;

    public boolean isRanked() {
        return (apRank != null && apRank <= 25) ||
               (netRank != null && netRank <= 25);
    }

    public boolean isMidMajor() {
        List<String> powerConferences = List.of(
            "SEC", "Big Ten", "Big 12", "ACC", "Pac-12", "Big East"
        );
        return !powerConferences.contains(conference);
    }

    public boolean isSmallConference() {
        List<String> smallConferences = List.of(
            "Horizon", "Big Sky", "MEAC", "SWAC", "Colonial",
            "Missouri Valley", "Summit", "Southland", "Northeast",
            "Patriot", "Ivy", "Atlantic Sun", "Big South", "Big West"
        );
        return smallConferences.contains(conference);
    }
}
