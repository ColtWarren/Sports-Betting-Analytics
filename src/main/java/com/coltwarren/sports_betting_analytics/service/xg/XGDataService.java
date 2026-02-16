package com.coltwarren.sports_betting_analytics.service.xg;

import com.coltwarren.sports_betting_analytics.model.xg.TeamXGStats;
import com.coltwarren.sports_betting_analytics.model.xg.MatchXGAnalysis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * xG Data Service
 *
 * Provides Expected Goals (xG) data for soccer teams.
 * Uses simulated realistic data for immediate testing,
 * structured for real API/scraping integration later.
 */
@Service
@Slf4j
public class XGDataService {

    // Cache for team xG stats
    private final Map<String, TeamXGStats> xgCache = new ConcurrentHashMap<>();

    // League mappings
    private static final Map<String, String> LEAGUE_NAMES = Map.of(
        "EPL", "English Premier League",
        "LA_LIGA", "La Liga",
        "BUNDESLIGA", "Bundesliga",
        "SERIE_A", "Serie A",
        "LIGUE_1", "Ligue 1"
    );

    /**
     * Get xG stats for a team
     */
    public TeamXGStats getTeamXGStats(String teamName, String league) {
        String cacheKey = teamName.toLowerCase() + "_" + league;

        if (xgCache.containsKey(cacheKey)) {
            return xgCache.get(cacheKey);
        }

        // Generate realistic xG data based on team reputation
        TeamXGStats stats = generateTeamXGStats(teamName, league);
        xgCache.put(cacheKey, stats);

        return stats;
    }

    /**
     * Analyze a match using xG data
     */
    public MatchXGAnalysis analyzeMatch(String homeTeam, String awayTeam, String league) {
        MatchXGAnalysis analysis = new MatchXGAnalysis();
        analysis.setHomeTeam(homeTeam);
        analysis.setAwayTeam(awayTeam);
        analysis.setLeague(league);

        analysis.setHomeXGStats(getTeamXGStats(homeTeam, league));
        analysis.setAwayXGStats(getTeamXGStats(awayTeam, league));

        analysis.analyze();

        log.info("xG Analysis for {} vs {}: {} (confidence: {})",
            homeTeam, awayTeam, analysis.getXgRecommendation(), analysis.getXgConfidence());

        return analysis;
    }

    /**
     * Generate realistic xG stats based on team reputation
     * In production, this would fetch from Understat API or database
     */
    private TeamXGStats generateTeamXGStats(String teamName, String league) {
        TeamXGStats stats = new TeamXGStats();
        stats.setTeamName(teamName);
        stats.setLeague(league);
        stats.setSeason("2024-25");
        stats.setLastUpdated(LocalDate.now());

        // Determine team tier based on name (simplified reputation system)
        int tier = getTeamTier(teamName);

        // Generate stats based on tier
        Random rand = new Random(teamName.hashCode()); // Consistent per team

        stats.setMatchesPlayed(20 + rand.nextInt(5));

        // Base xG values by tier
        double baseXG = switch (tier) {
            case 1 -> 1.8 + rand.nextDouble() * 0.4;  // Elite: 1.8-2.2 xG/game
            case 2 -> 1.4 + rand.nextDouble() * 0.4;  // Good: 1.4-1.8
            case 3 -> 1.1 + rand.nextDouble() * 0.3;  // Mid: 1.1-1.4
            default -> 0.8 + rand.nextDouble() * 0.3; // Lower: 0.8-1.1
        };

        double baseXGA = switch (tier) {
            case 1 -> 0.8 + rand.nextDouble() * 0.3;  // Elite concede less
            case 2 -> 1.1 + rand.nextDouble() * 0.3;
            case 3 -> 1.3 + rand.nextDouble() * 0.3;
            default -> 1.6 + rand.nextDouble() * 0.4; // Lower: 1.6-2.0
        };

        stats.setXGFor(baseXG * stats.getMatchesPlayed());
        stats.setXGAgainst(baseXGA * stats.getMatchesPlayed());

        // Add variance for goals vs xG (the luck factor!)
        // Some teams overperform, some underperform
        double luckFactor = (rand.nextDouble() - 0.5) * 0.4; // -0.2 to +0.2 per game
        double totalLuck = luckFactor * stats.getMatchesPlayed();

        stats.setGoalsScored((int) Math.round(stats.getXGFor() + totalLuck));
        stats.setGoalsConceded((int) Math.round(stats.getXGAgainst() - totalLuck * 0.5));

        // Recent form (last 5)
        stats.setRecentXG(baseXG * 5 * (0.9 + rand.nextDouble() * 0.2));
        stats.setRecentGoals((double) (int) (stats.getRecentXG() + (rand.nextDouble() - 0.5) * 2));

        stats.calculateDifferentials();

        return stats;
    }

    /**
     * Determine team tier (1=elite, 4=lower)
     */
    private int getTeamTier(String teamName) {
        String name = teamName.toLowerCase();

        // Tier 1: Elite clubs
        if (name.contains("manchester city") || name.contains("man city") ||
            name.contains("liverpool") || name.contains("arsenal") ||
            name.contains("real madrid") || name.contains("barcelona") ||
            name.contains("bayern") || name.contains("psg") ||
            name.contains("inter") || name.contains("juventus")) {
            return 1;
        }

        // Tier 2: Strong clubs
        if (name.contains("chelsea") || name.contains("tottenham") ||
            name.contains("manchester united") || name.contains("man united") ||
            name.contains("atletico") || name.contains("dortmund") ||
            name.contains("napoli") || name.contains("milan") ||
            name.contains("newcastle") || name.contains("aston villa")) {
            return 2;
        }

        // Tier 2: Strong clubs (continued - Ligue 1, MLS)
        if (name.contains("monaco") || name.contains("marseille") ||
            name.contains("atlanta") || name.contains("atlanta united") ||
            name.contains("cincinnati") || name.contains("fc cincinnati")) {
            return 2;
        }

        // Tier 3: Mid-table
        if (name.contains("west ham") || name.contains("brighton") ||
            name.contains("wolves") || name.contains("crystal palace") ||
            name.contains("fulham") || name.contains("bournemouth") ||
            name.contains("sevilla") || name.contains("villarreal") ||
            name.contains("roma") || name.contains("lazio") ||
            name.contains("nice") || name.contains("lens") ||
            name.contains("rennes") || name.contains("lyon") ||
            name.contains("st. louis") || name.contains("st louis") ||
            name.contains("charlotte")) {
            return 3;
        }

        // Tier 4: Lower table / promoted teams
        return 4;
    }

    /**
     * Get all teams with significant regression signals
     */
    public List<TeamXGStats> getTeamsWithRegressionSignals(String league) {
        // In production, this would query the database
        // For now, return cached teams with signals
        return xgCache.values().stream()
            .filter(t -> league == null || league.equals(t.getLeague()))
            .filter(t -> !"NEUTRAL".equals(t.getBettingSignal()))
            .sorted(Comparator.comparing(TeamXGStats::getRegressionLikelihood).reversed())
            .toList();
    }

    /**
     * Clear cache (for refreshing data)
     */
    public void clearCache() {
        xgCache.clear();
        log.info("xG cache cleared");
    }

    /**
     * Get cache stats
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
            "teamsInCache", xgCache.size(),
            "leagues", xgCache.values().stream()
                .map(TeamXGStats::getLeague)
                .distinct()
                .toList()
        );
    }
}
