package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.ActiveBetsTrackerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/active-bets")
@CrossOrigin(origins = "*")
public class ActiveBetsController {
    
    @Autowired
    private ActiveBetsTrackerService activeBetsTrackerService;
    
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
            e.printStackTrace();
            return ResponseEntity.ok(response);
        }
    }
}
