package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.LiveGameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/live")
public class LiveBettingController {

    private final LiveGameService liveGameService;

    public LiveBettingController(LiveGameService liveGameService) {
        this.liveGameService = liveGameService;
    }
    
    @GetMapping("/games")
    public ResponseEntity<Map<String, Object>> getLiveGames(
            @RequestParam(defaultValue = "NFL") String sport) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Map<String, Object>> games = liveGameService.getLiveGames(sport);
            
            response.put("success", true);
            response.put("games", games);
            response.put("count", games.size());
            response.put("sport", sport);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(response);
        }
    }
}
