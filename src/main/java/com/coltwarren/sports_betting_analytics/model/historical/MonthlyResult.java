package com.coltwarren.sports_betting_analytics.model.historical;

import lombok.Data;

/**
 * Monthly Result Model
 *
 * Tracks betting performance for a single month.
 * Used for analyzing consistency and trends over time.
 */
@Data
public class MonthlyResult {
    private String month;           // "2024-01"
    private Integer bets;
    private Integer wins;
    private Double profit;
    private Double roi;
    private Double endingBankroll;

    public MonthlyResult() {
        this.bets = 0;
        this.wins = 0;
        this.profit = 0.0;
    }

    /**
     * Get win rate for this month
     */
    public double getWinRate() {
        if (bets == null || bets == 0) return 0;
        return (double) wins / bets * 100;
    }

    /**
     * Check if month was profitable
     */
    public boolean isProfitable() {
        return profit != null && profit > 0;
    }

    /**
     * Get losses count
     */
    public int getLosses() {
        return bets != null && wins != null ? bets - wins : 0;
    }
}
