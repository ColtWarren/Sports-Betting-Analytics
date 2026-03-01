package com.coltwarren.sports_betting_analytics.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EspnService {
    
    private final WebClient webClient;
    
    // Team name to ESPN team ID mapping
    private static final Map<String, String> NFL_TEAM_IDS = new ConcurrentHashMap<>();
    static {
        NFL_TEAM_IDS.put("Bills", "2");
        NFL_TEAM_IDS.put("Buffalo", "2");
        NFL_TEAM_IDS.put("Dolphins", "15");
        NFL_TEAM_IDS.put("Miami", "15");
        NFL_TEAM_IDS.put("Patriots", "17");
        NFL_TEAM_IDS.put("New England", "17");
        NFL_TEAM_IDS.put("Jets", "20");
        NFL_TEAM_IDS.put("Ravens", "33");
        NFL_TEAM_IDS.put("Baltimore", "33");
        NFL_TEAM_IDS.put("Bengals", "4");
        NFL_TEAM_IDS.put("Cincinnati", "4");
        NFL_TEAM_IDS.put("Browns", "5");
        NFL_TEAM_IDS.put("Cleveland", "5");
        NFL_TEAM_IDS.put("Steelers", "23");
        NFL_TEAM_IDS.put("Pittsburgh", "23");
        NFL_TEAM_IDS.put("Texans", "34");
        NFL_TEAM_IDS.put("Houston", "34");
        NFL_TEAM_IDS.put("Colts", "11");
        NFL_TEAM_IDS.put("Indianapolis", "11");
        NFL_TEAM_IDS.put("Jaguars", "30");
        NFL_TEAM_IDS.put("Jacksonville", "30");
        NFL_TEAM_IDS.put("Titans", "10");
        NFL_TEAM_IDS.put("Tennessee", "10");
        NFL_TEAM_IDS.put("Broncos", "7");
        NFL_TEAM_IDS.put("Denver", "7");
        NFL_TEAM_IDS.put("Chiefs", "12");
        NFL_TEAM_IDS.put("Kansas City", "12");
        NFL_TEAM_IDS.put("Raiders", "13");
        NFL_TEAM_IDS.put("Las Vegas", "13");
        NFL_TEAM_IDS.put("Chargers", "24");
        NFL_TEAM_IDS.put("Cowboys", "6");
        NFL_TEAM_IDS.put("Dallas", "6");
        NFL_TEAM_IDS.put("Giants", "19");
        NFL_TEAM_IDS.put("Eagles", "21");
        NFL_TEAM_IDS.put("Philadelphia", "21");
        NFL_TEAM_IDS.put("Commanders", "28");
        NFL_TEAM_IDS.put("Washington", "28");
        NFL_TEAM_IDS.put("Bears", "3");
        NFL_TEAM_IDS.put("Chicago", "3");
        NFL_TEAM_IDS.put("Lions", "8");
        NFL_TEAM_IDS.put("Detroit", "8");
        NFL_TEAM_IDS.put("Packers", "9");
        NFL_TEAM_IDS.put("Green Bay", "9");
        NFL_TEAM_IDS.put("Vikings", "16");
        NFL_TEAM_IDS.put("Minnesota", "16");
        NFL_TEAM_IDS.put("Falcons", "1");
        NFL_TEAM_IDS.put("Atlanta", "1");
        NFL_TEAM_IDS.put("Panthers", "29");
        NFL_TEAM_IDS.put("Carolina", "29");
        NFL_TEAM_IDS.put("Saints", "18");
        NFL_TEAM_IDS.put("New Orleans", "18");
        NFL_TEAM_IDS.put("Buccaneers", "27");
        NFL_TEAM_IDS.put("Tampa Bay", "27");
        NFL_TEAM_IDS.put("Cardinals", "22");
        NFL_TEAM_IDS.put("Arizona", "22");
        NFL_TEAM_IDS.put("Rams", "14");
        NFL_TEAM_IDS.put("Los Angeles", "14");
        NFL_TEAM_IDS.put("49ers", "25");
        NFL_TEAM_IDS.put("San Francisco", "25");
        NFL_TEAM_IDS.put("Seahawks", "26");
        NFL_TEAM_IDS.put("Seattle", "26");
    }
    
    public EspnService() {
        this.webClient = WebClient.builder()
            .baseUrl("https://site.api.espn.com/apis/site/v2/sports/football/nfl")
            .build();
    }
    
    public Map<String, Object> getInjuriesForGame(String game) {
        Map<String, Object> result = new HashMap<>();
        result.put("available", false);
        
        try {
            // Extract team names from game string (e.g., "Bills @ Jaguars")
            String[] teams = extractTeams(game);
            if (teams == null || teams.length != 2) {
                result.put("reason", "Could not parse team names");
                return result;
            }
            
            String team1 = teams[0];
            String team2 = teams[1];
            
            // Get team IDs
            String teamId1 = getTeamId(team1);
            String teamId2 = getTeamId(team2);
            
            if (teamId1 == null || teamId2 == null) {
                result.put("reason", "Teams not found: " + team1 + " / " + team2);
                return result;
            }
            
            // Fetch injuries for both teams
            List<Map<String, Object>> team1Injuries = getTeamInjuries(teamId1, team1);
            List<Map<String, Object>> team2Injuries = getTeamInjuries(teamId2, team2);
            
            result.put("available", true);
            result.put("team1", team1);
            result.put("team2", team2);
            result.put("team1Injuries", team1Injuries);
            result.put("team2Injuries", team2Injuries);
            result.put("team1Count", team1Injuries.size());
            result.put("team2Count", team2Injuries.size());
            
            // Generate impact summary
            result.put("impactSummary", generateImpactSummary(team1, team1Injuries, team2, team2Injuries));
            
        } catch (Exception e) {
            result.put("reason", "Error fetching injuries: " + e.getMessage());
        }
        
        return result;
    }
    
    private String[] extractTeams(String game) {
        // Handle formats like "Bills @ Jaguars" or "Buffalo Bills @ Jacksonville Jaguars"
        String[] parts = game.split("@|vs|VS");
        if (parts.length != 2) return null;
        
        String team1 = parts[0].trim();
        String team2 = parts[1].trim();
        
        // Extract last word (usually team name)
        String[] team1Words = team1.split(" ");
        String[] team2Words = team2.split(" ");
        
        return new String[]{
            team1Words[team1Words.length - 1],
            team2Words[team2Words.length - 1]
        };
    }
    
    private String getTeamId(String teamName) {
        return NFL_TEAM_IDS.get(teamName);
    }
    
    private List<Map<String, Object>> getTeamInjuries(String teamId, String teamName) {
        try {
            Map<String, Object> response = webClient.get()
                .uri("/teams/" + teamId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (response == null || !response.containsKey("team")) {
                return Collections.emptyList();
            }
            
            Map<String, Object> team = (Map<String, Object>) response.get("team");
            
            if (!team.containsKey("injuries")) {
                return Collections.emptyList();
            }
            
            List<Map<String, Object>> injuries = (List<Map<String, Object>>) team.get("injuries");
            
            // Parse and format injuries
            return injuries.stream()
                .map(this::formatInjury)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Error fetching injuries for team {}: {}", teamId, e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private Map<String, Object> formatInjury(Map<String, Object> injury) {
        try {
            Map<String, Object> formatted = new HashMap<>();
            
            // Get athlete info
            Map<String, Object> athlete = (Map<String, Object>) injury.get("athlete");
            if (athlete != null) {
                formatted.put("name", athlete.get("displayName"));
                formatted.put("position", athlete.get("position"));
            }
            
            // Get injury details
            formatted.put("status", injury.get("status"));
            formatted.put("type", injury.get("type"));
            formatted.put("details", injury.getOrDefault("details", ""));
            
            // Determine severity icon
            String status = (String) injury.get("status");
            formatted.put("icon", getInjuryIcon(status));
            formatted.put("severity", getInjurySeverity(status));
            
            return formatted;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private String getInjuryIcon(String status) {
        if (status == null) return "❓";
        
        switch (status.toUpperCase()) {
            case "OUT":
                return "❌";
            case "DOUBTFUL":
                return "🔴";
            case "QUESTIONABLE":
                return "🟡";
            case "PROBABLE":
                return "🟢";
            case "DAY TO DAY":
                return "🟠";
            default:
                return "ℹ️";
        }
    }
    
    private String getInjurySeverity(String status) {
        if (status == null) return "UNKNOWN";
        
        switch (status.toUpperCase()) {
            case "OUT":
                return "HIGH";
            case "DOUBTFUL":
                return "HIGH";
            case "QUESTIONABLE":
                return "MEDIUM";
            case "PROBABLE":
                return "LOW";
            default:
                return "LOW";
        }
    }
    
    private String generateImpactSummary(String team1, List<Map<String, Object>> team1Injuries,
                                        String team2, List<Map<String, Object>> team2Injuries) {
        StringBuilder summary = new StringBuilder();
        
        long team1Critical = team1Injuries.stream()
            .filter(i -> "HIGH".equals(i.get("severity")))
            .count();
        
        long team2Critical = team2Injuries.stream()
            .filter(i -> "HIGH".equals(i.get("severity")))
            .count();
        
        if (team1Critical > team2Critical) {
            summary.append(String.format("⚠️ %s significantly impacted with %d critical injuries. ", 
                team1, team1Critical));
        } else if (team2Critical > team1Critical) {
            summary.append(String.format("⚠️ %s significantly impacted with %d critical injuries. ", 
                team2, team2Critical));
        } else if (team1Critical > 0) {
            summary.append(String.format("⚠️ Both teams dealing with injuries (%d critical each). ", 
                team1Critical));
        } else {
            summary.append("✅ Both teams relatively healthy. ");
        }
        
        if (team1Injuries.isEmpty() && team2Injuries.isEmpty()) {
            summary.append("No major injury concerns reported.");
        }
        
        return summary.toString().trim();
    }
}
