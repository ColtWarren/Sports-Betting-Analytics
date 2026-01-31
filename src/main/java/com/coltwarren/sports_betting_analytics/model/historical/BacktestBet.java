package com.coltwarren.sports_betting_analytics.model.historical;

import lombok.Data;
import java.time.LocalDate;

/**
 * Backtest Bet Model
 *
 * Represents a single bet made during backtesting.
 * Tracks all details for post-analysis.
 */
@Data
public class BacktestBet {
    private LocalDate date;
    private String homeTeam;
    private String awayTeam;
    private String betType;         // "H", "D", "A", "OVER", "UNDER"
    private Double odds;
    private Double stake;           // Amount bet
    private Double kellyPercent;
    private String actualResult;
    private Boolean won;
    private Double profit;          // Positive or negative
    private Double bankrollAfter;
    private Double expectedValue;
    private Double confidence;

    /**
     * Get match description
     */
    public String getMatchDescription() {
        return String.format("%s: %s vs %s", date, homeTeam, awayTeam);
    }

    /**
     * Get bet description
     */
    public String getBetDescription() {
        String outcome = won ? "WON" : "LOST";
        return String.format("%s on %s @ %.2f - %s (%.2f)",
            betType, getMatchDescription(), odds, outcome, profit);
    }

    /**
     * Get return on this bet
     */
    public double getReturn() {
        if (stake == null || stake == 0) return 0;
        return profit / stake * 100;
    }
}
