package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.util.OddsConversionUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class KellyCriterionService {

    private final BankrollService bankrollService;

    // Professional Kelly caps by sport (as fraction of bankroll)
    private static final double NBA_KELLY_CAP = 0.025;     // 2.5%
    private static final double NHL_KELLY_CAP = 0.030;     // 3.0%
    private static final double SOCCER_KELLY_CAP = 0.020;  // 2.0%
    private static final double NFL_KELLY_CAP = 0.030;     // 3.0%
    private static final double MLB_KELLY_CAP = 0.025;     // 2.5%
    private static final double CFB_KELLY_CAP = 0.020;     // 2.0% - higher variance
    private static final double WNBA_KELLY_CAP = 0.020;    // 2.0% - smaller market, less efficient
    private static final double DEFAULT_KELLY_CAP = 0.025;  // 2.5%

    public KellyCriterionService(BankrollService bankrollService) {
        this.bankrollService = bankrollService;
    }

    /**
     * Calculate Kelly Criterion optimal bet size
     * Formula: Kelly % = (bp - q) / b
     * Where:
     * - b = net decimal odds (decimal odds - 1), e.g., +150 = 1.5, -110 = 0.909
     * - p = probability of winning
     * - q = probability of losing (1 - p)
     */
    public Map<String, Object> calculateKelly(int americanOdds, double winProbability, boolean fractional) {
        return calculateKelly(americanOdds, winProbability, fractional, null);
    }

    /**
     * Calculate Kelly Criterion with sport-specific caps.
     * This is the preferred method — always pass sport when available.
     */
    public Map<String, Object> calculateKelly(int americanOdds, double winProbability,
                                               boolean fractional, String sport) {
        Map<String, Object> result = new HashMap<>();

        // Convert American odds to decimal odds
        double decimalOdds = OddsConversionUtils.americanToDecimal(americanOdds);

        // Calculate probabilities
        double p = winProbability;
        double q = 1 - winProbability;

        // Calculate raw Kelly percentage
        // b = net decimal odds (the profit multiplier, not the total return)
        double b = decimalOdds - 1;
        double rawKelly = ((b * p) - q) / b;

        // Ensure non-negative
        if (rawKelly < 0) {
            rawKelly = 0;
        }

        // Apply sport-specific cap BEFORE fractional reduction
        double kellyCap = getKellyCap(sport);
        double cappedKelly = Math.min(rawKelly, kellyCap);

        // Apply fractional Kelly if requested (typically 0.25 to 0.5 for safety)
        double kellyPercentage;
        if (fractional) {
            kellyPercentage = cappedKelly * 0.25; // Quarter Kelly
        } else {
            kellyPercentage = cappedKelly;
        }

        // Half-Kelly conservative recommendation (for display alongside primary)
        double conservativeKelly = cappedKelly / 2.0;

        // Get current bankroll
        BigDecimal currentBankroll = bankrollService.getCurrentBankroll();

        // Calculate recommended stake
        BigDecimal recommendedStake = currentBankroll
            .multiply(BigDecimal.valueOf(kellyPercentage))
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal conservativeStake = currentBankroll
            .multiply(BigDecimal.valueOf(conservativeKelly))
            .setScale(2, RoundingMode.HALF_UP);

        // Calculate expected value
        double ev = (decimalOdds * p) - 1;

        result.put("kellyPercentage", kellyPercentage * 100);
        result.put("rawKellyPercentage", rawKelly * 100);
        result.put("cappedKellyPercentage", cappedKelly * 100);
        result.put("conservativeKellyPercentage", conservativeKelly * 100);
        result.put("recommendedStake", recommendedStake);
        result.put("conservativeStake", conservativeStake);
        result.put("kellyCap", kellyCap * 100);
        result.put("currentBankroll", currentBankroll);
        result.put("expectedValue", ev * 100);
        result.put("decimalOdds", decimalOdds);
        result.put("winProbability", winProbability * 100);
        result.put("fractional", fractional);

        return result;
    }

    /**
     * Calculate Full Kelly (with sport-specific cap)
     */
    public Map<String, Object> calculateFullKelly(int americanOdds, double winProbability) {
        return calculateKelly(americanOdds, winProbability, false, null);
    }

    public Map<String, Object> calculateFullKelly(int americanOdds, double winProbability, String sport) {
        return calculateKelly(americanOdds, winProbability, false, sport);
    }

    /**
     * Calculate Quarter Kelly (safer, recommended for most bettors)
     */
    public Map<String, Object> calculateQuarterKelly(int americanOdds, double winProbability) {
        return calculateKelly(americanOdds, winProbability, true, null);
    }

    public Map<String, Object> calculateQuarterKelly(int americanOdds, double winProbability, String sport) {
        return calculateKelly(americanOdds, winProbability, true, sport);
    }

    /**
     * Get the Kelly cap for a given sport.
     */
    public double getKellyCap(String sport) {
        if (sport == null) return DEFAULT_KELLY_CAP;
        return switch (sport.toUpperCase()) {
            case "NBA", "BASKETBALL" -> NBA_KELLY_CAP;
            case "NHL", "HOCKEY" -> NHL_KELLY_CAP;
            case "NFL", "FOOTBALL" -> NFL_KELLY_CAP;
            case "MLB", "BASEBALL" -> MLB_KELLY_CAP;
            case "CFB", "NCAAF", "CBB", "COLLEGE-FOOTBALL" -> CFB_KELLY_CAP;
            case "WNBA", "WCBB" -> WNBA_KELLY_CAP;
            case "SOCCER", "EPL", "MLS", "LIGA", "SERIE_A", "BUNDESLIGA", "LIGUE_1" -> SOCCER_KELLY_CAP;
            default -> DEFAULT_KELLY_CAP;
        };
    }
    
    /**
     * Calculate implied probability from odds.
     * Delegates to shared utility.
     */
    public double calculateImpliedProbability(int americanOdds) {
        return OddsConversionUtils.oddsToImpliedProbability(americanOdds);
    }
    
    /**
     * Determine if bet has positive expected value
     */
    public boolean hasPositiveEV(int americanOdds, double winProbability) {
        double decimalOdds = OddsConversionUtils.americanToDecimal(americanOdds);
        double ev = (decimalOdds * winProbability) - 1;
        return ev > 0;
    }
}
