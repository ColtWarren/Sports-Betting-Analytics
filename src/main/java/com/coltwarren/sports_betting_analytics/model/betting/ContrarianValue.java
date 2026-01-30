package com.coltwarren.sports_betting_analytics.model.betting;

import lombok.Data;

/**
 * Contrarian Value Model
 *
 * Represents the analysis of a contrarian betting opportunity.
 * When 65%+ of the public is on one side, historical data shows
 * value in betting the other side ("fade the public").
 *
 * Historical Win Rates:
 * - 65%+ public on one side: 52.5% win rate fading
 * - 70%+ public on one side: 54.0% win rate fading
 * - 75%+ public on one side: 56.5% win rate fading
 */
@Data
public class ContrarianValue {
    private Boolean hasContrarianValue;
    private String recommendation;          // "BET_HOME", "BET_AWAY", "BET_OVER", "BET_UNDER"
    private String contrarianSide;          // The side to bet (opposite of public)
    private Integer publicPercent;          // How heavy public is on other side
    private Double historicalEdge;          // Historical win rate of this situation
    private Double confidence;              // 1-10 scale
    private String reason;
    private String alertLevel;              // "NONE", "MILD", "STRONG", "EXTREME"
    
    public ContrarianValue() {
        this.hasContrarianValue = false;
        this.alertLevel = "NONE";
        this.confidence = 0.0;
    }
    
    /**
     * Create a no-value result
     */
    public static ContrarianValue noValue() {
        ContrarianValue value = new ContrarianValue();
        value.setHasContrarianValue(false);
        value.setAlertLevel("NONE");
        value.setReason("No significant public lean detected");
        return value;
    }
    
    /**
     * Check if this is an extreme fade opportunity
     */
    public boolean isExtreme() {
        return "EXTREME".equals(alertLevel);
    }
    
    /**
     * Check if this is at least a strong fade opportunity
     */
    public boolean isStrong() {
        return "STRONG".equals(alertLevel) || "EXTREME".equals(alertLevel);
    }
    
    /**
     * Get formatted display for UI
     */
    public String getDisplayText() {
        if (!Boolean.TRUE.equals(hasContrarianValue)) {
            return "No contrarian value";
        }
        
        String icon = switch (alertLevel) {
            case "EXTREME" -> "🔥";
            case "STRONG" -> "📊";
            case "MILD" -> "📈";
            default -> "";
        };
        
        return String.format("%s Fade to %s (%.1f%% edge)", 
            icon, contrarianSide, historicalEdge);
    }
}
