package com.coltwarren.sports_betting_analytics.model.soccer;

/**
 * Team Standing Model - Represents a team's position in league standings
 *
 * Used for display on the soccer page and for AI analysis of matchups.
 */
public class TeamStanding {

    private Integer position;
    private String teamName;
    private String teamLogo;
    private Integer played;
    private Integer won;
    private Integer drawn;
    private Integer lost;
    private Integer goalsFor;
    private Integer goalsAgainst;
    private Integer goalDifference;
    private Integer points;
    private String form; // Last 5 results: W-D-L-W-W
    private String league;

    // Constructor
    public TeamStanding() {}

    public TeamStanding(String teamName, Integer position, Integer points) {
        this.teamName = teamName;
        this.position = position;
        this.points = points;
    }

    // Calculate points per game
    public double getPointsPerGame() {
        if (played == null || played == 0) return 0.0;
        return (double) points / played;
    }

    // Calculate goal difference
    public void calculateGoalDifference() {
        if (goalsFor != null && goalsAgainst != null) {
            this.goalDifference = goalsFor - goalsAgainst;
        }
    }

    // Format goal difference for display
    public String getGoalDifferenceFormatted() {
        if (goalDifference == null) return "0";
        if (goalDifference > 0) return "+" + goalDifference;
        return String.valueOf(goalDifference);
    }

    // Getters and Setters
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getTeamLogo() { return teamLogo; }
    public void setTeamLogo(String teamLogo) { this.teamLogo = teamLogo; }

    public Integer getPlayed() { return played; }
    public void setPlayed(Integer played) { this.played = played; }

    public Integer getWon() { return won; }
    public void setWon(Integer won) { this.won = won; }

    public Integer getDrawn() { return drawn; }
    public void setDrawn(Integer drawn) { this.drawn = drawn; }

    public Integer getLost() { return lost; }
    public void setLost(Integer lost) { this.lost = lost; }

    public Integer getGoalsFor() { return goalsFor; }
    public void setGoalsFor(Integer goalsFor) {
        this.goalsFor = goalsFor;
        calculateGoalDifference();
    }

    public Integer getGoalsAgainst() { return goalsAgainst; }
    public void setGoalsAgainst(Integer goalsAgainst) {
        this.goalsAgainst = goalsAgainst;
        calculateGoalDifference();
    }

    public Integer getGoalDifference() { return goalDifference; }
    public void setGoalDifference(Integer goalDifference) { this.goalDifference = goalDifference; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public String getForm() { return form; }
    public void setForm(String form) { this.form = form; }

    public String getLeague() { return league; }
    public void setLeague(String league) { this.league = league; }

    @Override
    public String toString() {
        return position + ". " + teamName + " - " + points + " pts";
    }
}
