package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.MultiSportBestBetsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/best-bets")
public class MultiSportBestBetsController {

    private final MultiSportBestBetsService multiSportBestBetsService;

    public MultiSportBestBetsController(MultiSportBestBetsService multiSportBestBetsService) {
        this.multiSportBestBetsService = multiSportBestBetsService;
    }
    
    @GetMapping("/all-sports")
    public ResponseEntity<Map<String, Object>> getBestBetsAllSports(
            @RequestParam(required = false) String sport) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            log.info("Getting best bets for all sports...");

            response = multiSportBestBetsService.getBestBetsFiltered(sport);
            response.put("success", true);

            log.info("Returning {} best bets", response.get("filteredBets"));

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting best bets: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
