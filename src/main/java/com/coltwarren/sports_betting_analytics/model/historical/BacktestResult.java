package com.coltwarren.sports_betting_analytics.model.historical;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

/**
 * Backtest Result Model
 *
 * Contains the complete results of running a betting strategy
 * against historical data, including financial stats and individual bets.
 */
@Data
public class BacktestResult {
    private String strategyName;
    private String league;
    private String dateRange;
    private Integer totalMatches;

    // Betting stats
    private Integer totalBets;
    private Integer wins;
    private Integer losses;
    private Double winRate;

    // Financial stats
    private Double startingBankroll;
    private Double finalBankroll;
    private Double totalProfit;
    private Double roi;                 // Return on Investment %
    private Double maxDrawdown;         // Worst losing streak %
    private Double sharpeRatio;         // Risk-adjusted return

    // Bet breakdown
    private Integer homeBets;
    private Integer drawBets;
    private Integer awayBets;
    private Double avgOdds;
    private Double avgKellyPercent;

    // Monthly breakdown
    private List<MonthlyResult> monthlyResults = new ArrayList<>();

    // Individual bets (for analysis)
    private List<BacktestBet> bets = new ArrayList<>();

    public BacktestResult() {
        this.totalBets = 0;
        this.wins = 0;
        this.losses = 0;
        this.homeBets = 0;
        this.drawBets = 0;
        this.awayBets = 0;
    }

    public void calculateStats() {
        if (totalBets != null && totalBets > 0) {
            this.winRate = (double) wins / totalBets * 100;
            this.totalProfit = finalBankroll - startingBankroll;
            this.roi = (totalProfit / startingBankroll) * 100;
        }
    }

    /**
     * Get summary string for display
     */
    public String getSummary() {
        return String.format("%s on %s: %d bets, %.1f%% win rate, %.1f%% ROI ($%.2f -> $%.2f)",
            strategyName, league, totalBets, winRate, roi, startingBankroll, finalBankroll);
    }

    /**
     * Check if strategy was profitable
     */
    public boolean isProfitable() {
        return roi != null && roi > 0;
    }

    /**
     * Get profit/loss amount
     */
    public double getProfitLoss() {
        return finalBankroll != null && startingBankroll != null ?
            finalBankroll - startingBankroll : 0.0;
    }
}
