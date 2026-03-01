package com.coltwarren.sports_betting_analytics.model.odds;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "odds_fetch_log", indexes = {
    @Index(name = "idx_created", columnList = "created_at"),
    @Index(name = "idx_status", columnList = "status, created_at")
})
public class OddsFetchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String sport;

    @Column(length = 100)
    private String league;

    @Column(length = 20, nullable = false)
    private String status; // SUCCESS, FAILED, RATE_LIMITED

    @Column(name = "events_fetched")
    private Integer eventsFetched = 0;

    @Column(name = "api_credits_used")
    private Integer apiCreditsUsed = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Constructors
    public OddsFetchLog() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }

    public String getLeague() { return league; }
    public void setLeague(String league) { this.league = league; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getEventsFetched() { return eventsFetched; }
    public void setEventsFetched(Integer eventsFetched) { this.eventsFetched = eventsFetched; }

    public Integer getApiCreditsUsed() { return apiCreditsUsed; }
    public void setApiCreditsUsed(Integer apiCreditsUsed) { this.apiCreditsUsed = apiCreditsUsed; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OddsFetchLog that = (OddsFetchLog) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
