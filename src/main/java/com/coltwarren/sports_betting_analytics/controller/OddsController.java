package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.model.odds.OddsResponse;
import com.coltwarren.sports_betting_analytics.service.odds.OddsService;
import com.coltwarren.sports_betting_analytics.service.odds.BestBetsAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/odds")
public class OddsController {
    
    private final OddsService oddsService;
    private final BestBetsAnalyzer bestBetsAnalyzer;
    
    @Autowired
    public OddsController(OddsService oddsService, BestBetsAnalyzer bestBetsAnalyzer) {
        this.oddsService = oddsService;
        this.bestBetsAnalyzer = bestBetsAnalyzer;
    }
    
    @GetMapping("/{sport}")
    public List<OddsResponse> getOdds(@PathVariable String sport) {
        String sportKey = oddsService.getSportKey(sport);
        return oddsService.getLiveOdds(sportKey);
    }
    
    @GetMapping("/best")
    public Map<String, Object> getBestOdds(
            @RequestParam String sport,
            @RequestParam String team,
            @RequestParam String betType) {
        
        String sportKey = oddsService.getSportKey(sport);
        String marketKey = oddsService.getMarketKey(betType);
        
        return oddsService.findBestOdds(sportKey, team, marketKey);
    }
    
    @GetMapping("/best-bets-today")
    public List<Map<String, Object>> getBestBetsToday(
            @RequestParam(defaultValue = "NFL") String sport,
            @RequestParam(defaultValue = "10") int limit) {
        return bestBetsAnalyzer.findBestBetsToday(sport, limit);
    }
}
