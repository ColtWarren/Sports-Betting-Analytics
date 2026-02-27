package com.coltwarren.sports_betting_analytics.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bankroll", indexes = {
    @Index(name = "idx_bankroll_user_id", columnList = "user_id"),
    @Index(name = "idx_bankroll_recorded_at", columnList = "recorded_at"),
    @Index(name = "idx_bankroll_user_recorded", columnList = "user_id, recorded_at")
})
public class Bankroll {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
    
    @Column(length = 500)
    private String notes;
    
    @Column(name = "transaction_type")
    private String transactionType; // DEPOSIT, WITHDRAWAL, PROFIT, LOSS
    
    @Column(name = "related_bet_id")
    private Long relatedBetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Bankroll() {
        this.recordedAt = LocalDateTime.now();
    }
    
    public Bankroll(BigDecimal amount, String transactionType) {
        this.amount = amount;
        this.transactionType = transactionType;
        this.recordedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
    
    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    
    public Long getRelatedBetId() {
        return relatedBetId;
    }
    
    public void setRelatedBetId(Long relatedBetId) {
        this.relatedBetId = relatedBetId;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
