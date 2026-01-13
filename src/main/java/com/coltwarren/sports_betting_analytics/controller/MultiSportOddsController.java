package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.odds.MultiSportOddsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/odds/multi-sport")
@CrossOrigin(origins = "*")
public class MultiSportOddsController {
    
    @Autowired
    private MultiSportOddsService multiSportOddsService;
    
    @GetMapping("/{sport}")
    public ResponseEntity<Map<String, Object>> getOddsForSport(@PathVariable String sport) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Map<String, Object>> odds = multiSportOddsService.getOddsForSport(sport);
            
            response.put("success", true);
            response.put("sport", sport);
            response.put("games", odds);
            response.put("count", odds.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    @GetMapping("/best-odds")
    public ResponseEntity<Map<String, Object>> getBestOdds(
            @RequestParam String sport,
            @RequestParam String homeTeam,
            @RequestParam String awayTeam,
            @RequestParam String betType) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> bestOdds = multiSportOddsService.getBestOddsForGame(
                sport, homeTeam, awayTeam, betType);
            
            response.put("success", true);
            response.put("bestOdds", bestOdds);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
