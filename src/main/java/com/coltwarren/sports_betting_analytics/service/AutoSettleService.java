package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.Bet;
import com.coltwarren.sports_betting_analytics.repository.BetRepository;
import com.coltwarren.sports_betting_analytics.service.espn.ESPNApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AutoSettleService {
    
    private final BetRepository betRepository;
    private final ESPNApiService espnApiService;
    
    @Autowired
    public AutoSettleService(BetRepository betRepository, ESPNApiService espnApiService) {
        this.betRepository = betRepository;
        this.espnApiService = espnApiService;
    }
    
    /**
     * Auto-settle all pending bets that have finished
     */
    public Map<String, Object> autoSettleAllBets() {
        List<Bet> pendingBets = betRepository.findByStatus("PENDING");
        
        int settled = 0;
        int failed = 0;
        List<String> results = new ArrayList<>();
        
        for (Bet bet : pendingBets) {
            // Only try to settle bets where the event has already started
            if (bet.getEventStartTime() != null && 
                bet.getEventStartTime().isBefore(LocalDateTime.now().minusHours(3))) {
                
                try {
                    String outcome = attemptAutoSettle(bet);
                    
                    if (!"PENDING".equals(outcome)) {
                        bet.setStatus(outcome);
                        bet.setSettledAt(LocalDateTime.now());
                        betRepository.save(bet);
                        settled++;
                        results.add(bet.getEventName() + ": " + outcome);
                    }
                } catch (Exception e) {
                    failed++;
                    results.add(bet.getEventName() + ": FAILED - " + e.getMessage());
                }
            }
        }
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPending", pendingBets.size());
        summary.put("settled", settled);
        summary.put("failed", failed);
        summary.put("stillPending", pendingBets.size() - settled - failed);
        summary.put("results", results);
        
        return summary;
    }
    
    /**
     * Attempt to auto-settle a single bet
     */
    public String attemptAutoSettle(Bet bet) {
        // Extract teams from event name (e.g., "Chiefs vs Bills")
        String[] teams = parseTeamsFromEvent(bet.getEventName());
        if (teams.length < 2) {
            return "PENDING"; // Can't parse teams
        }
        
        String homeTeam = teams[0];
        String awayTeam = teams[1];
        
        // Get game result from ESPN
        Map<String, Object> gameResult = espnApiService.getGameResult(
            bet.getSport(), 
            homeTeam, 
            awayTeam
        );
        
        // Extract line from selection if spread/total
        Double line = extractLineFromSelection(bet.getSelection());
        
        // Determine outcome
        return espnApiService.determineBetOutcome(
            gameResult,
            bet.getSelection(),
            bet.getBetType(),
            bet.getOdds().intValue(),
            line
        );
    }
    
    private String[] parseTeamsFromEvent(String eventName) {
        // Handle different formats: "Team1 vs Team2", "Team1 @ Team2", "Team1 - Team2"
        if (eventName.contains(" vs ")) {
            return eventName.split(" vs ");
        } else if (eventName.contains(" @ ")) {
            return eventName.split(" @ ");
        } else if (eventName.contains(" - ")) {
            return eventName.split(" - ");
        }
        return new String[]{eventName};
    }
    
    private Double extractLineFromSelection(String selection) {
        // Extract number from selection like "Chiefs -3.5" or "Over 48.5"
        try {
            String[] parts = selection.split("\\s+");
            for (String part : parts) {
                // Remove + or - and try to parse
                String cleaned = part.replace("+", "").replace("-", "");
                try {
                    return Double.parseDouble(cleaned);
                } catch (NumberFormatException e) {
                    // Continue searching
                }
            }
        } catch (Exception e) {
            // Return null if can't extract
        }
        return null;
    }
}
