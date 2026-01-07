package com.coltwarren.sports_betting_analytics.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bankroll")
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
}
