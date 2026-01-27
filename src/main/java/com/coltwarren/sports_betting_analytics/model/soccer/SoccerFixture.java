package com.coltwarren.sports_betting_analytics.model.soccer;

import java.time.LocalDateTime;

/**
 * Soccer Fixture Model - Represents a soccer match with stats and odds
 *
 * SOCCER 3-WAY BETTING: Unlike US sports (2-way), soccer has 3 outcomes:
 * - Home Win
 * - Draw (typically ~25-30% probability)
 * - Away Win
 *
 * Missouri Local Teams:
 * - Sporting Kansas City (MLS)
 * - St. Louis City SC (MLS)
 */
public class SoccerFixture {

    // Basic match info
    private Long fixtureId;
    private String league;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime kickoffTime;
    private String venue;
    private String status; // NS (Not Started), LIVE, FT (Full Time), etc.

    // Live score (if game in progress)
    private Integer homeScore;
    private Integer awayScore;

    // Team form (last 5 matches: W-D-L-W-W format)
    private String homeTeamForm;
    private String awayTeamForm;

    // League standings
    private Integer homeTeamPosition;
    private Integer awayTeamPosition;

    // 3-Way Odds (Home/Draw/Away)
    private Double homeWinOdds;
    private Double drawOdds;
    private Double awayWinOdds;

    // Best books for each outcome
    private String bestHomeBook;
    private String bestDrawBook;
    private String bestAwayBook;

    // Over/Under Goals
    private Double overOdds;
    private Double underOdds;
    private Double goalLine; // e.g., 2.5
    private String bestOverBook;
    private String bestUnderBook;

    // Both Teams To Score (BTTS)
    private Double bttsYesOdds;
    private Double bttsNoOdds;
    private String bestBttsYesBook;
    private String bestBttsNoBook;

    // Local team indicator (Missouri teams)
    private boolean isLocalTeam;
    private String localTeamName; // Which team is local (if any)

    // Team logos
    private String homeTeamLogo;
    private String awayTeamLogo;

    // Constructor
    public SoccerFixture() {
        this.isLocalTeam = false;
    }

    // Missouri local team check
    public void checkLocalTeam() {
        String[] missouriTeams = {
            "Sporting KC", "Sporting Kansas City", "Kansas City",
            "St. Louis City SC", "St. Louis City", "St. Louis SC", "STL City"
        };

        for (String team : missouriTeams) {
            if (homeTeam != null && homeTeam.toLowerCase().contains(team.toLowerCase())) {
                this.isLocalTeam = true;
                this.localTeamName = homeTeam;
                return;
            }
            if (awayTeam != null && awayTeam.toLowerCase().contains(team.toLowerCase())) {
                this.isLocalTeam = true;
                this.localTeamName = awayTeam;
                return;
            }
        }
        this.isLocalTeam = false;
        this.localTeamName = null;
    }

    // Helper to format odds display
    public String formatOdds(Double odds) {
        if (odds == null) return "N/A";
        if (odds >= 2.0) {
            // Convert decimal to American (positive)
            int american = (int) Math.round((odds - 1) * 100);
            return "+" + american;
        } else {
            // Convert decimal to American (negative)
            int american = (int) Math.round(-100 / (odds - 1));
            return String.valueOf(american);
        }
    }

    // Getters and Setters
    public Long getFixtureId() { return fixtureId; }
    public void setFixtureId(Long fixtureId) { this.fixtureId = fixtureId; }

    public String getLeague() { return league; }
    public void setLeague(String league) { this.league = league; }

    public String getHomeTeam() { return homeTeam; }
    public void setHomeTeam(String homeTeam) {
        this.homeTeam = homeTeam;
        checkLocalTeam();
    }

    public String getAwayTeam() { return awayTeam; }
    public void setAwayTeam(String awayTeam) {
        this.awayTeam = awayTeam;
        checkLocalTeam();
    }

    public LocalDateTime getKickoffTime() { return kickoffTime; }
    public void setKickoffTime(LocalDateTime kickoffTime) { this.kickoffTime = kickoffTime; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getHomeScore() { return homeScore; }
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }

    public Integer getAwayScore() { return awayScore; }
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }

    public String getHomeTeamForm() { return homeTeamForm; }
    public void setHomeTeamForm(String homeTeamForm) { this.homeTeamForm = homeTeamForm; }

    public String getAwayTeamForm() { return awayTeamForm; }
    public void setAwayTeamForm(String awayTeamForm) { this.awayTeamForm = awayTeamForm; }

    public Integer getHomeTeamPosition() { return homeTeamPosition; }
    public void setHomeTeamPosition(Integer homeTeamPosition) { this.homeTeamPosition = homeTeamPosition; }

    public Integer getAwayTeamPosition() { return awayTeamPosition; }
    public void setAwayTeamPosition(Integer awayTeamPosition) { this.awayTeamPosition = awayTeamPosition; }

    public Double getHomeWinOdds() { return homeWinOdds; }
    public void setHomeWinOdds(Double homeWinOdds) { this.homeWinOdds = homeWinOdds; }

    public Double getDrawOdds() { return drawOdds; }
    public void setDrawOdds(Double drawOdds) { this.drawOdds = drawOdds; }

    public Double getAwayWinOdds() { return awayWinOdds; }
    public void setAwayWinOdds(Double awayWinOdds) { this.awayWinOdds = awayWinOdds; }

    public String getBestHomeBook() { return bestHomeBook; }
    public void setBestHomeBook(String bestHomeBook) { this.bestHomeBook = bestHomeBook; }

    public String getBestDrawBook() { return bestDrawBook; }
    public void setBestDrawBook(String bestDrawBook) { this.bestDrawBook = bestDrawBook; }

    public String getBestAwayBook() { return bestAwayBook; }
    public void setBestAwayBook(String bestAwayBook) { this.bestAwayBook = bestAwayBook; }

    public Double getOverOdds() { return overOdds; }
    public void setOverOdds(Double overOdds) { this.overOdds = overOdds; }

    public Double getUnderOdds() { return underOdds; }
    public void setUnderOdds(Double underOdds) { this.underOdds = underOdds; }

    public Double getGoalLine() { return goalLine; }
    public void setGoalLine(Double goalLine) { this.goalLine = goalLine; }

    public String getBestOverBook() { return bestOverBook; }
    public void setBestOverBook(String bestOverBook) { this.bestOverBook = bestOverBook; }

    public String getBestUnderBook() { return bestUnderBook; }
    public void setBestUnderBook(String bestUnderBook) { this.bestUnderBook = bestUnderBook; }

    public Double getBttsYesOdds() { return bttsYesOdds; }
    public void setBttsYesOdds(Double bttsYesOdds) { this.bttsYesOdds = bttsYesOdds; }

    public Double getBttsNoOdds() { return bttsNoOdds; }
    public void setBttsNoOdds(Double bttsNoOdds) { this.bttsNoOdds = bttsNoOdds; }

    public String getBestBttsYesBook() { return bestBttsYesBook; }
    public void setBestBttsYesBook(String bestBttsYesBook) { this.bestBttsYesBook = bestBttsYesBook; }

    public String getBestBttsNoBook() { return bestBttsNoBook; }
    public void setBestBttsNoBook(String bestBttsNoBook) { this.bestBttsNoBook = bestBttsNoBook; }

    public boolean isLocalTeam() { return isLocalTeam; }
    public void setLocalTeam(boolean localTeam) { isLocalTeam = localTeam; }

    public String getLocalTeamName() { return localTeamName; }
    public void setLocalTeamName(String localTeamName) { this.localTeamName = localTeamName; }

    public String getHomeTeamLogo() { return homeTeamLogo; }
    public void setHomeTeamLogo(String homeTeamLogo) { this.homeTeamLogo = homeTeamLogo; }

    public String getAwayTeamLogo() { return awayTeamLogo; }
    public void setAwayTeamLogo(String awayTeamLogo) { this.awayTeamLogo = awayTeamLogo; }

    @Override
    public String toString() {
        return "SoccerFixture{" +
                "league='" + league + '\'' +
                ", homeTeam='" + homeTeam + '\'' +
                ", awayTeam='" + awayTeam + '\'' +
                ", kickoffTime=" + kickoffTime +
                ", isLocalTeam=" + isLocalTeam +
                '}';
    }
}
