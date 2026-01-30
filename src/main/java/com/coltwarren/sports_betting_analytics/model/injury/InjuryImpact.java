package com.coltwarren.sports_betting_analytics.model.injury;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

/**
 * Injury Impact Model
 *
 * Represents the calculated betting impact of injuries for a team.
 * Used to adjust spread expectations and betting recommendations.
 */
@Data
public class InjuryImpact {
    private String teamId;
    private String teamName;
    private List<PlayerInjury> injuries;

    // Calculated impacts
    private Double spreadImpact;        // e.g., -7.0 means team is 7 points worse
    private Double totalImpact;         // Impact on over/under
    private Double confidenceAdjustment; // How much to adjust our confidence

    private String severity;            // "NONE", "MINOR", "MODERATE", "SEVERE", "CRITICAL"
    private String summary;             // "QB Patrick Mahomes OUT, WR Rashee Rice QUESTIONABLE"
    private String bettingRecommendation; // "FADE Chiefs -7" or "Consider UNDER"

    private Boolean hasSignificantInjuries;

    public InjuryImpact() {
        this.injuries = new ArrayList<>();
        this.spreadImpact = 0.0;
        this.totalImpact = 0.0;
        this.confidenceAdjustment = 0.0;
        this.severity = "NONE";
        this.hasSignificantInjuries = false;
    }

    /**
     * Check if this is a critical injury situation
     */
    public boolean isCritical() {
        return "CRITICAL".equals(severity);
    }

    /**
     * Check if this is at least severe
     */
    public boolean isSevere() {
        return "SEVERE".equals(severity) || "CRITICAL".equals(severity);
    }

    /**
     * Get the count of OUT players
     */
    public int getOutCount() {
        if (injuries == null) return 0;
        return (int) injuries.stream()
            .filter(i -> i.getStatus() != null &&
                        (i.getStatus().equalsIgnoreCase("OUT") ||
                         i.getStatus().toUpperCase().contains("OUT")))
            .count();
    }

    /**
     * Get formatted impact for display
     */
    public String getFormattedImpact() {
        if (spreadImpact == null || spreadImpact == 0.0) {
            return "No impact";
        }
        return String.format("%.1f pts", spreadImpact);
    }
}
