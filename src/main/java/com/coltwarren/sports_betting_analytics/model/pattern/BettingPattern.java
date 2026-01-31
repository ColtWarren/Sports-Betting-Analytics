package com.coltwarren.sports_betting_analytics.model.pattern;

import lombok.Data;
import java.util.List;

/**
 * Betting Pattern Model
 *
 * Represents a discovered profitable betting pattern from historical data.
 * Patterns are validated through backtesting before being used for predictions.
 */
@Data
public class BettingPattern {
    private String patternId;
    private String name;
    private String description;
    private String sport;
    private String league;

    // Pattern criteria
    private String betType;              // "H", "D", "A", "OVER_2.5", "UNDER_2.5", "DOUBLE_CHANCE_HD"
    private Double minOdds;
    private Double maxOdds;
    private List<String> conditions;     // Human-readable conditions

    // Performance stats (from backtesting)
    private Integer sampleSize;          // Number of historical matches
    private Integer wins;
    private Integer losses;
    private Double winRate;
    private Double roi;                  // Return on Investment %
    private Double avgOdds;
    private Double profitUnits;          // Total profit in units

    // Confidence metrics
    private Double confidence;           // 1-10 scale
    private Double consistency;          // How steady across seasons
    private Boolean isStatisticallySignificant;  // p-value < 0.05

    // Status
    private Boolean isActive;            // Currently profitable?
    private String lastValidated;        // When last checked

    /**
     * Calculate derived statistics
     */
    public void calculateStats() {
        if (sampleSize != null && sampleSize > 0) {
            this.winRate = (double) wins / sampleSize * 100;
            this.isStatisticallySignificant = sampleSize >= 50 &&
                (winRate > 55 || (winRate > 35 && avgOdds > 2.5) || (winRate > 28 && avgOdds > 3.5));
        }
    }

    /**
     * Get a summary string
     */
    public String getSummary() {
        return String.format("%s: %.1f%% ROI, %.1f%% win rate, %d samples",
            name, roi, winRate, sampleSize);
    }
}
