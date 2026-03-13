package com.coltwarren.sports_betting_analytics.service.injury;

import com.coltwarren.sports_betting_analytics.model.injury.PlayerInjury;
import com.coltwarren.sports_betting_analytics.model.injury.InjuryImpact;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Injury Analysis Service
 *
 * Analyzes injury reports and calculates betting impact.
 * Provides spread adjustments and betting recommendations based on injury severity.
 *
 * Impact model:
 *   impact = positionCeiling * tierMultiplier * statusMultiplier
 *
 * Position ceiling = max spread impact for a star/All-Star at that position.
 * Tier multiplier scales based on player importance (star → bench).
 * Without minutes/usage data from ESPN, players default to ROTATION tier
 * unless they appear in the known-stars set.
 *
 * Target impact ranges:
 *   Star/All-Star (30+ min, high usage): 4-6 pts
 *   Quality starter (25-30 min):         2-3 pts
 *   Rotation player (15-25 min):         0.5-1.5 pts
 *   Deep bench (<15 min):                0-0.5 pts
 */
@Service
@Slf4j
public class InjuryAnalysisService {

    private final ESPNInjuryService espnInjuryService;

    public InjuryAnalysisService(ESPNInjuryService espnInjuryService) {
        this.espnInjuryService = espnInjuryService;
    }

    // ── Player Tier Multipliers ──────────────────────────────────────────
    // Applied to position ceiling to get actual impact.
    // These create the proper separation between stars and bench players.
    private static final double TIER_STAR = 1.0;       // All-Star / franchise player
    private static final double TIER_STARTER = 0.55;    // Quality starter
    private static final double TIER_ROTATION = 0.22;   // Rotation player (15-25 min)
    private static final double TIER_BENCH = 0.08;      // Deep bench (<15 min)
    private static final double TIER_DEFAULT = TIER_ROTATION; // Conservative default

    // ── Known Star Players ───────────────────────────────────────────────
    // Without minutes/usage data from ESPN, we identify stars explicitly.
    // Players not in this set default to ROTATION tier.
    // This should be updated periodically (trades, breakouts, retirements).
    private static final Set<String> NBA_STARS = Set.of(
        // MVP-caliber
        "nikola jokic", "shai gilgeous-alexander", "luka doncic", "giannis antetokounmpo",
        "jayson tatum", "anthony edwards", "joel embiid", "victor wembanyama",
        // All-Star level
        "stephen curry", "lebron james", "kevin durant", "anthony davis",
        "jaylen brown", "damian lillard", "devin booker", "ja morant",
        "donovan mitchell", "trae young", "bam adebayo", "karl-anthony towns",
        "tyrese haliburton", "paolo banchero", "jimmy butler", "pascal siakam",
        "chet holmgren", "lauri markkanen", "de'aaron fox", "domantas sabonis",
        "jalen brunson", "julius randle", "mikal bridges", "deandre ayton",
        "zion williamson", "lamelo ball", "scottie barnes", "evan mobley",
        "franz wagner", "alperen sengun", "desmond bane", "jaren jackson jr."
    );

    // Quality starters — not stars, but clearly above rotation level
    private static final Set<String> NBA_STARTERS = Set.of(
        "michael porter jr.", "andrew wiggins", "khris middleton", "jrue holiday",
        "marcus smart", "al horford", "bobby portis", "brook lopez",
        "derrick white", "jarrett allen", "myles turner", "buddy hield",
        "tyler herro", "coby white", "darius garland", "cam thomas",
        "herb jones", "josh hart", "og anunoby", "immanuel quickley",
        "brandon ingram", "dejounte murray", "fred vanvleet", "cade cunningham",
        "austin reaves", "rui hachimura", "jabari smith jr.", "jalen green",
        "jonathan kuminga", "nic claxton", "cameron johnson", "spencer dinwiddie",
        "mark williams", "tre jones", "keldon johnson", "devin vassell"
    );

    private static final Set<String> NFL_STARS = Set.of(
        "patrick mahomes", "josh allen", "lamar jackson", "joe burrow", "jalen hurts",
        "tua tagovailoa", "justin jefferson", "ja'marr chase", "tyreek hill",
        "ceedee lamb", "davante adams", "travis kelce", "nick bosa",
        "myles garrett", "micah parsons", "t.j. watt", "aaron donald",
        "jalen ramsey", "derwin james", "sauce gardner", "christian mccaffrey",
        "bijan robinson", "breece hall", "saquon barkley"
    );

    private static final Set<String> WNBA_STARS = Set.of(
        "a'ja wilson", "breanna stewart", "caitlin clark", "sabrina ionescu",
        "alyssa thomas", "napheesa collier", "kelsey plum", "jewell loyd",
        "kahleah copper", "chelsea gray", "dearica hamby", "jonquel jones"
    );

    // ── Position Impact Ceilings (star-level max, in spread points) ─────
    // These represent the impact when a STAR at this position is out.
    // Actual impact is scaled down by tier multiplier.

    private static final Map<String, Double> NFL_POSITION_IMPACT = Map.ofEntries(
        Map.entry("QB", 7.0),
        Map.entry("LT", 2.5),
        Map.entry("RT", 2.0),
        Map.entry("WR", 2.5),
        Map.entry("TE", 2.0),
        Map.entry("RB", 2.0),
        Map.entry("DE", 2.0),
        Map.entry("EDGE", 2.0),
        Map.entry("DT", 1.5),
        Map.entry("NT", 1.5),
        Map.entry("LB", 2.0),
        Map.entry("MLB", 2.0),
        Map.entry("OLB", 1.5),
        Map.entry("ILB", 1.5),
        Map.entry("CB", 2.5),
        Map.entry("S", 2.0),
        Map.entry("FS", 2.0),
        Map.entry("SS", 2.0),
        Map.entry("K", 1.0),
        Map.entry("P", 0.5),
        Map.entry("OL", 1.5),
        Map.entry("OG", 1.5),
        Map.entry("C", 1.5)
    );

    // NBA: star PG out = ~5 pts spread impact (e.g., Brunson out for Knicks)
    private static final Map<String, Double> NBA_POSITION_IMPACT = Map.of(
        "PG", 5.0,
        "SG", 4.5,
        "SF", 4.5,
        "PF", 4.0,
        "C", 5.0,
        "G", 4.5,
        "F", 4.0
    );

    private static final Map<String, Double> MLB_POSITION_IMPACT = Map.ofEntries(
        Map.entry("SP", 3.0),
        Map.entry("P", 2.5),
        Map.entry("RP", 1.0),
        Map.entry("CL", 1.5),
        Map.entry("C", 1.5),
        Map.entry("SS", 1.5),
        Map.entry("2B", 1.2),
        Map.entry("3B", 1.3),
        Map.entry("1B", 1.0),
        Map.entry("LF", 1.0),
        Map.entry("CF", 1.2),
        Map.entry("RF", 1.0),
        Map.entry("OF", 1.0),
        Map.entry("DH", 0.8)
    );

    private static final Map<String, Double> NHL_POSITION_IMPACT = Map.of(
        "G", 4.0,
        "C", 2.5,
        "LW", 2.0,
        "RW", 2.0,
        "W", 2.0,
        "D", 2.0
    );

    // WNBA: Higher ceilings because 12-team league = star concentration
    private static final Map<String, Double> WNBA_POSITION_IMPACT = Map.of(
        "PG", 6.0,
        "SG", 5.0,
        "SF", 5.0,
        "PF", 4.5,
        "C", 5.5,
        "G", 5.0,
        "F", 4.5
    );

    /**
     * Analyze injuries for a team and calculate betting impact
     */
    public InjuryImpact analyzeTeamInjuries(String sport, String teamName, String teamId) {
        InjuryImpact impact = new InjuryImpact();
        impact.setTeamId(teamId);
        impact.setTeamName(teamName);

        // Get injuries for the team
        List<PlayerInjury> allInjuries = espnInjuryService.getAllInjuries(sport);
        List<PlayerInjury> teamInjuries = allInjuries.stream()
            .filter(i -> matchesTeam(i, teamName, teamId))
            .collect(Collectors.toList());

        if (teamInjuries.isEmpty()) {
            impact.setSummary("No injuries reported");
            return impact;
        }

        impact.setInjuries(teamInjuries);

        // Calculate total impact
        double totalSpreadImpact = 0.0;
        List<String> significantInjuries = new ArrayList<>();

        for (PlayerInjury injury : teamInjuries) {
            // Only count OUT and DOUBTFUL
            if (!isSignificantStatus(injury.getStatus())) {
                continue;
            }

            double positionCeiling = getPositionImpact(injury.getPosition(), sport);
            double statusMultiplier = getStatusMultiplier(injury.getStatus());
            double tierMultiplier = getPlayerTierMultiplier(injury.getPlayerName(), sport);

            double injuryImpact = positionCeiling * tierMultiplier * statusMultiplier;
            totalSpreadImpact += injuryImpact;

            // Track significant injuries for summary
            if (injuryImpact >= 1.5) {
                significantInjuries.add(String.format("%s %s (%s)",
                    injury.getPosition() != null ? injury.getPosition() : "",
                    injury.getPlayerName() != null ? injury.getPlayerName() : "Unknown",
                    injury.getStatus() != null ? injury.getStatus() : ""));
            }

            log.debug("Injury impact: {} [{}] {} = {:.1f} pts (ceiling={:.1f}, tier={:.2f}, status={:.2f})",
                     injury.getPlayerName(), getTierLabel(tierMultiplier),
                     injury.getStatus(), injuryImpact,
                     positionCeiling, tierMultiplier, statusMultiplier);
        }

        // Cap max injury impact at 12 points per team to prevent runaway values
        if (totalSpreadImpact > 12.0) {
            log.info("Capping {} injury impact from {:.1f} to 12.0 pts", teamName, totalSpreadImpact);
            totalSpreadImpact = 12.0;
        }

        impact.setSpreadImpact(-totalSpreadImpact); // Negative because team is worse
        impact.setTotalImpact(-totalSpreadImpact * 0.7); // Injuries generally reduce scoring

        // Determine severity
        if (totalSpreadImpact >= 7.0) {
            impact.setSeverity("CRITICAL");
            impact.setConfidenceAdjustment(2.0);
            impact.setHasSignificantInjuries(true);
        } else if (totalSpreadImpact >= 4.0) {
            impact.setSeverity("SEVERE");
            impact.setConfidenceAdjustment(1.5);
            impact.setHasSignificantInjuries(true);
        } else if (totalSpreadImpact >= 2.0) {
            impact.setSeverity("MODERATE");
            impact.setConfidenceAdjustment(1.0);
            impact.setHasSignificantInjuries(true);
        } else if (totalSpreadImpact >= 1.0) {
            impact.setSeverity("MINOR");
            impact.setConfidenceAdjustment(0.5);
            impact.setHasSignificantInjuries(false);
        } else {
            impact.setSeverity("NONE");
            impact.setConfidenceAdjustment(0.0);
            impact.setHasSignificantInjuries(false);
        }

        // Build summary
        if (significantInjuries.isEmpty()) {
            impact.setSummary("Minor injuries only - no significant impact");
        } else {
            impact.setSummary(String.join(", ", significantInjuries));
        }

        // Build betting recommendation
        if (totalSpreadImpact >= 3.0) {
            impact.setBettingRecommendation(String.format(
                "FADE %s (%.1f pts of injuries) - Consider opponent or UNDER",
                teamName, totalSpreadImpact
            ));
        } else if (totalSpreadImpact >= 1.5) {
            impact.setBettingRecommendation(String.format(
                "%s has %.1f pts of injury impact - factor into analysis",
                teamName, totalSpreadImpact
            ));
        }

        log.info("Injury analysis for {}: {} severity, {:.1f} spread impact",
                teamName, impact.getSeverity(), totalSpreadImpact);

        return impact;
    }

    /**
     * Analyze injuries for BOTH teams in a matchup
     */
    public Map<String, InjuryImpact> analyzeMatchupInjuries(String sport,
                                                            String homeTeam, String homeTeamId,
                                                            String awayTeam, String awayTeamId) {
        Map<String, InjuryImpact> impacts = new LinkedHashMap<>();

        impacts.put("home", analyzeTeamInjuries(sport, homeTeam, homeTeamId));
        impacts.put("away", analyzeTeamInjuries(sport, awayTeam, awayTeamId));

        return impacts;
    }

    /**
     * Get net injury advantage (positive = home team healthier)
     */
    public Double getNetInjuryAdvantage(Map<String, InjuryImpact> matchupInjuries) {
        InjuryImpact homeImpact = matchupInjuries.get("home");
        InjuryImpact awayImpact = matchupInjuries.get("away");

        double homeSpreadImpact = homeImpact != null && homeImpact.getSpreadImpact() != null ?
            homeImpact.getSpreadImpact() : 0.0;
        double awaySpreadImpact = awayImpact != null && awayImpact.getSpreadImpact() != null ?
            awayImpact.getSpreadImpact() : 0.0;

        // Positive = home team healthier (less injury damage)
        // If home has -3 impact and away has -5 impact: (-3) - (-5) = +2 → home is healthier
        return homeSpreadImpact - awaySpreadImpact;
    }

    /**
     * Get the worst severity between two teams
     */
    public String getWorstSeverity(Map<String, InjuryImpact> matchupInjuries) {
        String homeSeverity = matchupInjuries.get("home").getSeverity();
        String awaySeverity = matchupInjuries.get("away").getSeverity();

        int homeRank = getSeverityRank(homeSeverity);
        int awayRank = getSeverityRank(awaySeverity);

        return homeRank >= awayRank ? homeSeverity : awaySeverity;
    }

    private int getSeverityRank(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 5;
            case "SEVERE" -> 4;
            case "MODERATE" -> 3;
            case "MINOR" -> 2;
            default -> 1;
        };
    }

    private boolean matchesTeam(PlayerInjury injury, String teamName, String teamId) {
        // Prefer exact teamId match when available
        if (teamId != null && injury.getTeamId() != null && injury.getTeamId().equals(teamId)) {
            return true;
        }

        // Guard: reject null or empty team names
        if (teamName == null || teamName.isBlank() ||
            injury.getTeam() == null || injury.getTeam().isBlank()) {
            return false;
        }

        String normalizedTeamName = teamName.toLowerCase().trim();
        String normalizedInjuryTeam = injury.getTeam().toLowerCase().trim();

        // Exact match (case-insensitive)
        if (normalizedInjuryTeam.equals(normalizedTeamName)) {
            return true;
        }

        // One fully contains the other, but ONLY when the shorter string is
        // at least 60% the length of the longer string to prevent loose matches
        // like "Jazz" matching "Trail Blazers Jazz Festival"
        int maxLen = Math.max(normalizedTeamName.length(), normalizedInjuryTeam.length());
        int minLen = Math.min(normalizedTeamName.length(), normalizedInjuryTeam.length());
        boolean lengthRatioOk = maxLen > 0 && (double) minLen / maxLen >= 0.6;

        if (lengthRatioOk && normalizedInjuryTeam.contains(normalizedTeamName)) {
            return true;
        }
        if (lengthRatioOk && normalizedTeamName.contains(normalizedInjuryTeam)) {
            return true;
        }

        // Last-word (nickname) matching: compare the LAST word of each name
        // e.g., "Utah Jazz" -> "Jazz", "Memphis Grizzlies" -> "Grizzlies"
        // This avoids false positives from shared city names ("Los Angeles")
        String teamNickname = getLastWord(normalizedTeamName);
        String injuryNickname = getLastWord(normalizedInjuryTeam);

        if (teamNickname.length() > 3 && teamNickname.equals(injuryNickname)) {
            log.debug("Fuzzy match (nickname): '{}' matched '{}' via nickname '{}'",
                      teamName, injury.getTeam(), teamNickname);
            return true;
        }

        return false;
    }

    private String getLastWord(String name) {
        String[] parts = name.split("\\s+");
        return parts.length > 0 ? parts[parts.length - 1] : name;
    }

    private boolean isSignificantStatus(String status) {
        if (status == null) return false;
        String upper = status.toUpperCase();
        return upper.equals("OUT") || upper.equals("DOUBTFUL") ||
               upper.contains("OUT") || upper.equals("IR") ||
               upper.equals("INJURED RESERVE") || upper.equals("PUP");
    }

    private double getStatusMultiplier(String status) {
        if (status == null) return 0.0;
        String upper = status.toUpperCase();

        if (upper.equals("OUT") || upper.contains("OUT") || upper.equals("IR") ||
            upper.equals("INJURED RESERVE") || upper.equals("PUP")) {
            return 1.0;  // Full impact
        } else if (upper.equals("DOUBTFUL")) {
            return 0.75; // 75% chance out
        } else if (upper.equals("QUESTIONABLE")) {
            return 0.25; // Usually plays
        } else if (upper.equals("PROBABLE") || upper.equals("ACTIVE")) {
            return 0.1;  // Almost always plays
        }
        return 0.0;
    }

    private double getPositionImpact(String position, String sport) {
        if (position == null) return 1.0;
        String upperPos = position.toUpperCase();

        if (sport.equalsIgnoreCase("NFL") || sport.equalsIgnoreCase("CFB")) {
            return NFL_POSITION_IMPACT.getOrDefault(upperPos, 1.0);
        } else if (sport.equalsIgnoreCase("WNBA")) {
            return WNBA_POSITION_IMPACT.getOrDefault(upperPos, 4.5);
        } else if (sport.equalsIgnoreCase("NBA") || sport.equalsIgnoreCase("CBB") ||
                   sport.equalsIgnoreCase("WCBB")) {
            return NBA_POSITION_IMPACT.getOrDefault(upperPos, 4.0);
        } else if (sport.equalsIgnoreCase("MLB")) {
            return MLB_POSITION_IMPACT.getOrDefault(upperPos, 1.0);
        } else if (sport.equalsIgnoreCase("NHL")) {
            return NHL_POSITION_IMPACT.getOrDefault(upperPos, 2.0);
        }

        return 1.5;
    }

    /**
     * Determine a player's tier multiplier based on known-player lookups.
     *
     * Without minutes/usage data from ESPN, we can't programmatically determine
     * player importance. Instead we use curated sets of known stars and starters.
     * Unknown players default to ROTATION tier (conservative — avoids inflating
     * impact for bench players like Ziaire Williams, Day'Ron Sharpe, Egor Demin).
     */
    private double getPlayerTierMultiplier(String playerName, String sport) {
        if (playerName == null) return TIER_DEFAULT;
        String normalized = playerName.toLowerCase().trim();

        if (sport == null) return TIER_DEFAULT;
        String upperSport = sport.toUpperCase();

        switch (upperSport) {
            case "NBA", "BASKETBALL" -> {
                if (NBA_STARS.contains(normalized)) return TIER_STAR;
                if (NBA_STARTERS.contains(normalized)) return TIER_STARTER;
                return TIER_DEFAULT;
            }
            case "NFL", "FOOTBALL" -> {
                if (NFL_STARS.contains(normalized)) return TIER_STAR;
                return TIER_DEFAULT;
            }
            case "WNBA" -> {
                if (WNBA_STARS.contains(normalized)) return TIER_STAR;
                return TIER_DEFAULT;
            }
            default -> {
                return TIER_DEFAULT;
            }
        }
    }

    private String getTierLabel(double tierMultiplier) {
        if (tierMultiplier >= TIER_STAR) return "STAR";
        if (tierMultiplier >= TIER_STARTER) return "STARTER";
        if (tierMultiplier >= TIER_ROTATION) return "ROTATION";
        return "BENCH";
    }
}
