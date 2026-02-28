package com.coltwarren.sports_betting_analytics.service.analyzer;

import java.time.LocalDateTime;

/**
 * Shared utility methods for bet analysis parsing.
 */
public final class BetAnalysisUtils {

    private BetAnalysisUtils() {}

    /**
     * Extract confidence score from AI analysis text.
     * Looks for "CONFIDENCE: X" pattern.
     */
    public static double extractConfidence(String analysis) {
        try {
            String lower = analysis.toLowerCase();

            if (lower.contains("confidence:")) {
                int start = lower.indexOf("confidence:") + 11;
                String sub = analysis.substring(start).trim();

                String[] parts = sub.split("[^0-9.]");
                if (parts.length > 0 && !parts[0].isEmpty()) {
                    return Double.parseDouble(parts[0]);
                }
            }

            return 5.0; // Below threshold - parse failures get filtered out

        } catch (Exception e) {
            return 5.0;
        }
    }

    /**
     * Extract recommendation text from AI analysis.
     * Looks for "RECOMMENDATION: ..." pattern.
     */
    public static String extractRecommendation(String analysis) {
        try {
            if (analysis.toLowerCase().contains("recommendation:")) {
                int start = analysis.toLowerCase().indexOf("recommendation:") + 15;
                String sub = analysis.substring(start);

                int end = sub.indexOf("|");
                if (end > 0) {
                    return sub.substring(0, end).trim();
                } else {
                    return sub.split("\n")[0].trim();
                }
            }

            return "See analysis";

        } catch (Exception e) {
            return "See analysis";
        }
    }

    /**
     * Parse game time from various formats to LocalDateTime.
     */
    public static LocalDateTime parseGameTime(Object gameTimeObj) {
        if (gameTimeObj == null) return null;

        try {
            if (gameTimeObj instanceof LocalDateTime) {
                return (LocalDateTime) gameTimeObj;
            }

            String gameTimeStr = gameTimeObj.toString();

            if (gameTimeStr.contains("T")) {
                return LocalDateTime.parse(gameTimeStr.split("\\.")[0]);
            }

            return LocalDateTime.now().plusHours(3);

        } catch (Exception e) {
            return LocalDateTime.now().plusHours(3);
        }
    }
}
