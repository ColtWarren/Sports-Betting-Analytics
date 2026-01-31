package com.coltwarren.sports_betting_analytics.model.historical;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * Historical Match Entity
 *
 * Stores historical soccer match data with betting odds from Football-Data.co.uk.
 * Used for backtesting Kelly Criterion and other betting strategies.
 */
@Entity
@Table(name = "historical_matches", indexes = {
    @Index(name = "idx_match_date", columnList = "matchDate"),
    @Index(name = "idx_match_league", columnList = "league"),
    @Index(name = "idx_match_season", columnList = "season")
})
@Data
public class HistoricalMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String league;          // "EPL", "LA_LIGA", "SERIE_A", etc.
    private String season;          // "2023-24", "2022-23", etc.
    private LocalDate matchDate;

    private String homeTeam;
    private String awayTeam;

    // Full-time result
    private Integer homeGoals;
    private Integer awayGoals;
    private String result;          // "H", "D", "A" (Home win, Draw, Away win)

    // Betting odds (Bet365 as primary)
    private Double homeOdds;        // e.g., 1.85
    private Double drawOdds;        // e.g., 3.40
    private Double awayOdds;        // e.g., 4.20

    // Over/Under odds
    private Double over25Odds;
    private Double under25Odds;

    // Additional bookmaker odds for comparison
    private Double b365Home;
    private Double b365Draw;
    private Double b365Away;
    private Double bwinHome;
    private Double bwinDraw;
    private Double bwinAway;

    // Calculated fields
    private Double homeImpliedProb;  // 1 / homeOdds
    private Double drawImpliedProb;
    private Double awayImpliedProb;

    @PrePersist
    @PreUpdate
    public void calculateImpliedProbs() {
        if (homeOdds != null && homeOdds > 0) {
            this.homeImpliedProb = 1.0 / homeOdds;
        }
        if (drawOdds != null && drawOdds > 0) {
            this.drawImpliedProb = 1.0 / drawOdds;
        }
        if (awayOdds != null && awayOdds > 0) {
            this.awayImpliedProb = 1.0 / awayOdds;
        }
    }

    /**
     * Get total goals scored
     */
    public int getTotalGoals() {
        return (homeGoals != null ? homeGoals : 0) + (awayGoals != null ? awayGoals : 0);
    }

    /**
     * Check if match went over 2.5 goals
     */
    public boolean isOver25() {
        return getTotalGoals() > 2;
    }

    /**
     * Get display string
     */
    public String getDisplayText() {
        return String.format("%s: %s %d-%d %s",
            matchDate, homeTeam, homeGoals, awayGoals, awayTeam);
    }
}
