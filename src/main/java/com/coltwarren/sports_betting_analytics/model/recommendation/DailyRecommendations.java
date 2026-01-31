package com.coltwarren.sports_betting_analytics.model.recommendation;

import lombok.Data;
import java.time.LocalDate;
import java.util.*;

/**
 * Daily Recommendations Model
 *
 * Aggregates all recommendations for a day, grouped by confidence level.
 * Provides summary statistics and top picks.
 */
@Data
public class DailyRecommendations {
    private LocalDate date;
    private Integer totalGamesAnalyzed;
    private Integer recommendedBets;
    private Double totalExpectedValue;
    private Double totalKellyExposure;

    // Grouped by confidence
    private List<MultiFactorRecommendation> highConfidence;   // 8-10
    private List<MultiFactorRecommendation> mediumConfidence; // 6-7.9
    private List<MultiFactorRecommendation> lowConfidence;    // Below 6
    private List<MultiFactorRecommendation> noBets;           // Not recommended

    // Top picks
    private MultiFactorRecommendation topPick;
    private List<MultiFactorRecommendation> topFive;

    // Summary stats
    private Map<String, Integer> betsBySport;
    private Map<String, Double> evBySport;

    public DailyRecommendations() {
        this.date = LocalDate.now();
        this.highConfidence = new ArrayList<>();
        this.mediumConfidence = new ArrayList<>();
        this.lowConfidence = new ArrayList<>();
        this.noBets = new ArrayList<>();
        this.topFive = new ArrayList<>();
        this.betsBySport = new HashMap<>();
        this.evBySport = new HashMap<>();
    }

    /**
     * Get total bets by confidence tier
     */
    public Map<String, Integer> getBetsByTier() {
        Map<String, Integer> tiers = new LinkedHashMap<>();
        tiers.put("high", highConfidence.size());
        tiers.put("medium", mediumConfidence.size());
        tiers.put("low", lowConfidence.size());
        tiers.put("pass", noBets.size());
        return tiers;
    }

    /**
     * Get all recommended bets (high + medium confidence)
     */
    public List<MultiFactorRecommendation> getAllRecommended() {
        List<MultiFactorRecommendation> all = new ArrayList<>();
        all.addAll(highConfidence);
        all.addAll(mediumConfidence);
        return all;
    }
}
