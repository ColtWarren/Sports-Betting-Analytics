package com.coltwarren.sports_betting_analytics.model.college;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CFBGame {
    private Long gameId;
    private String season;
    private Integer week;
    private String seasonType;        // "regular", "postseason"

    private String homeTeam;
    private String awayTeam;
    private String homeConference;
    private String awayConference;

    private LocalDateTime startTime;
    private String venue;
    private String city;
    private String state;

    // Rankings at game time
    private Integer homeRank;
    private Integer awayRank;

    // Betting lines
    private Double spread;            // Positive = home underdog
    private Double overUnder;
    private Double homeMoneyline;
    private Double awayMoneyline;

    // Results (for historical)
    private Integer homeScore;
    private Integer awayScore;
    private Boolean completed;

    // Betting results
    private String atsResult;         // "HOME_COVER", "AWAY_COVER", "PUSH"
    private String ouResult;          // "OVER", "UNDER", "PUSH"

    // Analysis flags
    private Boolean isRankedMatchup;
    private Boolean isMidMajorGame;
    private Boolean isConferenceGame;
    private Boolean isRivalryGame;

    public void determineGameType() {
        this.isRankedMatchup = (homeRank != null && homeRank <= 25) ||
                               (awayRank != null && awayRank <= 25);
    }
}
