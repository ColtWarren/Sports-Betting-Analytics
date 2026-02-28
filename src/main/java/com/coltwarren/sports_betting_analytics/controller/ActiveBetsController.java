package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.ActiveBetsTrackerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/active-bets")
public class ActiveBetsController {

    private final ActiveBetsTrackerService activeBetsTrackerService;

    public ActiveBetsController(ActiveBetsTrackerService activeBetsTrackerService) {
        this.activeBetsTrackerService = activeBetsTrackerService;
    }
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getActiveBets() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Map<String, Object>> activeBets = activeBetsTrackerService.getActiveBetsWithLiveData();
            
            // Separate by status
            List<Map<String, Object>> liveBets = new ArrayList<>();
            List<Map<String, Object>> upcomingBets = new ArrayList<>();
            
            for (Map<String, Object> bet : activeBets) {
                Boolean isLive = (Boolean) bet.get("isLive");
                if (isLive != null && isLive) {
                    liveBets.add(bet);
                } else {
                    upcomingBets.add(bet);
                }
            }
            
            response.put("success", true);
            response.put("totalActiveBets", activeBets.size());
            response.put("liveBets", liveBets);
            response.put("liveCount", liveBets.size());
            response.put("upcomingBets", upcomingBets);
            response.put("upcomingCount", upcomingBets.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            log.error("Error fetching active bets", e);
            return ResponseEntity.ok(response);
        }
    }
}
