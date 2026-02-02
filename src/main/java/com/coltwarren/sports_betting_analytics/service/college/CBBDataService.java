package com.coltwarren.sports_betting_analytics.service.college;

import com.coltwarren.sports_betting_analytics.model.college.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CBBDataService {

    @Value("${cbb.api.key:}")
    private String apiKey;

    @Value("${cbb.api.url:https://api.collegebasketballdata.com}")
    private String baseUrl;

    private final Map<String, CBBTeamStats> statsCache = new ConcurrentHashMap<>();

    // Conference classifications
    private static final List<String> POWER_CONFERENCES = List.of(
        "SEC", "Big Ten", "Big 12", "ACC", "Pac-12", "Big East"
    );

    private static final List<String> SMALL_CONFERENCES = List.of(
        "Horizon", "Big Sky", "MEAC", "SWAC", "Colonial", "Missouri Valley",
        "Summit", "Southland", "Northeast", "Patriot", "Ivy", "Atlantic Sun",
        "Big South", "Big West", "Ohio Valley", "Southern", "WAC", "America East"
    );

    private static final List<String> BLUE_BLOODS = List.of(
        "Kentucky", "Duke", "North Carolina", "Kansas", "UCLA", "Indiana"
    );

    /**
     * Get team stats
     */
    @Cacheable(value = "cbbDataCache", key = "#teamName")
    public CBBTeamStats getTeamStats(String teamName) {
        if (statsCache.containsKey(teamName.toLowerCase())) {
            return statsCache.get(teamName.toLowerCase());
        }

        CBBTeamStats stats = generateRealisticStats(teamName);
        statsCache.put(teamName.toLowerCase(), stats);

        return stats;
    }

    /**
     * Generate realistic CBB stats
     */
    private CBBTeamStats generateRealisticStats(String teamName) {
        CBBTeamStats stats = new CBBTeamStats();
        stats.setTeamName(teamName);

        String conference = determineConference(teamName);
        stats.setConference(conference);

        int tier = getTeamTier(teamName);
        Random rand = new Random(teamName.hashCode());

        // Record based on tier
        int wins = switch (tier) {
            case 1 -> 22 + rand.nextInt(10);  // Elite: 22-31 wins
            case 2 -> 18 + rand.nextInt(8);   // Good: 18-25 wins
            case 3 -> 14 + rand.nextInt(8);   // Mid: 14-21 wins
            default -> 8 + rand.nextInt(10);  // Lower: 8-17 wins
        };
        stats.setWins(wins);
        stats.setLosses(32 - wins);

        // KenPom-style ratings
        double baseEfficiency = switch (tier) {
            case 1 -> 20 + rand.nextDouble() * 10;   // Elite: +20 to +30
            case 2 -> 10 + rand.nextDouble() * 10;   // Good: +10 to +20
            case 3 -> 0 + rand.nextDouble() * 10;    // Mid: 0 to +10
            default -> -10 + rand.nextDouble() * 10; // Lower: -10 to 0
        };

        stats.setAdjEfficiency(baseEfficiency);
        stats.setAdjOffense(105 + baseEfficiency * 0.4 + (rand.nextDouble() - 0.5) * 8);
        stats.setAdjDefense(100 - baseEfficiency * 0.4 + (rand.nextDouble() - 0.5) * 8);
        stats.setAdjTempo(65 + rand.nextDouble() * 10);  // 65-75 possessions

        // Rankings
        if (tier == 1) {
            stats.setKenPomRank(rand.nextInt(30) + 1);
            stats.setApRank(rand.nextInt(25) + 1);
            stats.setNetRank(rand.nextInt(30) + 1);
        } else if (tier == 2) {
            stats.setKenPomRank(30 + rand.nextInt(50));
            if (rand.nextDouble() > 0.6) {
                stats.setApRank(15 + rand.nextInt(15));
            }
            stats.setNetRank(30 + rand.nextInt(50));
        } else if (tier == 3) {
            stats.setKenPomRank(80 + rand.nextInt(100));
            stats.setNetRank(80 + rand.nextInt(100));
        } else {
            stats.setKenPomRank(180 + rand.nextInt(180));
            stats.setNetRank(180 + rand.nextInt(180));
        }

        // Scoring
        stats.setPointsPerGame(stats.getAdjOffense() * stats.getAdjTempo() / 100);
        stats.setPointsAllowedPerGame(stats.getAdjDefense() * stats.getAdjTempo() / 100);

        // Four Factors
        stats.setEffectiveFGPercent(48 + tier * 2 + rand.nextDouble() * 6);
        stats.setTurnoverPercent(18 - tier + rand.nextDouble() * 4);
        stats.setOffReboundPercent(28 + rand.nextDouble() * 8);
        stats.setFreeThrowRate(30 + rand.nextDouble() * 15);

        // ATS records
        int atsWins = (int) (15 + (rand.nextDouble() - 0.5) * 10);
        stats.setAtsWins(atsWins);
        stats.setAtsLosses(32 - atsWins);
        stats.setAtsPercentage((double) atsWins / 32 * 100);

        // Over/Under
        stats.setOverHits(14 + rand.nextInt(6));
        stats.setUnderHits(32 - stats.getOverHits());

        // Splits
        stats.setHomeAtsPercentage(stats.getAtsPercentage() + 5 + rand.nextDouble() * 5);
        stats.setAwayAtsPercentage(stats.getAtsPercentage() - 5 + rand.nextDouble() * 5);
        stats.setEarlySeasonAtsPercentage(45 + rand.nextDouble() * 20); // More variance early

        // Public bias (blue bloods get way more public action)
        if (BLUE_BLOODS.contains(teamName)) {
            stats.setPublicBettingBias(65 + rand.nextDouble() * 15);
        } else if (tier == 1) {
            stats.setPublicBettingBias(55 + rand.nextDouble() * 15);
        } else {
            stats.setPublicBettingBias(40 + rand.nextDouble() * 15);
        }

        // Betting trend
        if (stats.getAtsPercentage() > 55) {
            stats.setBettingTrend("HOT");
        } else if (stats.getAtsPercentage() < 45) {
            stats.setBettingTrend("COLD");
        } else {
            stats.setBettingTrend("NEUTRAL");
        }

        // Key insights
        List<String> insights = new ArrayList<>();
        if (BLUE_BLOODS.contains(teamName)) {
            insights.add("Blue blood program - often overvalued by public in March");
        }
        if (stats.isSmallConference()) {
            insights.add("Small conference - less sportsbook data = more betting value");
        }
        if (stats.getAdjTempo() > 72) {
            insights.add("Fast tempo team - affects totals significantly");
        } else if (stats.getAdjTempo() < 66) {
            insights.add("Slow tempo team - games often go UNDER");
        }
        if (stats.getAtsPercentage() > 55) {
            insights.add("ATS: " + String.format("%.1f%%", stats.getAtsPercentage()) + " - hot against spread");
        }
        stats.setKeyInsights(insights);

        return stats;
    }

    private String determineConference(String teamName) {
        Map<String, String> teamConferences = Map.ofEntries(
            Map.entry("Kentucky", "SEC"), Map.entry("Tennessee", "SEC"), Map.entry("Auburn", "SEC"),
            Map.entry("Alabama", "SEC"), Map.entry("Texas A&M", "SEC"), Map.entry("Florida", "SEC"),
            Map.entry("Arkansas", "SEC"), Map.entry("LSU", "SEC"), Map.entry("Mississippi State", "SEC"),
            Map.entry("Duke", "ACC"), Map.entry("North Carolina", "ACC"), Map.entry("Virginia", "ACC"),
            Map.entry("Wake Forest", "ACC"), Map.entry("Clemson", "ACC"), Map.entry("Miami", "ACC"),
            Map.entry("Kansas", "Big 12"), Map.entry("Baylor", "Big 12"), Map.entry("Houston", "Big 12"),
            Map.entry("Texas Tech", "Big 12"), Map.entry("Iowa State", "Big 12"), Map.entry("TCU", "Big 12"),
            Map.entry("Purdue", "Big Ten"), Map.entry("Michigan State", "Big Ten"), Map.entry("Illinois", "Big Ten"),
            Map.entry("Wisconsin", "Big Ten"), Map.entry("Indiana", "Big Ten"), Map.entry("Iowa", "Big Ten"),
            Map.entry("UCLA", "Big Ten"), Map.entry("Arizona", "Big 12"), Map.entry("Michigan", "Big Ten"),
            Map.entry("UConn", "Big East"), Map.entry("Marquette", "Big East"), Map.entry("Creighton", "Big East"),
            Map.entry("St. John's", "Big East"), Map.entry("Villanova", "Big East"), Map.entry("Xavier", "Big East"),
            Map.entry("Gonzaga", "WCC"), Map.entry("Saint Mary's", "WCC"), Map.entry("San Francisco", "WCC"),
            Map.entry("San Diego State", "Mountain West"), Map.entry("Nevada", "Mountain West"), Map.entry("New Mexico", "Mountain West")
        );

        return teamConferences.getOrDefault(teamName, "Missouri Valley");
    }

    private int getTeamTier(String teamName) {
        List<String> tier1 = List.of(
            "Kentucky", "Duke", "Kansas", "North Carolina", "Gonzaga", "UConn",
            "Purdue", "Houston", "Arizona", "Tennessee", "Auburn", "Alabama"
        );
        if (tier1.contains(teamName)) return 1;

        List<String> tier2 = List.of(
            "Baylor", "UCLA", "Marquette", "Creighton", "Texas", "Michigan State",
            "Illinois", "Virginia", "St. John's", "Florida", "Missouri", "Wisconsin",
            "Iowa State", "Indiana", "Michigan", "Iowa"
        );
        if (tier2.contains(teamName)) return 2;

        List<String> tier3 = List.of(
            "San Diego State", "Nevada", "New Mexico", "Saint Mary's", "Memphis",
            "VCU", "Dayton", "Drake", "Loyola Chicago", "Bradley", "Utah State"
        );
        if (tier3.contains(teamName)) return 3;

        return 4;
    }

    /**
     * Analyze a CBB matchup
     */
    public CBBMatchupAnalysis analyzeMatchup(String homeTeam, String awayTeam,
                                             Double spread, Double total, LocalDate gameDate) {
        CBBMatchupAnalysis analysis = new CBBMatchupAnalysis();
        analysis.setHomeTeam(homeTeam);
        analysis.setAwayTeam(awayTeam);
        analysis.setSpread(spread);
        analysis.setOverUnder(total);
        analysis.setGameDate(gameDate != null ? gameDate : LocalDate.now());

        analysis.setHomeStats(getTeamStats(homeTeam));
        analysis.setAwayStats(getTeamStats(awayTeam));
        analysis.analyze();

        return analysis;
    }

    /**
     * Get teams with edges
     */
    public List<CBBTeamStats> getTeamsWithEdges() {
        return statsCache.values().stream()
            .filter(t -> t.getAtsPercentage() > 55 || t.isSmallConference())
            .sorted(Comparator.comparing(CBBTeamStats::getAtsPercentage).reversed())
            .limit(20)
            .toList();
    }

    public boolean isApiConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public void clearCache() {
        statsCache.clear();
    }
}
