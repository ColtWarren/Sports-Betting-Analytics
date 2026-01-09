package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.ai.MatchupAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/matchup-analysis")
public class MatchupAnalysisController {
    
    private final MatchupAnalyzerService analyzerService;
    
    @Autowired
    public MatchupAnalysisController(MatchupAnalyzerService analyzerService) {
        this.analyzerService = analyzerService;
    }
    
    @PostMapping("/analyze")
    public Map<String, Object> analyzeMatchup(
            @RequestParam String game,
            @RequestParam String betType,
            @RequestParam String selection,
            @RequestParam int bestOdds,
            @RequestParam int worstOdds,
            @RequestParam double valuePoints) {
        
        String analysis = analyzerService.analyzeMatchup(
            game, betType, selection, bestOdds, worstOdds, valuePoints
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("analysis", analysis);
        response.put("game", game);
        
        return response;
    }
}
