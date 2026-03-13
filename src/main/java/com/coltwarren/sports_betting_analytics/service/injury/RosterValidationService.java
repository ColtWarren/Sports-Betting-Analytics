package com.coltwarren.sports_betting_analytics.service.injury;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Roster Validation Service
 *
 * Validates that a player actually belongs to the team they're listed under
 * in ESPN injury reports. Uses BallDontLie API for NBA lookups, with a static
 * fallback map for known mismatches and when the API is unavailable.
 */
@Service
@Slf4j
public class RosterValidationService {

    @Value("${balldontlie.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // BallDontLie NBA base URL (different from WNBA)
    private static final String BDL_NBA_BASE_URL = "https://api.balldontlie.io/v1";

    // Cache: playerName (lowercase) -> teamFullName (from API)
    // TTL managed by cacheTimestamp
    private final Map<String, String> playerTeamCache = new ConcurrentHashMap<>();
    private volatile long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour

    // Static fallback map for known player-team associations.
    // Used when BallDontLie API is unavailable or for non-NBA sports.
    // Update this map periodically or after major trades.
    private static final Map<String, String> KNOWN_PLAYER_TEAMS = Map.ofEntries(
        // NBA - Known mismatch corrections (2025-26 season)
        Map.entry("michael porter jr.", "Denver Nuggets"),
        Map.entry("jaren jackson jr.", "Memphis Grizzlies"),
        Map.entry("al horford", "Boston Celtics"),
        Map.entry("nikola jokic", "Denver Nuggets"),
        Map.entry("luka doncic", "Dallas Mavericks"),
        Map.entry("stephen curry", "Golden State Warriors"),
        Map.entry("lebron james", "Los Angeles Lakers"),
        Map.entry("giannis antetokounmpo", "Milwaukee Bucks"),
        Map.entry("kevin durant", "Phoenix Suns"),
        Map.entry("jayson tatum", "Boston Celtics"),
        Map.entry("joel embiid", "Philadelphia 76ers"),
        Map.entry("anthony davis", "Los Angeles Lakers"),
        Map.entry("damian lillard", "Milwaukee Bucks"),
        Map.entry("jimmy butler", "Phoenix Suns"),
        Map.entry("devin booker", "Phoenix Suns"),
        Map.entry("ja morant", "Memphis Grizzlies"),
        Map.entry("shai gilgeous-alexander", "Oklahoma City Thunder"),
        Map.entry("anthony edwards", "Minnesota Timberwolves"),
        Map.entry("donovan mitchell", "Cleveland Cavaliers"),
        Map.entry("trae young", "Atlanta Hawks"),
        Map.entry("zion williamson", "New Orleans Pelicans"),
        Map.entry("karl-anthony towns", "New York Knicks"),
        Map.entry("bam adebayo", "Miami Heat"),
        Map.entry("jaylen brown", "Boston Celtics"),
        Map.entry("pascal siakam", "Indiana Pacers"),
        Map.entry("tyrese haliburton", "Indiana Pacers"),
        Map.entry("paolo banchero", "Orlando Magic"),
        Map.entry("victor wembanyama", "San Antonio Spurs"),
        Map.entry("chet holmgren", "Oklahoma City Thunder"),
        Map.entry("lauri markkanen", "Utah Jazz")
    );

    /**
     * Validate whether a player belongs to the given team.
     *
     * @return true if the player is confirmed on the team (or cannot be verified),
     *         false if the player is confirmed on a DIFFERENT team
     */
    public boolean isPlayerOnTeam(String playerName, String teamName, String sport) {
        if (playerName == null || teamName == null) {
            return true; // Can't validate, allow through
        }

        String normalizedPlayer = playerName.toLowerCase().trim();
        String normalizedTeam = teamName.toLowerCase().trim();

        // 1. Check static fallback map first (fastest, always available)
        String knownTeam = KNOWN_PLAYER_TEAMS.get(normalizedPlayer);
        if (knownTeam != null) {
            boolean matches = teamNameMatches(knownTeam.toLowerCase(), normalizedTeam);
            if (!matches) {
                log.warn("ROSTER MISMATCH (static map): {} is on {}, not {}. Skipping injury.",
                    playerName, knownTeam, teamName);
            }
            return matches;
        }

        // 2. For NBA, try BallDontLie API lookup
        if (isNbaSport(sport) && isApiConfigured()) {
            String apiTeam = lookupPlayerTeam(playerName);
            if (apiTeam != null) {
                boolean matches = teamNameMatches(apiTeam.toLowerCase(), normalizedTeam);
                if (!matches) {
                    log.warn("ROSTER MISMATCH (BallDontLie API): {} is on {}, not {}. Skipping injury.",
                        playerName, apiTeam, teamName);
                }
                return matches;
            }
        }

        // 3. Cannot verify - allow through
        return true;
    }

    /**
     * Look up a player's current team via BallDontLie API.
     * Results are cached for 1 hour.
     */
    private String lookupPlayerTeam(String playerName) {
        String cacheKey = playerName.toLowerCase().trim();

        // Check cache (not expired)
        if (isCacheValid() && playerTeamCache.containsKey(cacheKey)) {
            return playerTeamCache.get(cacheKey);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            headers.set("Accept", "application/json");

            // Search by player name
            String url = BDL_NBA_BASE_URL + "/players?search=" + playerName.replace(" ", "+");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

            if (response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");

                if (data.isArray() && data.size() > 0) {
                    // Find best match - exact name match preferred
                    for (JsonNode player : data) {
                        String firstName = player.path("first_name").asText("");
                        String lastName = player.path("last_name").asText("");
                        String fullName = (firstName + " " + lastName).trim();

                        if (fullName.equalsIgnoreCase(playerName.trim())) {
                            JsonNode teamNode = player.path("team");
                            String teamFullName = teamNode.path("full_name").asText("");

                            if (!teamFullName.isEmpty()) {
                                playerTeamCache.put(cacheKey, teamFullName);
                                if (cacheTimestamp == 0) {
                                    cacheTimestamp = System.currentTimeMillis();
                                }
                                log.debug("BallDontLie: {} -> {}", playerName, teamFullName);
                                return teamFullName;
                            }
                        }
                    }

                    // Fallback: use first result if only one match
                    if (data.size() == 1) {
                        JsonNode teamNode = data.get(0).path("team");
                        String teamFullName = teamNode.path("full_name").asText("");
                        if (!teamFullName.isEmpty()) {
                            playerTeamCache.put(cacheKey, teamFullName);
                            if (cacheTimestamp == 0) {
                                cacheTimestamp = System.currentTimeMillis();
                            }
                            return teamFullName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("BallDontLie lookup failed for {}: {}", playerName, e.getMessage());
        }

        return null;
    }

    /**
     * Check if two team names refer to the same team.
     * Handles variations like "Denver Nuggets" vs "nuggets" vs "denver nuggets".
     */
    private boolean teamNameMatches(String actualTeam, String reportedTeam) {
        if (actualTeam.equals(reportedTeam)) {
            return true;
        }

        // Check if one contains the other
        if (actualTeam.contains(reportedTeam) || reportedTeam.contains(actualTeam)) {
            return true;
        }

        // Compare last word (nickname): "Denver Nuggets" -> "nuggets"
        String actualNickname = getLastWord(actualTeam);
        String reportedNickname = getLastWord(reportedTeam);

        return actualNickname.length() > 3 && actualNickname.equals(reportedNickname);
    }

    private String getLastWord(String name) {
        String[] parts = name.trim().split("\\s+");
        return parts.length > 0 ? parts[parts.length - 1] : name;
    }

    private boolean isNbaSport(String sport) {
        if (sport == null) return false;
        String upper = sport.toUpperCase();
        return upper.equals("NBA") || upper.equals("BASKETBALL");
    }

    private boolean isApiConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    private boolean isCacheValid() {
        return cacheTimestamp > 0 &&
               (System.currentTimeMillis() - cacheTimestamp) < CACHE_TTL_MS;
    }

    /**
     * Clear the player-team cache.
     */
    public void clearCache() {
        playerTeamCache.clear();
        cacheTimestamp = 0;
        log.info("Roster validation cache cleared");
    }
}
