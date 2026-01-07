package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.KellyCriterionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/kelly")
public class KellyController {
    
    private final KellyCriterionService kellyService;
    
    @Autowired
    public KellyController(KellyCriterionService kellyService) {
        this.kellyService = kellyService;
    }
    
    @GetMapping("/calculate")
    public Map<String, Object> calculateKelly(
            @RequestParam int odds,
            @RequestParam double winProbability,
            @RequestParam(defaultValue = "true") boolean fractional) {
        return kellyService.calculateKelly(odds, winProbability, fractional);
    }
    
    @GetMapping("/implied-probability")
    public Map<String, Object> getImpliedProbability(@RequestParam int odds) {
        double impliedProb = kellyService.calculateImpliedProbability(odds);
        return Map.of("odds", odds, "impliedProbability", impliedProb * 100);
    }
}
