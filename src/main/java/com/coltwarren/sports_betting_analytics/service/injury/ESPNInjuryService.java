package com.coltwarren.sports_betting_analytics.service.injury;

import com.coltwarren.sports_betting_analytics.model.injury.PlayerInjury;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ESPN Injury Service
 *
 * Fetches real-time injury data from ESPN's free API.
 * No API key required - uses public endpoints.
 */
@Service
@Slf4j
public class ESPNInjuryService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RosterValidationService rosterValidationService;

    public ESPNInjuryService(RosterValidationService rosterValidationService) {
        this.rosterValidationService = rosterValidationService;
    }

    // ESPN API endpoints (free, no key needed!)
    private static final String NFL_INJURIES_URL = "https://site.api.espn.com/apis/site/v2/sports/football/nfl/injuries";
    private static final String NBA_INJURIES_URL = "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/injuries";
    private static final String MLB_INJURIES_URL = "https://site.api.espn.com/apis/site/v2/sports/baseball/mlb/injuries";
    private static final String NHL_INJURIES_URL = "https://site.api.espn.com/apis/site/v2/sports/hockey/nhl/injuries";
    private static final String CFB_INJURIES_URL = "https://site.api.espn.com/apis/site/v2/sports/football/college-football/injuries";

    // Team-specific endpoint
    private static final String TEAM_INJURIES_TEMPLATE = "https://site.api.espn.com/apis/site/v2/sports/%s/%s/teams/%s/injuries";

    // Cache for injuries (5 minute TTL)
    private final Map<String, CachedInjuries> injuryCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    private static class CachedInjuries {
        List<PlayerInjury> injuries;
        long timestamp;

        CachedInjuries(List<PlayerInjury> injuries) {
            this.injuries = injuries;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    /**
     * Get all injuries for a sport
     */
    public List<PlayerInjury> getAllInjuries(String sport) {
        String cacheKey = sport.toUpperCase();

        // Check cache first
        CachedInjuries cached = injuryCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached injuries for {}", sport);
            return cached.injuries;
        }

        String url = getInjuryUrl(sport);
        if (url == null) {
            log.warn("No injury URL for sport: {}", sport);
            return Collections.emptyList();
        }

        try {
            log.info("Fetching {} injuries from ESPN API...", sport);
            String response = restTemplate.getForObject(url, String.class);
            List<PlayerInjury> injuries = parseInjuryResponse(response, sport);

            // Cache the result
            injuryCache.put(cacheKey, new CachedInjuries(injuries));

            return injuries;
        } catch (Exception e) {
            log.error("Error fetching {} injuries: {}", sport, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get injuries for a specific team
     */
    public List<PlayerInjury> getTeamInjuries(String sport, String teamId) {
        String sportPath = getSportPath(sport);
        String leaguePath = getLeaguePath(sport);

        if (sportPath == null || leaguePath == null) {
            return Collections.emptyList();
        }

        String url = String.format(TEAM_INJURIES_TEMPLATE, sportPath, leaguePath, teamId);

        try {
            String response = restTemplate.getForObject(url, String.class);
            return parseTeamInjuryResponse(response, sport, teamId);
        } catch (Exception e) {
            log.error("Error fetching injuries for team {}: {}", teamId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Clear the injury cache
     */
    public void clearCache() {
        injuryCache.clear();
        log.info("Injury cache cleared");
    }

    private String getInjuryUrl(String sport) {
        return switch (sport.toUpperCase()) {
            case "NFL", "FOOTBALL" -> NFL_INJURIES_URL;
            case "NBA", "BASKETBALL" -> NBA_INJURIES_URL;
            case "WNBA" -> "https://site.api.espn.com/apis/site/v2/sports/basketball/wnba/injuries";
            case "MLB", "BASEBALL" -> MLB_INJURIES_URL;
            case "NHL", "HOCKEY" -> NHL_INJURIES_URL;
            case "CFB", "COLLEGE-FOOTBALL", "NCAAF" -> CFB_INJURIES_URL;
            case "WCBB" -> "https://site.api.espn.com/apis/site/v2/sports/basketball/womens-college-basketball/injuries";
            default -> null;
        };
    }

    private String getSportPath(String sport) {
        return switch (sport.toUpperCase()) {
            case "NFL", "CFB" -> "football";
            case "NBA", "WNBA", "WCBB", "CBB" -> "basketball";
            case "MLB" -> "baseball";
            case "NHL" -> "hockey";
            default -> null;
        };
    }

    private String getLeaguePath(String sport) {
        return switch (sport.toUpperCase()) {
            case "NFL" -> "nfl";
            case "NBA" -> "nba";
            case "WNBA" -> "wnba";
            case "MLB" -> "mlb";
            case "NHL" -> "nhl";
            case "CFB", "NCAAF" -> "college-football";
            case "WCBB" -> "womens-college-basketball";
            default -> null;
        };
    }

    private List<PlayerInjury> parseInjuryResponse(String response, String sport) {
        List<PlayerInjury> injuries = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(response);

            // ESPN returns injuries grouped by team
            // Structure: { "injuries": [ { "id": "1", "displayName": "Team Name", "injuries": [...] } ] }
            JsonNode teamsNode = root.path("injuries");

            if (teamsNode.isArray()) {
                for (JsonNode teamNode : teamsNode) {
                    // Team name is directly at displayName (not nested under team)
                    String teamName = teamNode.path("displayName").asText("");
                    String teamId = teamNode.path("id").asText("");
                    String teamAbbrev = teamNode.path("abbreviation").asText("");

                    JsonNode injuriesNode = teamNode.path("injuries");
                    if (injuriesNode.isArray()) {
                        for (JsonNode injuryNode : injuriesNode) {
                            PlayerInjury injury = parsePlayerInjury(injuryNode, teamName, teamId, teamAbbrev, sport);
                            if (injury != null) {
                                injuries.add(injury);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing injury response: {}", e.getMessage());
        }

        log.info("Parsed {} injuries for {}", injuries.size(), sport);
        return injuries;
    }

    private List<PlayerInjury> parseTeamInjuryResponse(String response, String sport, String teamId) {
        List<PlayerInjury> injuries = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode teamNode = root.path("team");
            String teamName = teamNode.path("displayName").asText();
            String teamAbbrev = teamNode.path("abbreviation").asText();

            JsonNode injuriesNode = root.path("injuries");
            if (injuriesNode.isArray()) {
                for (JsonNode injuryNode : injuriesNode) {
                    PlayerInjury injury = parsePlayerInjury(injuryNode, teamName, teamId, teamAbbrev, sport);
                    if (injury != null) {
                        injuries.add(injury);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing team injury response: {}", e.getMessage());
        }

        return injuries;
    }

    private PlayerInjury parsePlayerInjury(JsonNode node, String teamName, String teamId, String teamAbbrev, String sport) {
        try {
            PlayerInjury injury = new PlayerInjury();

            // Parse athlete info
            JsonNode athleteNode = node.path("athlete");
            injury.setPlayerId(athleteNode.path("id").asText());
            injury.setPlayerName(athleteNode.path("displayName").asText());

            // Position can be nested differently
            JsonNode posNode = athleteNode.path("position");
            if (posNode.isObject()) {
                injury.setPosition(posNode.path("abbreviation").asText());
            } else {
                injury.setPosition(athleteNode.path("position").asText());
            }

            injury.setTeam(teamName);
            injury.setTeamId(teamId);
            injury.setSport(sport);

            // Validate player actually belongs to this team using roster data
            if (!rosterValidationService.isPlayerOnTeam(injury.getPlayerName(), teamName, sport)) {
                log.warn("Skipping injury for {} - roster validation confirms player is NOT on {}",
                    injury.getPlayerName(), teamName);
                return null;
            }

            // Parse injury status
            injury.setStatus(node.path("status").asText("UNKNOWN"));

            // Parse injury type/description
            JsonNode typeNode = node.path("type");
            if (typeNode.isObject()) {
                injury.setInjuryType(typeNode.path("description").asText("Unknown"));
            } else {
                injury.setInjuryType(node.path("description").asText("Unknown"));
            }

            // Parse details
            JsonNode detailsNode = node.path("details");
            if (detailsNode.isObject()) {
                injury.setDetails(detailsNode.path("detail").asText(""));
            } else {
                injury.setDetails(node.path("longComment").asText(""));
            }

            injury.setLastUpdated(LocalDateTime.now());

            // Estimate player rating based on position and status
            injury.setPlayerRating(estimatePlayerRating(injury.getPosition(), sport));
            injury.setIsStarter(injury.getPlayerRating() >= 70);
            injury.setIsProBowler(false); // Would need additional API call

            return injury;
        } catch (Exception e) {
            log.error("Error parsing player injury: {}", e.getMessage());
            return null;
        }
    }

    private Integer estimatePlayerRating(String position, String sport) {
        if (position == null) return 60;

        // Higher rating = more important to team
        if (sport.equalsIgnoreCase("NFL") || sport.equalsIgnoreCase("CFB")) {
            return switch (position.toUpperCase()) {
                case "QB" -> 95;
                case "LT", "RT" -> 80;
                case "WR" -> 75;
                case "TE" -> 72;
                case "RB" -> 70;
                case "DE", "EDGE" -> 75;
                case "DT", "NT" -> 70;
                case "LB", "MLB", "OLB", "ILB" -> 70;
                case "CB" -> 73;
                case "S", "FS", "SS" -> 68;
                case "K" -> 50;
                case "P" -> 45;
                case "OL", "OG", "C" -> 70;
                default -> 60;
            };
        } else if (sport.equalsIgnoreCase("NBA") || sport.equalsIgnoreCase("CBB")) {
            return switch (position.toUpperCase()) {
                case "PG" -> 82;
                case "SG" -> 78;
                case "SF" -> 78;
                case "PF" -> 76;
                case "C" -> 80;
                case "G" -> 78;
                case "F" -> 76;
                default -> 70;
            };
        } else if (sport.equalsIgnoreCase("MLB")) {
            return switch (position.toUpperCase()) {
                case "SP", "P" -> 85;
                case "RP", "CL" -> 70;
                case "C" -> 75;
                case "SS" -> 75;
                case "2B" -> 70;
                case "3B" -> 72;
                case "1B" -> 68;
                case "LF", "CF", "RF", "OF" -> 70;
                case "DH" -> 65;
                default -> 60;
            };
        } else if (sport.equalsIgnoreCase("NHL")) {
            return switch (position.toUpperCase()) {
                case "G" -> 85;
                case "C" -> 80;
                case "LW", "RW", "W" -> 75;
                case "D" -> 75;
                default -> 70;
            };
        }
        return 60;
    }
}
