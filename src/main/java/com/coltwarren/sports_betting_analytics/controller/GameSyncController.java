package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.EspnGameSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class GameSyncController {
    
    private final EspnGameSyncService syncService;
    
    @Autowired
    public GameSyncController(EspnGameSyncService syncService) {
        this.syncService = syncService;
    }
    
    @PostMapping("/games")
    public Map<String, Object> syncGames() {
        return syncService.syncCompletedGames();
    }
    
    @PostMapping("/games/range")
    public Map<String, Object> syncDateRange(
            @RequestParam String startDate, 
            @RequestParam String endDate) {
        return syncService.syncDateRange(startDate, endDate);
    }
}
