package com.coltwarren.sports_betting_analytics.model.recommendation;

import lombok.Data;

/**
 * Factor Analysis Model
 *
 * Represents the analysis of a single factor (weather, injuries, patterns, etc.)
 * for a specific game. Used to build multi-factor recommendations.
 */
@Data
public class FactorAnalysis {
    private String factorName;          // "Weather", "Public Betting", etc.
    private Boolean isApplicable;       // Does this factor apply to this game?
    private Double rawScore;            // Factor-specific score (0-10)
    private Double weightedScore;       // After applying weight
    private Double weight;              // How much this factor matters (0-1)
    private String recommendation;      // What this factor suggests
    private String reason;              // Human-readable explanation
    private String alertLevel;          // "NONE", "LOW", "MEDIUM", "HIGH", "CRITICAL"

    public FactorAnalysis(String name) {
        this.factorName = name;
        this.isApplicable = false;
        this.rawScore = 0.0;
        this.weightedScore = 0.0;
        this.alertLevel = "NONE";
    }

    /**
     * Check if this factor strongly supports a bet
     */
    public boolean isStrongSignal() {
        return isApplicable && rawScore != null && rawScore >= 7.0;
    }

    /**
     * Get formatted display string
     */
    public String getDisplayString() {
        if (!isApplicable) {
            return factorName + ": Not applicable";
        }
        return String.format("%s: %.1f/10 - %s", factorName, rawScore, reason);
    }
}
