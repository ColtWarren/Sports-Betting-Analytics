package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.GameStats;
import com.coltwarren.sports_betting_analytics.repository.GameStatsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class StatsService {
    
    private final GameStatsRepository gameStatsRepository;
    
    public StatsService(GameStatsRepository gameStatsRepository) {
        this.gameStatsRepository = gameStatsRepository;
    }
    
    public Map<String, Object> getTeamStats(String teamName) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get ATS record
            Long atsWins = gameStatsRepository.countATSWins(teamName);
            Long atsLosses = gameStatsRepository.countATSLosses(teamName);
            Long totalATS = atsWins + atsLosses;
            
            stats.put("atsWins", atsWins);
            stats.put("atsLosses", atsLosses);
            stats.put("atsRecord", String.format("%d-%d", atsWins, atsLosses));
            stats.put("atsPercentage", totalATS > 0 ? (atsWins * 100.0 / totalATS) : 0);
            
            // Get over/under record
            Long overs = gameStatsRepository.countOvers(teamName);
            Long unders = gameStatsRepository.countUnders(teamName);
            Long totalOU = overs + unders;
            
            stats.put("overs", overs);
            stats.put("unders", unders);
            stats.put("ouRecord", String.format("%d-%d", overs, unders));
            stats.put("overPercentage", totalOU > 0 ? (overs * 100.0 / totalOU) : 0);
            
            // Get recent games (last 5)
            LocalDateTime fiveGamesAgo = LocalDateTime.now().minusWeeks(8);
            List<GameStats> recentGames = gameStatsRepository.findRecentByTeam(teamName, fiveGamesAgo);
            
            if (recentGames.size() >= 5) {
                recentGames = recentGames.subList(0, 5);
            }
            
            stats.put("recentGames", recentGames);
            stats.put("recentATS", calculateRecentATS(recentGames, teamName));
            
            stats.put("available", true);
            
        } catch (Exception e) {
            stats.put("available", false);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    public Map<String, Object> getMatchupStats(String team1, String team2) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get individual team stats
            Map<String, Object> team1Stats = getTeamStats(team1);
            Map<String, Object> team2Stats = getTeamStats(team2);
            
            stats.put("team1", team1);
            stats.put("team2", team2);
            stats.put("team1Stats", team1Stats);
            stats.put("team2Stats", team2Stats);
            
            // Get head-to-head history
            List<GameStats> h2h = gameStatsRepository.findHeadToHead(team1, team2, PageRequest.of(0, 50));
            stats.put("headToHead", h2h);
            stats.put("h2hCount", h2h.size());
            
            // Generate summary
            stats.put("summary", generateMatchupSummary(team1, team1Stats, team2, team2Stats, h2h));
            
            stats.put("available", true);
            
        } catch (Exception e) {
            stats.put("available", false);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    private String calculateRecentATS(List<GameStats> games, String teamName) {
        int wins = 0;
        int losses = 0;
        
        for (GameStats game : games) {
            boolean isHome = game.getHomeTeam().equals(teamName);
            Boolean covered = isHome ? game.getHomeCoveredSpread() : game.getAwayCoveredSpread();
            
            if (covered != null) {
                if (covered) wins++;
                else losses++;
            }
        }
        
        return String.format("%d-%d", wins, losses);
    }
    
    private String generateMatchupSummary(String team1, Map<String, Object> team1Stats,
                                         String team2, Map<String, Object> team2Stats,
                                         List<GameStats> h2h) {
        StringBuilder summary = new StringBuilder();
        
        // ATS comparison
        double team1ATS = (double) team1Stats.getOrDefault("atsPercentage", 0.0);
        double team2ATS = (double) team2Stats.getOrDefault("atsPercentage", 0.0);
        
        if (team1ATS > 55) {
            summary.append(String.format("✅ %s strong ATS: %s (%.1f%%). ", 
                team1, team1Stats.get("atsRecord"), team1ATS));
        } else if (team1ATS < 45) {
            summary.append(String.format("⚠️ %s poor ATS: %s (%.1f%%). ", 
                team1, team1Stats.get("atsRecord"), team1ATS));
        }
        
        if (team2ATS > 55) {
            summary.append(String.format("✅ %s strong ATS: %s (%.1f%%). ", 
                team2, team2Stats.get("atsRecord"), team2ATS));
        } else if (team2ATS < 45) {
            summary.append(String.format("⚠️ %s poor ATS: %s (%.1f%%). ", 
                team2, team2Stats.get("atsRecord"), team2ATS));
        }
        
        // O/U trends
        double team1Over = (double) team1Stats.getOrDefault("overPercentage", 0.0);
        double team2Over = (double) team2Stats.getOrDefault("overPercentage", 0.0);
        
        if (team1Over > 60 && team2Over > 60) {
            summary.append("📈 Both teams trend OVER. ");
        } else if (team1Over < 40 && team2Over < 40) {
            summary.append("📉 Both teams trend UNDER. ");
        }
        
        // H2H
        if (!h2h.isEmpty()) {
            summary.append(String.format("Last met %d time(s). ", h2h.size()));
        }
        
        if (summary.length() == 0) {
            summary.append("Limited historical data available.");
        }
        
        return summary.toString().trim();
    }
    
    @Transactional
    public GameStats saveGameStats(GameStats gameStats) {
        gameStats.setUpdatedAt(LocalDateTime.now());
        return gameStatsRepository.save(gameStats);
    }
}
