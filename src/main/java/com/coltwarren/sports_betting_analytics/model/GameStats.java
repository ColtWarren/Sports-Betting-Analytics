package com.coltwarren.sports_betting_analytics.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_stats", indexes = {
    @Index(name = "idx_gamestats_sport", columnList = "sport"),
    @Index(name = "idx_gamestats_home_team", columnList = "homeTeam"),
    @Index(name = "idx_gamestats_away_team", columnList = "awayTeam"),
    @Index(name = "idx_gamestats_game_time", columnList = "gameTime"),
    @Index(name = "idx_gamestats_sport_game_time", columnList = "sport, gameTime"),
    @Index(name = "idx_gamestats_home_team_game_time", columnList = "homeTeam, gameTime"),
    @Index(name = "idx_gamestats_away_team_game_time", columnList = "awayTeam, gameTime")
})
public class GameStats {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String sport;
    
    @Column(nullable = false)
    private String homeTeam;
    
    @Column(nullable = false)
    private String awayTeam;
    
    @Column(nullable = false)
    private LocalDateTime gameTime;
    
    private Integer homeScore;
    private Integer awayScore;
    
    private Double openingSpread;
    private Double closingSpread;
    
    private Double openingTotal;
    private Double closingTotal;
    
    private String winner; // "HOME", "AWAY", or "PUSH"
    
    private Boolean homeCoveredSpread;
    private Boolean awayCoveredSpread;
    
    private Boolean wentOver;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Constructors
    public GameStats() {
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }
    
    public String getHomeTeam() { return homeTeam; }
    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }
    
    public String getAwayTeam() { return awayTeam; }
    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }
    
    public LocalDateTime getGameTime() { return gameTime; }
    public void setGameTime(LocalDateTime gameTime) { this.gameTime = gameTime; }
    
    public Integer getHomeScore() { return homeScore; }
    public void setHomeScore(Integer homeScore) { 
        this.homeScore = homeScore;
        calculateResults();
    }
    
    public Integer getAwayScore() { return awayScore; }
    public void setAwayScore(Integer awayScore) { 
        this.awayScore = awayScore;
        calculateResults();
    }
    
    public Double getOpeningSpread() { return openingSpread; }
    public void setOpeningSpread(Double openingSpread) { this.openingSpread = openingSpread; }
    
    public Double getClosingSpread() { return closingSpread; }
    public void setClosingSpread(Double closingSpread) { this.closingSpread = closingSpread; }
    
    public Double getOpeningTotal() { return openingTotal; }
    public void setOpeningTotal(Double openingTotal) { this.openingTotal = openingTotal; }
    
    public Double getClosingTotal() { return closingTotal; }
    public void setClosingTotal(Double closingTotal) { this.closingTotal = closingTotal; }
    
    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }
    
    public Boolean getHomeCoveredSpread() { return homeCoveredSpread; }
    public void setHomeCoveredSpread(Boolean homeCoveredSpread) { this.homeCoveredSpread = homeCoveredSpread; }
    
    public Boolean getAwayCoveredSpread() { return awayCoveredSpread; }
    public void setAwayCoveredSpread(Boolean awayCoveredSpread) { this.awayCoveredSpread = awayCoveredSpread; }
    
    public Boolean getWentOver() { return wentOver; }
    public void setWentOver(Boolean wentOver) { this.wentOver = wentOver; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Auto-calculate results when scores are set
    private void calculateResults() {
        if (homeScore != null && awayScore != null) {
            // Determine winner
            if (homeScore > awayScore) {
                winner = "HOME";
            } else if (awayScore > homeScore) {
                winner = "AWAY";
            } else {
                winner = "PUSH";
            }
            
            // Calculate ATS (if closing spread exists)
            if (closingSpread != null) {
                double homeSpreadResult = homeScore + closingSpread - awayScore;
                if (homeSpreadResult > 0) {
                    homeCoveredSpread = true;
                    awayCoveredSpread = false;
                } else if (homeSpreadResult < 0) {
                    homeCoveredSpread = false;
                    awayCoveredSpread = true;
                } else {
                    homeCoveredSpread = null; // Push
                    awayCoveredSpread = null;
                }
            }
            
            // Calculate over/under (if closing total exists)
            if (closingTotal != null) {
                int totalPoints = homeScore + awayScore;
                if (totalPoints > closingTotal) {
                    wentOver = true;
                } else if (totalPoints < closingTotal) {
                    wentOver = false;
                } else {
                    wentOver = null; // Push
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameStats that = (GameStats) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
