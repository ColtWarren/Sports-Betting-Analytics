package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.Bet;
import com.coltwarren.sports_betting_analytics.model.BetStatus;
import com.coltwarren.sports_betting_analytics.model.BetType;
import com.coltwarren.sports_betting_analytics.model.Sport;
import com.coltwarren.sports_betting_analytics.repository.BetRepository;
import com.coltwarren.sports_betting_analytics.service.odds.MultiSportOddsService;
import com.coltwarren.sports_betting_analytics.util.TeamNameUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class ActiveBetsTrackerService {
    
    private final BetRepository betRepository;

    private final LiveGameService liveGameService;

    private final MultiSportOddsService multiSportOddsService;

    public ActiveBetsTrackerService(BetRepository betRepository,
                                    LiveGameService liveGameService,
                                    MultiSportOddsService multiSportOddsService) {
        this.betRepository = betRepository;
        this.liveGameService = liveGameService;
        this.multiSportOddsService = multiSportOddsService;
    }

    /**
     * Returns active bets separated into live and upcoming categories.
     */
    public Map<String, Object> getActiveBetsSeparated() {
        List<Map<String, Object>> activeBets = getActiveBetsWithLiveData();

        List<Map<String, Object>> liveBets = new ArrayList<>();
        List<Map<String, Object>> upcomingBets = new ArrayList<>();

        for (Map<String, Object> bet : activeBets) {
            Boolean isLive = (Boolean) bet.get("isLive");
            if (isLive != null && isLive) {
                liveBets.add(bet);
            } else {
                upcomingBets.add(bet);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalActiveBets", activeBets.size());
        result.put("liveBets", liveBets);
        result.put("liveCount", liveBets.size());
        result.put("upcomingBets", upcomingBets);
        result.put("upcomingCount", upcomingBets.size());
        return result;
    }

    public List<Map<String, Object>> getActiveBetsWithLiveData() {
        try {
            // Get all pending bets
            List<Bet> pendingBets = betRepository.findByStatus(BetStatus.PENDING);
            List<Map<String, Object>> activeBetsData = new ArrayList<>();
            
            for (Bet bet : pendingBets) {
                Map<String, Object> betData = new HashMap<>();
                
                // Basic bet info
                betData.put("betId", bet.getId());
                betData.put("sport", bet.getSport());
                betData.put("eventName", bet.getEventName());
                betData.put("selection", bet.getSelection());
                betData.put("betType", bet.getBetType());
                betData.put("odds", bet.getOdds());
                betData.put("stake", bet.getStake());
                betData.put("potentialPayout", bet.getPotentialPayout());
                betData.put("eventStartTime", bet.getEventStartTime());
                
                // Get live game data
                Map<String, Object> liveGameData = findLiveGame(bet);
                if (liveGameData != null) {
                    betData.put("liveGame", liveGameData);
                    betData.put("isLive", true);
                    
                    // Calculate current position
                    Map<String, Object> position = calculatePosition(bet, liveGameData);
                    betData.put("position", position);
                } else {
                    betData.put("isLive", false);
                }
                
                // Get current odds for hedge opportunities
                Map<String, Object> currentOdds = getCurrentOdds(bet);
                if (currentOdds != null && !currentOdds.isEmpty()) {
                    betData.put("currentOdds", currentOdds);
                    
                    // Calculate hedge opportunity
                    Map<String, Object> hedgeOpportunity = calculateHedgeOpportunity(bet, currentOdds);
                    betData.put("hedgeOpportunity", hedgeOpportunity);
                }
                
                activeBetsData.add(betData);
            }
            
            return activeBetsData;
            
        } catch (Exception e) {
            log.error("Error getting active bets: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    private Map<String, Object> findLiveGame(Bet bet) {
        try {
            Sport sport = bet.getSport();
            List<Map<String, Object>> liveGames = liveGameService.getLiveGames(sport.name());
            
            String eventName = bet.getEventName();
            
            for (Map<String, Object> game : liveGames) {
                String homeTeam = (String) game.get("homeTeam");
                String awayTeam = (String) game.get("awayTeam");
                
                // Check if bet matches this game
                if (eventName.contains(homeTeam) || eventName.contains(awayTeam) ||
                    eventName.contains(TeamNameUtils.extractTeamName(homeTeam)) || 
                    eventName.contains(TeamNameUtils.extractTeamName(awayTeam))) {
                    return game;
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Error finding live game: {}", e.getMessage(), e);
            return null;
        }
    }
    
    private Map<String, Object> calculatePosition(Bet bet, Map<String, Object> liveGame) {
        Map<String, Object> position = new HashMap<>();
        
        try {
            BetType betType = bet.getBetType();
            Integer homeScore = (Integer) liveGame.get("homeScore");
            Integer awayScore = (Integer) liveGame.get("awayScore");

            if (homeScore == null || awayScore == null) {
                position.put("status", "UNKNOWN");
                return position;
            }

            String selection = bet.getSelection();

            if (betType == BetType.SPREAD) {
                // Extract spread from selection (e.g. "Bills -3.5")
                double spread = extractSpread(selection);
                boolean isHome = isHomeTeam(selection, liveGame);
                
                int actualMargin = isHome ? (homeScore - awayScore) : (awayScore - homeScore);
                double coverageMargin = actualMargin + spread;
                
                if (coverageMargin > 0) {
                    position.put("status", "WINNING");
                    position.put("margin", coverageMargin);
                } else if (coverageMargin < 0) {
                    position.put("status", "LOSING");
                    position.put("margin", Math.abs(coverageMargin));
                } else {
                    position.put("status", "PUSH");
                    position.put("margin", 0.0);
                }
                
            } else if (betType == BetType.TOTAL_OVER || betType == BetType.TOTAL_UNDER || betType == BetType.OVER_GOALS || betType == BetType.UNDER_GOALS) {
                // Extract total from selection (e.g. "Over 51.5")
                double total = extractTotal(selection);
                int actualTotal = homeScore + awayScore;
                boolean isOver = selection.toLowerCase().contains("over");
                
                double margin = actualTotal - total;
                
                if (isOver) {
                    if (margin > 0) {
                        position.put("status", "WINNING");
                        position.put("margin", margin);
                    } else {
                        position.put("status", "LOSING");
                        position.put("margin", Math.abs(margin));
                    }
                } else {
                    if (margin < 0) {
                        position.put("status", "WINNING");
                        position.put("margin", Math.abs(margin));
                    } else {
                        position.put("status", "LOSING");
                        position.put("margin", margin);
                    }
                }
                
            } else if (betType == BetType.MONEYLINE) {
                boolean isHome = isHomeTeam(selection, liveGame);
                
                if (isHome) {
                    if (homeScore > awayScore) {
                        position.put("status", "WINNING");
                        position.put("margin", homeScore - awayScore);
                    } else if (homeScore < awayScore) {
                        position.put("status", "LOSING");
                        position.put("margin", awayScore - homeScore);
                    } else {
                        position.put("status", "TIED");
                        position.put("margin", 0);
                    }
                } else {
                    if (awayScore > homeScore) {
                        position.put("status", "WINNING");
                        position.put("margin", awayScore - homeScore);
                    } else if (awayScore < homeScore) {
                        position.put("status", "LOSING");
                        position.put("margin", homeScore - awayScore);
                    } else {
                        position.put("status", "TIED");
                        position.put("margin", 0);
                    }
                }
            }
            
            position.put("currentScore", homeScore + "-" + awayScore);
            
        } catch (Exception e) {
            log.error("Error calculating position: {}", e.getMessage(), e);
            position.put("status", "ERROR");
        }
        
        return position;
    }
    
    private Map<String, Object> getCurrentOdds(Bet bet) {
        try {
            Sport sport = bet.getSport();
            String eventName = bet.getEventName();

            // Parse team names from event
            String[] teams = parseTeamNames(eventName);
            if (teams.length != 2) {
                return Collections.emptyMap();
            }

            String awayTeam = teams[0];
            String homeTeam = teams[1];
            BetType betType = bet.getBetType();

            return multiSportOddsService.getBestOddsForGame(sport.name(), homeTeam, awayTeam, betType.name());
            
        } catch (Exception e) {
            log.error("Error getting current odds: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    private Map<String, Object> calculateHedgeOpportunity(Bet bet, Map<String, Object> currentOdds) {
        Map<String, Object> hedge = new HashMap<>();
        
        try {
            // Check if there's value in hedging
            BigDecimal originalOdds = bet.getOdds();
            BigDecimal stake = bet.getStake();
            BigDecimal potentialPayout = bet.getPotentialPayout();
            
            // For now, mark if hedge is available
            hedge.put("available", !currentOdds.isEmpty());
            hedge.put("recommendedStake", 0.0);
            hedge.put("guaranteedProfit", 0.0);
            
            // TODO: Implement full hedge calculation
            
        } catch (Exception e) {
            log.error("Error calculating hedge: {}", e.getMessage(), e);
        }
        
        return hedge;
    }
    
    private double extractSpread(String selection) {
        // Extract number from "Team -3.5" or "Team +3.5"
        try {
            String[] parts = selection.split(" ");
            for (String part : parts) {
                if (part.matches("[+-]?\\d+\\.?\\d*")) {
                    return Double.parseDouble(part);
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting spread: {}", e.getMessage());
        }
        return 0.0;
    }
    
    private double extractTotal(String selection) {
        // Extract number from "Over 51.5" or "Under 51.5"
        try {
            String[] parts = selection.split(" ");
            for (String part : parts) {
                if (part.matches("\\d+\\.?\\d*")) {
                    return Double.parseDouble(part);
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting total: {}", e.getMessage());
        }
        return 0.0;
    }
    
    private boolean isHomeTeam(String selection, Map<String, Object> game) {
        String homeTeam = (String) game.get("homeTeam");
        return selection.contains(homeTeam) || selection.contains(TeamNameUtils.extractTeamName(homeTeam));
    }
    
    private String[] parseTeamNames(String eventName) {
        // Parse "Team1 @ Team2" or "Team1 vs Team2"
        if (eventName.contains(" @ ")) {
            return eventName.split(" @ ");
        } else if (eventName.contains(" vs ")) {
            return eventName.split(" vs ");
        } else if (eventName.contains(" v ")) {
            return eventName.split(" v ");
        }
        return new String[0];
    }
}
