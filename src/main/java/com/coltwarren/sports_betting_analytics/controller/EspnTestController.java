package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.EspnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/espn")
public class EspnTestController {
    
    private final EspnService espnService;
    
    @Autowired
    public EspnTestController(EspnService espnService) {
        this.espnService = espnService;
    }
    
    @GetMapping("/injuries")
    public Map<String, Object> getInjuries(@RequestParam(defaultValue = "Buffalo Bills @ Jacksonville Jaguars") String game) {
        return espnService.getInjuriesForGame(game);
    }
}
