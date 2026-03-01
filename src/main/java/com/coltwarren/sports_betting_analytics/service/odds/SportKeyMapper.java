package com.coltwarren.sports_betting_analytics.service.odds;

/**
 * Shared sport-key and market-key mapping used by both CachedOddsService
 * (database cache reads) and MultiSportOddsService (live API fetches).
 */
public final class SportKeyMapper {

    private SportKeyMapper() {}

    public static String toApiKey(String sport) {
        return switch (sport.toUpperCase()) {
            case "NFL" -> "americanfootball_nfl";
            case "CFB", "COLLEGE-FOOTBALL", "NCAAF" -> "americanfootball_ncaaf";
            case "NBA" -> "basketball_nba";
            case "WNBA" -> "basketball_wnba";
            case "CBB", "COLLEGE-BASKETBALL", "NCAAB" -> "basketball_ncaab";
            case "WCBB", "WOMENS-COLLEGE-BASKETBALL" -> "basketball_wncaab";
            case "MLB" -> "baseball_mlb";
            case "NHL" -> "icehockey_nhl";
            case "MLS" -> "soccer_usa_mls";
            case "EPL" -> "soccer_epl";
            case "UFC" -> "mma_mixed_martial_arts";
            default -> "americanfootball_nfl";
        };
    }

    public static String toMarketKey(String betType) {
        return switch (betType.toUpperCase()) {
            case "MONEYLINE" -> "h2h";
            case "SPREAD" -> "spreads";
            case "TOTAL_OVER", "TOTAL_UNDER" -> "totals";
            default -> "h2h";
        };
    }
}
