package com.coltwarren.sports_betting_analytics.service.odds;

import com.coltwarren.sports_betting_analytics.model.odds.OddsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class OddsService {
    
    private final WebClient webClient;
    private final String apiKey;
    
    public OddsService(
            @Value("${odds.api.url}") String apiUrl,
            @Value("${odds.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
            .baseUrl(apiUrl)
            .build();
    }
    
    public List<OddsResponse> getLiveOdds(String sportKey) {
        try {
            String url = String.format("/sports/%s/odds?apiKey=%s&regions=us&markets=h2h,spreads,totals&oddsFormat=american", 
                                      sportKey, apiKey);
            
            Mono<List<OddsResponse>> response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<OddsResponse>>() {});
            
            return response.block();
            
        } catch (Exception e) {
            System.err.println("Error fetching odds: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public Map<String, Object> findBestOdds(String sportKey, String teamName, String marketType) {
        List<OddsResponse> allGames = getLiveOdds(sportKey);
        
        Map<String, Object> result = new HashMap<>();
        result.put("found", false);
        
        for (OddsResponse game : allGames) {
            if (game.getHome_team().equalsIgnoreCase(teamName) || 
                game.getAway_team().equalsIgnoreCase(teamName)) {
                
                result.put("found", true);
                result.put("game", String.format("%s vs %s", game.getAway_team(), game.getHome_team()));
                result.put("commence_time", game.getCommence_time());
                
                Map<String, Map<String, Object>> bookmakerOdds = new HashMap<>();
                
                for (OddsResponse.Bookmaker bookmaker : game.getBookmakers()) {
                    for (OddsResponse.Market market : bookmaker.getMarkets()) {
                        if (market.getKey().equals(marketType)) {
                            for (OddsResponse.Outcome outcome : market.getOutcomes()) {
                                if (outcome.getName().equalsIgnoreCase(teamName)) {
                                    Map<String, Object> oddsData = new HashMap<>();
                                    oddsData.put("price", outcome.getPrice());
                                    if (outcome.getPoint() != null) {
                                        oddsData.put("point", outcome.getPoint());
                                    }
                                    bookmakerOdds.put(bookmaker.getTitle(), oddsData);
                                }
                            }
                        }
                    }
                }
                
                result.put("bookmakers", bookmakerOdds);
                
                if (!bookmakerOdds.isEmpty()) {
                    String bestBook = null;
                    Double bestOdds = Double.NEGATIVE_INFINITY;
                    
                    for (Map.Entry<String, Map<String, Object>> entry : bookmakerOdds.entrySet()) {
                        Double price = (Double) entry.getValue().get("price");
                        if (price > bestOdds) {
                            bestOdds = price;
                            bestBook = entry.getKey();
                        }
                    }
                    
                    result.put("bestBook", bestBook);
                    result.put("bestOdds", bestOdds);
                }
                
                break;
            }
        }
        
        return result;
    }
    
    public String getSportKey(String sport) {
        return switch (sport.toUpperCase()) {
            case "NFL" -> "americanfootball_nfl";
            case "NBA" -> "basketball_nba";
            case "MLB" -> "baseball_mlb";
            case "NHL" -> "icehockey_nhl";
            case "NCAAF" -> "americanfootball_ncaaf";
            case "NCAAB" -> "basketball_ncaab";
            default -> "americanfootball_nfl";
        };
    }
    
    public String getMarketKey(String betType) {
        return switch (betType.toUpperCase()) {
            case "MONEYLINE" -> "h2h";
            case "SPREAD" -> "spreads";
            case "TOTAL_OVER", "TOTAL_UNDER" -> "totals";
            default -> "h2h";
        };
    }
}
