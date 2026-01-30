package com.coltwarren.sports_betting_analytics.model.betting;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Public Betting Data Model
 *
 * Represents public betting percentages for a game.
 * Used to identify contrarian betting opportunities when
 * the public is heavily on one side (65%+).
 */
@Data
public class PublicBettingData {
    private String gameId;
    private String sport;
    
    // Spread betting
    private Integer spreadHomePercent;      // e.g., 35 = 35% on home
    private Integer spreadAwayPercent;      // e.g., 65 = 65% on away
    
    // Moneyline betting
    private Integer moneylineHomePercent;
    private Integer moneylineAwayPercent;
    
    // Totals betting
    private Integer overPercent;
    private Integer underPercent;
    
    // Ticket vs Money split (sharp indicator)
    private Integer ticketPercent;          // % of tickets
    private Integer moneyPercent;           // % of money (if different, sharps involved)
    
    private LocalDateTime lastUpdated;
    private String source;
    
    /**
     * Get the dominant spread side percentage
     */
    public int getMaxSpreadPercent() {
        return Math.max(
            spreadHomePercent != null ? spreadHomePercent : 0,
            spreadAwayPercent != null ? spreadAwayPercent : 0
        );
    }
    
    /**
     * Check if there's significant public lean (65%+)
     */
    public boolean hasSignificantLean() {
        return getMaxSpreadPercent() >= 65;
    }
    
    /**
     * Get the sharp money divergence (difference between ticket % and money %)
     */
    public int getSharpDivergence() {
        if (ticketPercent == null || moneyPercent == null) return 0;
        return Math.abs(ticketPercent - moneyPercent);
    }
}
