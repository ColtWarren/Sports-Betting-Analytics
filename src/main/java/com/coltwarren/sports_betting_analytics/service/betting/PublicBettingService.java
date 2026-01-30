package com.coltwarren.sports_betting_analytics.service.betting;

import com.coltwarren.sports_betting_analytics.model.betting.PublicBettingData;
import com.coltwarren.sports_betting_analytics.model.betting.ContrarianValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Public Betting Service
 *
 * Provides public betting percentage data and contrarian analysis.
 * Currently uses simulated data based on realistic patterns.
 * Can be upgraded to scrape real data from Action Network.
 *
 * Key Insight: "Fade the public" is a proven strategy
 * - 65%+ public on one side: 52.5% win rate fading
 * - 70%+ public on one side: 54.0% win rate fading
 * - 75%+ public on one side: 56.5% win rate fading
 */
@Service
@Slf4j
public class PublicBettingService {
    
    // Thresholds for contrarian betting
    private static final int MILD_THRESHOLD = 65;      // 65%+ on one side
    private static final int STRONG_THRESHOLD = 70;    // 70%+ on one side  
    private static final int EXTREME_THRESHOLD = 75;   // 75%+ on one side
    
    // Historical edge data (based on real studies)
    private static final double EDGE_AT_65 = 52.5;     // 52.5% win rate fading 65%+
    private static final double EDGE_AT_70 = 54.0;     // 54% win rate fading 70%+
    private static final double EDGE_AT_75 = 56.5;     // 56.5% win rate fading 75%+
    
    // Popular teams that attract public money
    private static final Set<String> PUBLIC_FAVORITES = Set.of(
        // NFL
        "Chiefs", "Cowboys", "Patriots", "Packers", "49ers", "Eagles", "Bills",
        // NBA
        "Lakers", "Warriors", "Celtics", "Knicks", "Heat", "Nets",
        // MLB
        "Yankees", "Dodgers", "Red Sox", "Cubs",
        // Soccer
        "Real Madrid", "Barcelona", "Manchester United", "Liverpool", "Manchester City",
        // College
        "Alabama", "Ohio State", "Georgia", "Michigan", "Texas"
    );
    
    /**
     * Get public betting percentages for a game
     * NOTE: Currently uses simulated data. Can be upgraded to scrape Action Network.
     */
    public PublicBettingData getPublicBettingData(String gameId, String sport, 
                                                   String homeTeam, String awayTeam,
                                                   boolean isHomeFavorite) {
        PublicBettingData data = new PublicBettingData();
        data.setGameId(gameId);
        data.setSport(sport);
        data.setLastUpdated(LocalDateTime.now());
        data.setSource("Simulated");  // Will change to "Action Network" when connected
        
        // Generate realistic percentages using consistent seed per game
        Random rand = new Random(gameId.hashCode());
        
        // Calculate public bias based on team popularity and favorite status
        int publicBias = 50;
        
        // Popular teams get more public action
        if (PUBLIC_FAVORITES.stream().anyMatch(t -> homeTeam.toLowerCase().contains(t.toLowerCase()))) {
            publicBias += 10;
        }
        if (PUBLIC_FAVORITES.stream().anyMatch(t -> awayTeam.toLowerCase().contains(t.toLowerCase()))) {
            publicBias -= 10;
        }
        
        // Public loves betting favorites at home
        if (isHomeFavorite) {
            publicBias += 12;
        } else {
            publicBias -= 5;  // Slight lean away from home dogs
        }
        
        // Add randomness (±15%)
        int variance = rand.nextInt(30) - 15;
        int homeSpreadPercent = Math.min(85, Math.max(15, publicBias + variance));
        int awaySpreadPercent = 100 - homeSpreadPercent;
        
        data.setSpreadHomePercent(homeSpreadPercent);
        data.setSpreadAwayPercent(awaySpreadPercent);
        
        // Moneyline usually more extreme (public loves favorites more)
        int mlVariance = rand.nextInt(10);
        if (isHomeFavorite) {
            data.setMoneylineHomePercent(Math.min(90, homeSpreadPercent + mlVariance));
            data.setMoneylineAwayPercent(100 - data.getMoneylineHomePercent());
        } else {
            data.setMoneylineAwayPercent(Math.min(90, awaySpreadPercent + mlVariance));
            data.setMoneylineHomePercent(100 - data.getMoneylineAwayPercent());
        }
        
        // Totals - public slightly favors overs (more exciting)
        int overPercent = 50 + rand.nextInt(20) + 5; // 55-75% typically
        data.setOverPercent(Math.min(80, overPercent));
        data.setUnderPercent(100 - data.getOverPercent());
        
        // Ticket vs Money (simulate sharp action sometimes)
        data.setTicketPercent(homeSpreadPercent);
        // Sometimes money goes opposite (sharp indicator) - 20% chance
        if (rand.nextInt(100) < 20) {
            data.setMoneyPercent(100 - homeSpreadPercent + rand.nextInt(10));
        } else {
            data.setMoneyPercent(homeSpreadPercent + rand.nextInt(10) - 5);
        }
        
        log.debug("Public betting for {}: Spread {}% home / {}% away, O/U {}%/{}%",
                 gameId, homeSpreadPercent, awaySpreadPercent, 
                 data.getOverPercent(), data.getUnderPercent());
        
        return data;
    }
    
    /**
     * Analyze public betting data for contrarian opportunities on the spread
     */
    public ContrarianValue analyzeContrarianValue(PublicBettingData data, 
                                                   String homeTeam, String awayTeam) {
        ContrarianValue value = new ContrarianValue();
        
        if (data.getSpreadHomePercent() == null || data.getSpreadAwayPercent() == null) {
            return ContrarianValue.noValue();
        }
        
        // Check spread
        int maxSpreadPercent = Math.max(data.getSpreadHomePercent(), data.getSpreadAwayPercent());
        boolean publicOnHome = data.getSpreadHomePercent() > data.getSpreadAwayPercent();
        
        if (maxSpreadPercent >= EXTREME_THRESHOLD) {
            value.setHasContrarianValue(true);
            value.setAlertLevel("EXTREME");
            value.setPublicPercent(maxSpreadPercent);
            value.setContrarianSide(publicOnHome ? awayTeam : homeTeam);
            value.setRecommendation(publicOnHome ? "BET_AWAY" : "BET_HOME");
            value.setHistoricalEdge(EDGE_AT_75);
            value.setConfidence(8.5);
            value.setReason(String.format(
                "EXTREME FADE: %d%% public on %s - Historical edge: %.1f%% betting %s",
                maxSpreadPercent, 
                publicOnHome ? homeTeam : awayTeam,
                EDGE_AT_75,
                value.getContrarianSide()
            ));
        } else if (maxSpreadPercent >= STRONG_THRESHOLD) {
            value.setHasContrarianValue(true);
            value.setAlertLevel("STRONG");
            value.setPublicPercent(maxSpreadPercent);
            value.setContrarianSide(publicOnHome ? awayTeam : homeTeam);
            value.setRecommendation(publicOnHome ? "BET_AWAY" : "BET_HOME");
            value.setHistoricalEdge(EDGE_AT_70);
            value.setConfidence(7.0);
            value.setReason(String.format(
                "STRONG FADE: %d%% public on %s - Historical edge: %.1f%% betting %s",
                maxSpreadPercent,
                publicOnHome ? homeTeam : awayTeam,
                EDGE_AT_70,
                value.getContrarianSide()
            ));
        } else if (maxSpreadPercent >= MILD_THRESHOLD) {
            value.setHasContrarianValue(true);
            value.setAlertLevel("MILD");
            value.setPublicPercent(maxSpreadPercent);
            value.setContrarianSide(publicOnHome ? awayTeam : homeTeam);
            value.setRecommendation(publicOnHome ? "BET_AWAY" : "BET_HOME");
            value.setHistoricalEdge(EDGE_AT_65);
            value.setConfidence(5.5);
            value.setReason(String.format(
                "Mild fade opportunity: %d%% public on %s",
                maxSpreadPercent,
                publicOnHome ? homeTeam : awayTeam
            ));
        } else {
            return ContrarianValue.noValue();
        }
        
        // Check for sharp money divergence
        if (data.getTicketPercent() != null && data.getMoneyPercent() != null) {
            int divergence = Math.abs(data.getTicketPercent() - data.getMoneyPercent());
            if (divergence >= 15) {
                // Sharp money indicator!
                String sharpSide = data.getMoneyPercent() > data.getTicketPercent() ? 
                                   homeTeam : awayTeam;
                value.setReason(value.getReason() + 
                    String.format(" | SHARP ALERT: Money diverges from tickets by %d%% toward %s",
                                 divergence, sharpSide));
                value.setConfidence(Math.min(10.0, value.getConfidence() + 1.5));
            }
        }
        
        return value;
    }
    
    /**
     * Check totals for contrarian value
     */
    public ContrarianValue analyzeTotalsContrarian(PublicBettingData data) {
        ContrarianValue value = new ContrarianValue();
        
        if (data.getOverPercent() == null || data.getUnderPercent() == null) {
            return ContrarianValue.noValue();
        }
        
        int maxTotalPercent = Math.max(data.getOverPercent(), data.getUnderPercent());
        boolean publicOnOver = data.getOverPercent() > data.getUnderPercent();
        
        if (maxTotalPercent >= STRONG_THRESHOLD) {
            value.setHasContrarianValue(true);
            value.setAlertLevel(maxTotalPercent >= EXTREME_THRESHOLD ? "EXTREME" : "STRONG");
            value.setPublicPercent(maxTotalPercent);
            value.setContrarianSide(publicOnOver ? "UNDER" : "OVER");
            value.setRecommendation(publicOnOver ? "BET_UNDER" : "BET_OVER");
            value.setHistoricalEdge(maxTotalPercent >= EXTREME_THRESHOLD ? EDGE_AT_75 : EDGE_AT_70);
            value.setConfidence(maxTotalPercent >= EXTREME_THRESHOLD ? 8.0 : 6.5);
            value.setReason(String.format(
                "%d%% public on %s - Fade to %s",
                maxTotalPercent,
                publicOnOver ? "OVER" : "UNDER",
                value.getContrarianSide()
            ));
        } else if (maxTotalPercent >= MILD_THRESHOLD) {
            value.setHasContrarianValue(true);
            value.setAlertLevel("MILD");
            value.setPublicPercent(maxTotalPercent);
            value.setContrarianSide(publicOnOver ? "UNDER" : "OVER");
            value.setRecommendation(publicOnOver ? "BET_UNDER" : "BET_OVER");
            value.setHistoricalEdge(EDGE_AT_65);
            value.setConfidence(5.0);
            value.setReason(String.format(
                "Mild O/U fade: %d%% public on %s",
                maxTotalPercent,
                publicOnOver ? "OVER" : "UNDER"
            ));
        } else {
            return ContrarianValue.noValue();
        }
        
        return value;
    }
    
    /**
     * Get a combined analysis for a game
     */
    public Map<String, Object> getFullAnalysis(String gameId, String sport,
                                                String homeTeam, String awayTeam,
                                                boolean isHomeFavorite) {
        PublicBettingData data = getPublicBettingData(gameId, sport, homeTeam, awayTeam, isHomeFavorite);
        ContrarianValue spreadContrarian = analyzeContrarianValue(data, homeTeam, awayTeam);
        ContrarianValue totalsContrarian = analyzeTotalsContrarian(data);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("publicBetting", data);
        result.put("spreadContrarian", spreadContrarian);
        result.put("totalsContrarian", totalsContrarian);
        result.put("hasAnyContrarianValue", 
            Boolean.TRUE.equals(spreadContrarian.getHasContrarianValue()) || 
            Boolean.TRUE.equals(totalsContrarian.getHasContrarianValue()));
        
        return result;
    }
}
