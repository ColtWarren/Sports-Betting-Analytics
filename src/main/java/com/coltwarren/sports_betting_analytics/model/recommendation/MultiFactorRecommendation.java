package com.coltwarren.sports_betting_analytics.model.recommendation;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Multi-Factor Recommendation Model
 *
 * The complete recommendation for a single game, combining all factors
 * into a unified betting recommendation with confidence scoring.
 */
@Data
public class MultiFactorRecommendation {
    // Game info
    private String gameId;
    private String sport;
    private String league;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime gameTime;
    private String venue;

    // Current odds
    private Double homeOdds;
    private Double drawOdds;
    private Double awayOdds;
    private Double overOdds;
    private Double underOdds;
    private String bestSportsbook;

    // Individual factor analyses
    private FactorAnalysis weatherFactor;
    private FactorAnalysis publicBettingFactor;
    private FactorAnalysis injuryFactor;
    private FactorAnalysis patternFactor;
    private FactorAnalysis valueFactor;  // Pure odds value
    private FactorAnalysis xgFactor;     // Expected Goals (soccer only)

    // Combined analysis
    private Double combinedScore;        // 0-100 scale
    private Double confidence;           // 1-10 scale
    private Integer factorsAligned;      // How many factors agree
    private Integer factorsTotal;        // Total applicable factors

    // Final recommendation
    private String primaryBet;           // "HOME", "AWAY", "DRAW", "OVER", "UNDER", "NO_BET"
    private String primaryBetDisplay;    // "Chiefs -7" or "Over 45.5"
    private Double recommendedOdds;
    private Double expectedValue;        // EV percentage
    private Double kellyStake;           // Recommended stake %
    private Double suggestedAmount;      // Dollar amount (based on bankroll)

    // Risk assessment
    private String riskLevel;            // "LOW", "MEDIUM", "HIGH"
    private Double maxDrawdownRisk;
    private List<String> riskFactors;

    // Summary
    private String headline;             // "🔥 STRONG BET: Chiefs -7"
    private String summary;              // Multi-line explanation
    private List<String> keyInsights;    // Bullet points

    // Metadata
    private LocalDateTime generatedAt;
    private String modelVersion;

    public MultiFactorRecommendation() {
        this.riskFactors = new ArrayList<>();
        this.keyInsights = new ArrayList<>();
        this.generatedAt = LocalDateTime.now();
        this.modelVersion = "v2.6.0";
    }

    /**
     * Check if this is a recommended bet
     */
    public boolean isBetRecommended() {
        return !"NO_BET".equals(primaryBet) && confidence != null && confidence >= 6.0;
    }

    /**
     * Get all applicable factors
     */
    public List<FactorAnalysis> getApplicableFactors() {
        List<FactorAnalysis> factors = new ArrayList<>();
        if (weatherFactor != null && weatherFactor.getIsApplicable()) factors.add(weatherFactor);
        if (publicBettingFactor != null && publicBettingFactor.getIsApplicable()) factors.add(publicBettingFactor);
        if (injuryFactor != null && injuryFactor.getIsApplicable()) factors.add(injuryFactor);
        if (patternFactor != null && patternFactor.getIsApplicable()) factors.add(patternFactor);
        if (valueFactor != null && valueFactor.getIsApplicable()) factors.add(valueFactor);
        if (xgFactor != null && xgFactor.getIsApplicable()) factors.add(xgFactor);
        return factors;
    }
}
