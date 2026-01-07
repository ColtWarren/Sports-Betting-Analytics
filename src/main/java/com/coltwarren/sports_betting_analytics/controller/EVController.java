package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.AdvancedEVCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ev")
public class EVController {
    
    private final AdvancedEVCalculator evCalculator;
    
    @Autowired
    public EVController(AdvancedEVCalculator evCalculator) {
        this.evCalculator = evCalculator;
    }
    
    @GetMapping("/analyze")
    public Map<String, Object> analyzeEV(
            @RequestParam String sport,
            @RequestParam String event,
            @RequestParam String selection,
            @RequestParam int odds,
            @RequestParam String betType,
            @RequestParam(required = false) String context) {
        
        return evCalculator.analyzeEV(sport, event, selection, odds, betType, context);
    }
    
    @GetMapping("/simple")
    public Map<String, Object> calculateSimpleEV(
            @RequestParam int odds,
            @RequestParam double winProbability) {
        
        return evCalculator.calculateSimpleEV(odds, winProbability);
    }
}
