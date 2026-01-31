package com.coltwarren.sports_betting_analytics.model.pattern;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Pattern Match Model
 *
 * Represents a match between a discovered pattern and an upcoming game.
 * Used to flag betting opportunities based on historical patterns.
 */
@Data
public class PatternMatch {
    private String gameId;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime gameTime;
    private String sport;
    private String league;

    // The pattern that matched
    private BettingPattern pattern;

    // Bet details
    private String recommendedBet;       // "HOME", "AWAY", "DRAW", etc.
    private Double currentOdds;
    private Double expectedValue;        // EV as percentage
    private Double suggestedKelly;       // Suggested Kelly stake

    // Confidence
    private Double matchConfidence;      // How well does this game fit the pattern
    private String matchReason;          // Why it matched

    // Combined with other factors
    private Boolean weatherSupports;
    private Boolean publicBettingSupports;
    private Boolean injurySupports;
    private Double combinedConfidence;   // All factors together

    /**
     * Get formatted recommendation
     */
    public String getRecommendation() {
        return String.format("%s vs %s: Bet %s @ %.2f (%.1f%% EV, %.1f%% Kelly)",
            homeTeam, awayTeam, recommendedBet, currentOdds, expectedValue, suggestedKelly * 100);
    }

    /**
     * Check if this is a strong match
     */
    public boolean isStrongMatch() {
        return combinedConfidence != null && combinedConfidence >= 7.0;
    }
}
