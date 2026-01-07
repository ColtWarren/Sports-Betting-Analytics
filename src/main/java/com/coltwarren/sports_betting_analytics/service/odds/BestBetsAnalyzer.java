package com.coltwarren.sports_betting_analytics.service.odds;

import com.coltwarren.sports_betting_analytics.model.odds.OddsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BestBetsAnalyzer {
    
    private final OddsService oddsService;
    
    @Autowired
    public BestBetsAnalyzer(OddsService oddsService) {
        this.oddsService = oddsService;
    }
    
    public List<Map<String, Object>> findBestBetsToday(String sport, int limit) {
        String sportKey = oddsService.getSportKey(sport);
        List<OddsResponse> allGames = oddsService.getLiveOdds(sportKey);
        
        List<Map<String, Object>> bestBets = new ArrayList<>();
        
        for (OddsResponse game : allGames) {
            // Analyze moneyline opportunities
            analyzeBestOdds(game, "h2h", "MONEYLINE", bestBets);
            
            // Analyze spread opportunities
            analyzeBestOdds(game, "spreads", "SPREAD", bestBets);
            
            // Analyze totals opportunities
            analyzeBestOdds(game, "totals", "TOTALS", bestBets);
        }
        
        // Sort by value (biggest difference between best and worst odds)
        bestBets.sort((a, b) -> {
            Double valueA = (Double) a.get("value");
            Double valueB = (Double) b.get("value");
            return valueB.compareTo(valueA);
        });
        
        return bestBets.stream().limit(limit).collect(Collectors.toList());
    }
    
    private void analyzeBestOdds(OddsResponse game, String marketKey, String betType, List<Map<String, Object>> bestBets) {
        for (OddsResponse.Bookmaker bookmaker : game.getBookmakers()) {
            for (OddsResponse.Market market : bookmaker.getMarkets()) {
                if (market.getKey().equals(marketKey)) {
                    for (OddsResponse.Outcome outcome : market.getOutcomes()) {
                        
                        // Find best and worst odds for this outcome across all books
                        Map<String, Object> oddsComparison = findOddsRange(game, marketKey, outcome.getName());
                        
                        if (oddsComparison.get("value") != null) {
                            Double value = (Double) oddsComparison.get("value");
                            
                            // Only include if there's significant value (10+ points difference)
                            if (value >= 10) {
                                Map<String, Object> bet = new HashMap<>();
                                bet.put("game", game.getAway_team() + " @ " + game.getHome_team());
                                bet.put("sport", game.getSport_title());
                                bet.put("commence_time", game.getCommence_time());
                                bet.put("betType", betType);
                                bet.put("selection", outcome.getName());
                                bet.put("point", outcome.getPoint());
                                bet.put("bestBook", oddsComparison.get("bestBook"));
                                bet.put("bestOdds", oddsComparison.get("bestOdds"));
                                bet.put("worstBook", oddsComparison.get("worstBook"));
                                bet.put("worstOdds", oddsComparison.get("worstOdds"));
                                bet.put("value", value);
                                bet.put("allBooks", oddsComparison.get("allBooks"));
                                
                                bestBets.add(bet);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private Map<String, Object> findOddsRange(OddsResponse game, String marketKey, String outcomeName) {
        Map<String, Object> result = new HashMap<>();
        
        Double bestOdds = Double.NEGATIVE_INFINITY;
        Double worstOdds = Double.POSITIVE_INFINITY;
        String bestBook = null;
        String worstBook = null;
        Map<String, Double> allBooks = new HashMap<>();
        
        for (OddsResponse.Bookmaker bookmaker : game.getBookmakers()) {
            for (OddsResponse.Market market : bookmaker.getMarkets()) {
                if (market.getKey().equals(marketKey)) {
                    for (OddsResponse.Outcome outcome : market.getOutcomes()) {
                        if (outcome.getName().equals(outcomeName)) {
                            Double price = outcome.getPrice();
                            allBooks.put(bookmaker.getTitle(), price);
                            
                            if (price > bestOdds) {
                                bestOdds = price;
                                bestBook = bookmaker.getTitle();
                            }
                            if (price < worstOdds) {
                                worstOdds = price;
                                worstBook = bookmaker.getTitle();
                            }
                        }
                    }
                }
            }
        }
        
        if (bestBook != null && worstBook != null) {
            result.put("bestOdds", bestOdds);
            result.put("worstOdds", worstOdds);
            result.put("bestBook", bestBook);
            result.put("worstBook", worstBook);
            result.put("value", Math.abs(bestOdds - worstOdds));
            result.put("allBooks", allBooks);
        }
        
        return result;
    }
}
