package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.MultiSportBestBetsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/best-bets")
public class MultiSportBestBetsController {
    
    @Autowired
    private MultiSportBestBetsService multiSportBestBetsService;
    
    @GetMapping("/all-sports")
    public ResponseEntity<Map<String, Object>> getBestBetsAllSports(
            @RequestParam(required = false) String sport) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("🎯 Getting best bets for all sports...");
            
            // Get all best bets
            List<Map<String, Object>> allBets = multiSportBestBetsService.getBestBetsAcrossAllSports();
            
            // Filter by sport if requested
            List<Map<String, Object>> filteredBets = allBets;
            if (sport != null && !sport.isEmpty() && !sport.equalsIgnoreCase("ALL")) {
                filteredBets = allBets.stream()
                    .filter(bet -> sport.equalsIgnoreCase((String) bet.get("sport")))
                    .collect(Collectors.toList());
            }
            
            // Get sport counts
            Map<String, Long> sportCounts = allBets.stream()
                .collect(Collectors.groupingBy(
                    bet -> (String) bet.get("sport"),
                    Collectors.counting()
                ));
            
            response.put("success", true);
            response.put("totalBets", allBets.size());
            response.put("filteredBets", filteredBets.size());
            response.put("bets", filteredBets);
            response.put("sportCounts", sportCounts);
            response.put("appliedFilter", sport != null ? sport : "ALL");
            
            System.out.println("✅ Returning " + filteredBets.size() + " best bets");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error getting best bets: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
