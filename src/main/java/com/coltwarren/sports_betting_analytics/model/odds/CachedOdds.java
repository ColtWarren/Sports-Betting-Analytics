package com.coltwarren.sports_betting_analytics.model.odds;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cached_odds", indexes = {
    @Index(name = "idx_event", columnList = "sport, league, event_id", unique = true),
    @Index(name = "idx_sport_expires", columnList = "sport, expires_at"),
    @Index(name = "idx_commence_time", columnList = "commence_time")
})
public class CachedOdds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String sport;

    @Column(length = 100, nullable = false)
    private String league;

    @Column(name = "event_id", length = 100, nullable = false)
    private String eventId;

    @Column(name = "home_team", length = 100)
    private String homeTeam;

    @Column(name = "away_team", length = 100)
    private String awayTeam;

    @Column(name = "commence_time")
    private LocalDateTime commenceTime;

    @Column(name = "odds_data", columnDefinition = "TEXT", nullable = false)
    private String oddsData;

    @Column(name = "bookmakers_count")
    private Integer bookmakersCount;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (this.fetchedAt == null) {
            this.fetchedAt = LocalDateTime.now();
        }
    }

    // Constructors
    public CachedOdds() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }

    public String getLeague() { return league; }
    public void setLeague(String league) { this.league = league; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getHomeTeam() { return homeTeam; }
    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }

    public String getAwayTeam() { return awayTeam; }
    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }

    public LocalDateTime getCommenceTime() { return commenceTime; }
    public void setCommenceTime(LocalDateTime commenceTime) { this.commenceTime = commenceTime; }

    public String getOddsData() { return oddsData; }
    public void setOddsData(String oddsData) { this.oddsData = oddsData; }

    public Integer getBookmakersCount() { return bookmakersCount; }
    public void setBookmakersCount(Integer bookmakersCount) { this.bookmakersCount = bookmakersCount; }

    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime fetchedAt) { this.fetchedAt = fetchedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
