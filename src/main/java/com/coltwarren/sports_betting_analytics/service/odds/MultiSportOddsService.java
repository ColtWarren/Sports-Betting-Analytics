package com.coltwarren.sports_betting_analytics.service.odds;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class MultiSportOddsService {
    
    @Value("${odds.api.key}")
    private String apiKey;
    
    private final WebClient oddsClient;
    
    public MultiSportOddsService() {
        this.oddsClient = WebClient.builder()
            .baseUrl("https://api.the-odds-api.com/v4")
            .build();
    }
    
    public List<Map<String, Object>> getOddsForSport(String sport) {
        try {
            String sportKey = getSportKey(sport);
            
            // The Odds API returns an array directly, not an object
            List<Map<String, Object>> oddsData = oddsClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/sports/" + sportKey + "/odds")
                    .queryParam("apiKey", apiKey)
                    .queryParam("regions", "us")
                    .queryParam("markets", "h2h,spreads,totals")
                    .queryParam("oddsFormat", "american")
                    .build())
                .retrieve()
                .bodyToMono(List.class)
                .block();
            
            if (oddsData == null || oddsData.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<Map<String, Object>> games = new ArrayList<>();
            
            for (Map<String, Object> game : oddsData) {
                Map<String, Object> parsedGame = parseGame(game, sport);
                games.add(parsedGame);
            }
            
            return games;
            
        } catch (Exception e) {
            System.err.println("Error fetching odds for " + sport + ": " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    
    public Map<String, Object> getBestOddsForGame(String sport, String homeTeam, String awayTeam, String betType) {
        try {
            List<Map<String, Object>> allOdds = getOddsForSport(sport);
            
            // Find the game
            for (Map<String, Object> game : allOdds) {
                String gameHome = (String) game.get("homeTeam");
                String gameAway = (String) game.get("awayTeam");
                
                if (matchesTeams(gameHome, homeTeam) && matchesTeams(gameAway, awayTeam)) {
                    return findBestOddsForBetType(game, betType);
                }
            }
            
            return Collections.emptyMap();
            
        } catch (Exception e) {
            System.err.println("Error finding best odds: " + e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    private String getSportKey(String sport) {
        switch (sport.toUpperCase()) {
            case "NFL":
                return "americanfootball_nfl";
            case "CFB":
            case "COLLEGE-FOOTBALL":
                return "americanfootball_ncaaf";
            case "NBA":
                return "basketball_nba";
            case "CBB":
            case "COLLEGE-BASKETBALL":
                return "basketball_ncaab";
            case "MLB":
                return "baseball_mlb";
            case "NHL":
                return "icehockey_nhl";
            case "MLS":
                return "soccer_usa_mls";
            case "EPL":
                return "soccer_epl";
            case "UFC":
                return "mma_mixed_martial_arts";
            default:
                return "americanfootball_nfl";
        }
    }
    
    private Map<String, Object> parseGame(Map<String, Object> game, String sport) {
        Map<String, Object> parsedGame = new HashMap<>();
        
        try {
            parsedGame.put("sport", sport);
            parsedGame.put("gameId", game.get("id"));
            parsedGame.put("homeTeam", game.get("home_team"));
            parsedGame.put("awayTeam", game.get("away_team"));
            parsedGame.put("commenceTime", game.get("commence_time"));
            
            // Parse bookmaker odds
            List<Map<String, Object>> bookmakers = (List<Map<String, Object>>) game.get("bookmakers");
            
            if (bookmakers != null && !bookmakers.isEmpty()) {
                List<Map<String, Object>> allBookmakerOdds = new ArrayList<>();
                
                for (Map<String, Object> bookmaker : bookmakers) {
                    String bookmakerName = (String) bookmaker.get("title");
                    List<Map<String, Object>> markets = (List<Map<String, Object>>) bookmaker.get("markets");
                    
                    Map<String, Object> bookmakerOdds = new HashMap<>();
                    bookmakerOdds.put("bookmaker", bookmakerName);
                    
                    for (Map<String, Object> market : markets) {
                        String marketKey = (String) market.get("key");
                        List<Map<String, Object>> outcomes = (List<Map<String, Object>>) market.get("outcomes");
                        
                        if ("h2h".equals(marketKey)) {
                            // Moneyline
                            for (Map<String, Object> outcome : outcomes) {
                                String team = (String) outcome.get("name");
                                Integer price = (Integer) outcome.get("price");
                                
                                if (team.equals(game.get("home_team"))) {
                                    bookmakerOdds.put("homeML", price);
                                } else {
                                    bookmakerOdds.put("awayML", price);
                                }
                            }
                        } else if ("spreads".equals(marketKey)) {
                            // Spread
                            for (Map<String, Object> outcome : outcomes) {
                                String team = (String) outcome.get("name");
                                Number point = (Number) outcome.get("point");
                                Integer price = (Integer) outcome.get("price");
                                
                                if (team.equals(game.get("home_team"))) {
                                    bookmakerOdds.put("homeSpread", point.doubleValue());
                                    bookmakerOdds.put("homeSpreadOdds", price);
                                } else {
                                    bookmakerOdds.put("awaySpread", point.doubleValue());
                                    bookmakerOdds.put("awaySpreadOdds", price);
                                }
                            }
                        } else if ("totals".equals(marketKey)) {
                            // Totals
                            for (Map<String, Object> outcome : outcomes) {
                                String name = (String) outcome.get("name");
                                Number point = (Number) outcome.get("point");
                                Integer price = (Integer) outcome.get("price");
                                
                                if ("Over".equals(name)) {
                                    bookmakerOdds.put("total", point.doubleValue());
                                    bookmakerOdds.put("overOdds", price);
                                } else {
                                    bookmakerOdds.put("underOdds", price);
                                }
                            }
                        }
                    }
                    
                    allBookmakerOdds.add(bookmakerOdds);
                }
                
                parsedGame.put("bookmakers", allBookmakerOdds);
                
                // Find best odds
                parsedGame.put("bestOdds", findBestOdds(allBookmakerOdds));
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing game: " + e.getMessage());
            e.printStackTrace();
        }
        
        return parsedGame;
    }
    
    private Map<String, Object> findBestOdds(List<Map<String, Object>> allBookmakerOdds) {
        Map<String, Object> bestOdds = new HashMap<>();
        
        int bestHomeML = Integer.MIN_VALUE;
        int bestAwayML = Integer.MIN_VALUE;
        int bestHomeSpread = Integer.MIN_VALUE;
        int bestAwaySpread = Integer.MIN_VALUE;
        int bestOver = Integer.MIN_VALUE;
        int bestUnder = Integer.MIN_VALUE;
        
        String bestHomeMLBook = "";
        String bestAwayMLBook = "";
        String bestHomeSpreadBook = "";
        String bestAwaySpreadBook = "";
        String bestOverBook = "";
        String bestUnderBook = "";
        
        for (Map<String, Object> bookmaker : allBookmakerOdds) {
            String bookmakerName = (String) bookmaker.get("bookmaker");
            
            if (bookmaker.containsKey("homeML")) {
                int homeML = (Integer) bookmaker.get("homeML");
                if (homeML > bestHomeML) {
                    bestHomeML = homeML;
                    bestHomeMLBook = bookmakerName;
                }
            }
            
            if (bookmaker.containsKey("awayML")) {
                int awayML = (Integer) bookmaker.get("awayML");
                if (awayML > bestAwayML) {
                    bestAwayML = awayML;
                    bestAwayMLBook = bookmakerName;
                }
            }
            
            if (bookmaker.containsKey("homeSpreadOdds")) {
                int homeSpread = (Integer) bookmaker.get("homeSpreadOdds");
                if (homeSpread > bestHomeSpread) {
                    bestHomeSpread = homeSpread;
                    bestHomeSpreadBook = bookmakerName;
                }
            }
            
            if (bookmaker.containsKey("awaySpreadOdds")) {
                int awaySpread = (Integer) bookmaker.get("awaySpreadOdds");
                if (awaySpread > bestAwaySpread) {
                    bestAwaySpread = awaySpread;
                    bestAwaySpreadBook = bookmakerName;
                }
            }
            
            if (bookmaker.containsKey("overOdds")) {
                int over = (Integer) bookmaker.get("overOdds");
                if (over > bestOver) {
                    bestOver = over;
                    bestOverBook = bookmakerName;
                }
            }
            
            if (bookmaker.containsKey("underOdds")) {
                int under = (Integer) bookmaker.get("underOdds");
                if (under > bestUnder) {
                    bestUnder = under;
                    bestUnderBook = bookmakerName;
                }
            }
        }
        
        if (bestHomeML != Integer.MIN_VALUE) {
            bestOdds.put("homeML", bestHomeML);
            bestOdds.put("homeMLBook", bestHomeMLBook);
        }
        
        if (bestAwayML != Integer.MIN_VALUE) {
            bestOdds.put("awayML", bestAwayML);
            bestOdds.put("awayMLBook", bestAwayMLBook);
        }
        
        if (bestHomeSpread != Integer.MIN_VALUE) {
            bestOdds.put("homeSpreadOdds", bestHomeSpread);
            bestOdds.put("homeSpreadBook", bestHomeSpreadBook);
        }
        
        if (bestAwaySpread != Integer.MIN_VALUE) {
            bestOdds.put("awaySpreadOdds", bestAwaySpread);
            bestOdds.put("awaySpreadBook", bestAwaySpreadBook);
        }
        
        if (bestOver != Integer.MIN_VALUE) {
            bestOdds.put("overOdds", bestOver);
            bestOdds.put("overBook", bestOverBook);
        }
        
        if (bestUnder != Integer.MIN_VALUE) {
            bestOdds.put("underOdds", bestUnder);
            bestOdds.put("underBook", bestUnderBook);
        }
        
        return bestOdds;
    }
    
    private Map<String, Object> findBestOddsForBetType(Map<String, Object> game, String betType) {
        Map<String, Object> bestOdds = (Map<String, Object>) game.get("bestOdds");
        Map<String, Object> result = new HashMap<>();
        
        switch (betType.toUpperCase()) {
            case "MONEYLINE":
            case "ML":
                result.put("homeML", bestOdds.get("homeML"));
                result.put("homeMLBook", bestOdds.get("homeMLBook"));
                result.put("awayML", bestOdds.get("awayML"));
                result.put("awayMLBook", bestOdds.get("awayMLBook"));
                break;
            case "SPREAD":
                result.put("homeSpread", bestOdds.get("homeSpreadOdds"));
                result.put("homeSpreadBook", bestOdds.get("homeSpreadBook"));
                result.put("awaySpread", bestOdds.get("awaySpreadOdds"));
                result.put("awaySpreadBook", bestOdds.get("awaySpreadBook"));
                break;
            case "TOTAL":
            case "OVER/UNDER":
                result.put("over", bestOdds.get("overOdds"));
                result.put("overBook", bestOdds.get("overBook"));
                result.put("under", bestOdds.get("underOdds"));
                result.put("underBook", bestOdds.get("underBook"));
                break;
        }
        
        return result;
    }
    
    private boolean matchesTeams(String team1, String team2) {
        return team1.toLowerCase().contains(team2.toLowerCase()) || 
               team2.toLowerCase().contains(team1.toLowerCase());
    }
}
