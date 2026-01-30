package com.coltwarren.sports_betting_analytics.model.injury;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Player Injury Model
 *
 * Represents an individual player injury report from ESPN.
 * Tracks injury status, type, and player importance for betting impact calculation.
 */
@Data
public class PlayerInjury {
    private String playerId;
    private String playerName;
    private String team;
    private String teamId;
    private String position;         // QB, RB, WR, etc.
    private String injuryType;       // Knee, Hamstring, Concussion, etc.
    private String status;           // OUT, DOUBTFUL, QUESTIONABLE, PROBABLE
    private String details;          // "Did not practice Wednesday"
    private LocalDateTime lastUpdated;
    private String sport;

    // Player importance (calculated based on position)
    private Integer playerRating;    // 1-100 scale
    private Boolean isStarter;
    private Boolean isProBowler;     // or All-Star

    /**
     * Check if this injury is significant for betting purposes
     */
    public boolean isSignificant() {
        if (status == null) return false;
        String upper = status.toUpperCase();
        return (upper.equals("OUT") || upper.equals("DOUBTFUL") || upper.contains("OUT") || upper.equals("IR"))
               && playerRating != null && playerRating >= 70;
    }

    /**
     * Get display text for UI
     */
    public String getDisplayText() {
        return String.format("%s %s (%s) - %s",
            position != null ? position : "",
            playerName != null ? playerName : "Unknown",
            status != null ? status : "Unknown",
            injuryType != null ? injuryType : "");
    }
}
