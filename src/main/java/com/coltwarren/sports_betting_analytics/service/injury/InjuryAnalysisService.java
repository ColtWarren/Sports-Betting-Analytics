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
 */
@Service
@Slf4j
public class InjuryAnalysisService {

    private final ESPNInjuryService espnInjuryService;

    public InjuryAnalysisService(ESPNInjuryService espnInjuryService) {
        this.espnInjuryService = espnInjuryService;
    }

    // Impact values by position (in points for spread) - NFL
    private static final Map<String, Double> NFL_POSITION_IMPACT = Map.ofEntries(
        Map.entry("QB", 7.0),      // Starting QB out is HUGE
        Map.entry("LT", 2.5),      // Left tackle protects QB's blind side
        Map.entry("RT", 2.0),
        Map.entry("WR", 2.0),
        Map.entry("TE", 1.5),
        Map.entry("RB", 1.5),
        Map.entry("DE", 1.5),
        Map.entry("EDGE", 1.5),
        Map.entry("DT", 1.0),
        Map.entry("NT", 1.0),
        Map.entry("LB", 1.5),
        Map.entry("MLB", 1.5),
        Map.entry("OLB", 1.3),
        Map.entry("ILB", 1.3),
        Map.entry("CB", 2.0),      // Top corners are valuable
        Map.entry("S", 1.5),
        Map.entry("FS", 1.5),
        Map.entry("SS", 1.5),
        Map.entry("K", 1.0),
        Map.entry("P", 0.5),
        Map.entry("OL", 1.5),
        Map.entry("OG", 1.5),
        Map.entry("C", 1.5)
    );

    private static final Map<String, Double> NBA_POSITION_IMPACT = Map.of(
        "PG", 4.0,
        "SG", 3.5,
        "SF", 3.5,
        "PF", 3.0,
        "C", 3.5,
        "G", 3.5,
        "F", 3.0
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

    // WNBA: Higher impact than NBA because 12-team league means star players are more critical
    private static final Map<String, Double> WNBA_POSITION_IMPACT = Map.of(
        "PG", 5.0,   // Star PGs (Clark, Ionescu) have outsized impact in smaller league
        "SG", 4.0,
        "SF", 4.0,
        "PF", 3.5,
        "C", 4.5,    // Elite centers (Wilson, Stewart) dominate both ends
        "G", 4.0,
        "F", 3.5
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

            double positionImpact = getPositionImpact(injury.getPosition(), sport);
            double statusMultiplier = getStatusMultiplier(injury.getStatus());
            double playerMultiplier = injury.getPlayerRating() != null ?
                injury.getPlayerRating() / 80.0 : 1.0; // Boost for star players

            double injuryImpact = positionImpact * statusMultiplier * playerMultiplier;
            totalSpreadImpact += injuryImpact;

            // Track significant injuries for summary
            if (injuryImpact >= 1.5) {
                significantInjuries.add(String.format("%s %s (%s)",
                    injury.getPosition() != null ? injury.getPosition() : "",
                    injury.getPlayerName() != null ? injury.getPlayerName() : "Unknown",
                    injury.getStatus() != null ? injury.getStatus() : ""));
            }

            log.debug("Injury impact: {} {} = {:.1f} points",
                     injury.getPlayerName(), injury.getStatus(), injuryImpact);
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

        // If home has -3 impact and away has -5 impact, home has +2 advantage
        return awaySpreadImpact - homeSpreadImpact;
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

        // One fully contains the other, but only if the shorter string is
        // substantial enough (>5 chars) to avoid false positives like contains("")
        if (normalizedTeamName.length() > 5 && normalizedInjuryTeam.contains(normalizedTeamName)) {
            return true;
        }
        if (normalizedInjuryTeam.length() > 5 && normalizedTeamName.contains(normalizedInjuryTeam)) {
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
            return WNBA_POSITION_IMPACT.getOrDefault(upperPos, 4.0);
        } else if (sport.equalsIgnoreCase("NBA") || sport.equalsIgnoreCase("CBB") ||
                   sport.equalsIgnoreCase("WCBB")) {
            return NBA_POSITION_IMPACT.getOrDefault(upperPos, 3.0);
        } else if (sport.equalsIgnoreCase("MLB")) {
            return MLB_POSITION_IMPACT.getOrDefault(upperPos, 1.0);
        } else if (sport.equalsIgnoreCase("NHL")) {
            return NHL_POSITION_IMPACT.getOrDefault(upperPos, 2.0);
        }

        return 1.5; // Default for other sports
    }
}
