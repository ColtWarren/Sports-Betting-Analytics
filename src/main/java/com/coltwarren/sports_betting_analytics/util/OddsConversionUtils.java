package com.coltwarren.sports_betting_analytics.util;

/**
 * Utility methods for converting between odds formats.
 * All odds are in American format (e.g., +150, -110).
 */
public final class OddsConversionUtils {

    private OddsConversionUtils() {}

    /**
     * Convert American odds to decimal odds.
     * +150 → 2.5, -110 → 1.909
     */
    public static double americanToDecimal(int americanOdds) {
        if (americanOdds > 0) {
            return (americanOdds / 100.0) + 1;
        } else {
            return (100.0 / Math.abs(americanOdds)) + 1;
        }
    }

    /**
     * Convert American odds to implied probability (0.0 to 1.0).
     * -110 → 0.524, +150 → 0.400
     */
    public static double oddsToImpliedProbability(int americanOdds) {
        if (americanOdds > 0) {
            return 100.0 / (americanOdds + 100);
        } else {
            return Math.abs(americanOdds) / (Math.abs(americanOdds) + 100.0);
        }
    }
}
