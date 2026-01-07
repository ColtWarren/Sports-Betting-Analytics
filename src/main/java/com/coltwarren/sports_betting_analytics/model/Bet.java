package com.coltwarren.sports_betting_analytics.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bet Entity - Represents a single sports bet in the system
 * 
 * @author Colt Warren
 * @version 1.0
 */
@Entity
@Table(name = "bets")
public class Bet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String sport;
    
    @Column(nullable = false, length = 200)
    private String eventName;
    
    @Column(nullable = false, length = 50)
    private String betType;
    
    @Column(nullable = false, length = 100)
    private String selection;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal stake;
    
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal odds;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal potentialPayout;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal actualPayout;
    
    @Column(nullable = false, length = 50)
    private String sportsbookName;
    
    @Column(nullable = false, length = 20)
    private String status;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal profitLoss;
    
    @Column(nullable = false)
    private LocalDateTime placedAt;
    
    private LocalDateTime settledAt;
    
    private LocalDateTime eventStartTime;
    
    @Column(precision = 6, scale = 2)
    private BigDecimal closingOdds;
    
    private Boolean beatClosingLine;
    
    @Column(length = 500)
    private String notes;
    
    // Constructors
    public Bet() {
        this.status = "PENDING";
        this.placedAt = LocalDateTime.now();
    }
    
    public Bet(String sport, String eventName, String betType, String selection,
               BigDecimal stake, BigDecimal odds, String sportsbookName) {
        this();
        this.sport = sport;
        this.eventName = eventName;
        this.betType = betType;
        this.selection = selection;
        this.stake = stake;
        this.odds = odds;
        this.sportsbookName = sportsbookName;
        this.potentialPayout = calculatePotentialPayout(stake, odds);
    }
    
    // Business Logic
    private BigDecimal calculatePotentialPayout(BigDecimal stake, BigDecimal americanOdds) {
        if (americanOdds.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal profit = stake.multiply(americanOdds.divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP));
            return profit.add(stake);
        } else {
            BigDecimal absOdds = americanOdds.abs();
            BigDecimal profit = stake.multiply(new BigDecimal("100").divide(absOdds, 2, BigDecimal.ROUND_HALF_UP));
            return profit.add(stake);
        }
    }
    
    public void markAsWon() {
        this.status = "WON";
        this.actualPayout = this.potentialPayout;
        this.profitLoss = this.actualPayout.subtract(this.stake);
        this.settledAt = LocalDateTime.now();
    }
    
    public void markAsLost() {
        this.status = "LOST";
        this.actualPayout = BigDecimal.ZERO;
        this.profitLoss = this.stake.negate();
        this.settledAt = LocalDateTime.now();
    }
    
    public void markAsPush() {
        this.status = "PUSH";
        this.actualPayout = this.stake;
        this.profitLoss = BigDecimal.ZERO;
        this.settledAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }
    
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    
    public String getBetType() { return betType; }
    public void setBetType(String betType) { this.betType = betType; }
    
    public String getSelection() { return selection; }
    public void setSelection(String selection) { this.selection = selection; }
    
    public BigDecimal getStake() { return stake; }
    public void setStake(BigDecimal stake) { this.stake = stake; }
    
    public BigDecimal getOdds() { return odds; }
    public void setOdds(BigDecimal odds) { this.odds = odds; }
    
    public BigDecimal getPotentialPayout() { return potentialPayout; }
    public void setPotentialPayout(BigDecimal potentialPayout) { this.potentialPayout = potentialPayout; }
    
    public BigDecimal getActualPayout() { return actualPayout; }
    public void setActualPayout(BigDecimal actualPayout) { this.actualPayout = actualPayout; }
    
    public String getSportsbookName() { return sportsbookName; }
    public void setSportsbookName(String sportsbookName) { this.sportsbookName = sportsbookName; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public BigDecimal getProfitLoss() { return profitLoss; }
    public void setProfitLoss(BigDecimal profitLoss) { this.profitLoss = profitLoss; }
    
    public LocalDateTime getPlacedAt() { return placedAt; }
    public void setPlacedAt(LocalDateTime placedAt) { this.placedAt = placedAt; }
    
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    
    public LocalDateTime getEventStartTime() { return eventStartTime; }
    public void setEventStartTime(LocalDateTime eventStartTime) { this.eventStartTime = eventStartTime; }
    
    public BigDecimal getClosingOdds() { return closingOdds; }
    public void setClosingOdds(BigDecimal closingOdds) { this.closingOdds = closingOdds; }
    
    public Boolean getBeatClosingLine() { return beatClosingLine; }
    public void setBeatClosingLine(Boolean beatClosingLine) { this.beatClosingLine = beatClosingLine; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    @Override
    public String toString() {
        return "Bet{" +
                "id=" + id +
                ", sport='" + sport + '\'' +
                ", eventName='" + eventName + '\'' +
                ", betType='" + betType + '\'' +
                ", selection='" + selection + '\'' +
                ", stake=" + stake +
                ", odds=" + odds +
                ", status='" + status + '\'' +
                ", sportsbookName='" + sportsbookName + '\'' +
                '}';
    }

    // CLV Calculation Methods
    public Double calculateCLV() {
        if (this.closingOdds == null || this.odds == null) {
            return null;
        }
        
        double yourOddsDecimal = americanToDecimal(this.odds.intValue());
        double closingOddsDecimal = americanToDecimal(this.closingOdds.intValue());
        
        // CLV = (Your Decimal Odds / Closing Decimal Odds) - 1
        return ((yourOddsDecimal / closingOddsDecimal) - 1) * 100;
    }
    
    public void checkBeatClosingLine() {
        if (this.closingOdds == null || this.odds == null) {
            this.beatClosingLine = null;
            return;
        }
        
        int yourOdds = this.odds.intValue();
        int closing = this.closingOdds.intValue();
        
        // Better odds = beat the line
        if (yourOdds > 0 && closing > 0) {
            this.beatClosingLine = yourOdds > closing;
        } else if (yourOdds < 0 && closing < 0) {
            this.beatClosingLine = Math.abs(yourOdds) < Math.abs(closing);
        } else {
            this.beatClosingLine = yourOdds > closing;
        }
    }
    
    private double americanToDecimal(int americanOdds) {
        if (americanOdds > 0) {
            return (americanOdds / 100.0) + 1;
        } else {
            return (100.0 / Math.abs(americanOdds)) + 1;
        }
    }
    }

