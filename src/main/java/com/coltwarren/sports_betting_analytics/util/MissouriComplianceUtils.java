package com.coltwarren.sports_betting_analytics.util;

import java.util.Set;

/**
 * Missouri legal sportsbook compliance filter.
 * Ensures only Missouri-licensed sportsbooks are displayed to users.
 */
public final class MissouriComplianceUtils {

    private MissouriComplianceUtils() {}

    /**
     * Missouri-licensed sportsbooks (as of 2024).
     * - DraftKings, FanDuel, BetMGM, Caesars Sportsbook
     * - bet365, Fanatics Sportsbook, Circa Sports, theScore Bet
     */
    public static final Set<String> MISSOURI_LEGAL_BOOKS = Set.of(
        "draftkings",
        "fanduel",
        "betmgm",
        "mgm",
        "caesars",
        "bet365",
        "fanatics",
        "circa",
        "thescore",
        "score"
    );

    /**
     * Check if a bookmaker is licensed and legal in Missouri.
     *
     * Normalization:
     * 1. Convert to lowercase
     * 2. Remove common suffixes ("sportsbook", "ag", "the")
     * 3. Remove spaces and special characters
     * 4. Match against known legal books
     *
     * @param bookmakerName The bookmaker name from The Odds API
     * @return true if the bookmaker is Missouri-licensed
     */
    public static boolean isMissouriLegal(String bookmakerName) {
        if (bookmakerName == null || bookmakerName.isEmpty()) {
            return false;
        }

        String normalized = bookmakerName.toLowerCase()
            .replace("sportsbook", "")
            .replace(" ", "")
            .replace(".", "")
            .replace("ag", "")
            .replace("the", "")
            .trim();

        return MISSOURI_LEGAL_BOOKS.stream()
            .anyMatch(legalBook -> normalized.contains(legalBook) || legalBook.contains(normalized));
    }
}
