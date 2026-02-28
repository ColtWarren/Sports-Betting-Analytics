package com.coltwarren.sports_betting_analytics.util;

/**
 * Utility methods for team name extraction and normalization.
 */
public final class TeamNameUtils {

    private TeamNameUtils() {}

    /**
     * Extract the team name (last word) from a full display name.
     * "Buffalo Bills" → "Bills", "Kansas City Chiefs" → "Chiefs"
     *
     * @param displayName Full team display name
     * @return The team name portion (last word)
     */
    public static String extractTeamName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "";
        }
        String[] parts = displayName.split(" ");
        return parts[parts.length - 1];
    }
}
