package com.coltwarren.sports_betting_analytics.model;

import com.coltwarren.sports_betting_analytics.util.OddsConversionUtils;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Bet Entity - Represents a single sports bet in the system
 * 
 * @author Colt Warren
 * @version 1.0
 */
@Entity
@Table(name = "bets", indexes = {
    @Index(name = "idx_bet_user_id", columnList = "user_id"),
    @Index(name = "idx_bet_status", columnList = "status"),
    @Index(name = "idx_bet_sport", columnList = "sport"),
    @Index(name = "idx_bet_placed_at", columnList = "placedAt"),
    @Index(name = "idx_bet_settled_at", columnList = "settledAt"),
    @Index(name = "idx_bet_sportsbook", columnList = "sportsbookName"),
    @Index(name = "idx_bet_user_status", columnList = "user_id, status"),
    @Index(name = "idx_bet_user_sport", columnList = "user_id, sport"),
    @Index(name = "idx_bet_user_bet_type", columnList = "user_id, betType"),
    @Index(name = "idx_bet_user_placed_at", columnList = "user_id, placedAt"),
    @Index(name = "idx_bet_user_settled_at", columnList = "user_id, settledAt")
})
public class Bet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Sport sport;
    
    @Column(nullable = false, length = 200)
    private String eventName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BetType betType;
    
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
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BetStatus status;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal profitLoss;
    
    @Column(nullable = false)
    private LocalDateTime placedAt;
    
    private LocalDateTime settledAt;
    
    private LocalDateTime eventStartTime;
    
    @Column(precision = 6, scale = 2)
    private BigDecimal closingOdds;
    
    private Boolean beatClosingLine;
    
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 100)
    private String sportsbookLocation; // e.g., "Online - NJ" or "Las Vegas, NV"

    @Column
    private Boolean triggersW2G = false; // Auto-calculated when bet wins

    @Column
    private Boolean w2gIssued = false; // User manually marks when they receive form

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Bet() {
        this.status = BetStatus.PENDING;
        this.placedAt = LocalDateTime.now();
    }

    public Bet(Sport sport, String eventName, BetType betType, String selection,
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
            // Positive odds: profit = stake × (odds / 100)
            BigDecimal profit = stake.multiply(americanOdds.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            return profit.add(stake);
        } else {
            // Negative odds: profit = stake × (100 / |odds|)
            BigDecimal absOdds = americanOdds.abs();
            BigDecimal profit = stake.multiply(BigDecimal.valueOf(100).divide(absOdds, 2, RoundingMode.HALF_UP));
            return profit.add(stake);
        }
    }

    /**
     * Recalculate potentialPayout from current stake and odds.
     * Call this after setting stake/odds on a bet created via the default constructor
     * (e.g., JSON deserialization), which does not auto-calculate potentialPayout.
     */
    public void recalculatePotentialPayout() {
        if (this.stake != null && this.odds != null) {
            this.potentialPayout = calculatePotentialPayout(this.stake, this.odds);
        }
    }

    public void markAsWon() {
        this.status = BetStatus.WON;
        // Ensure potentialPayout exists (defensive — covers bets created before the fix)
        if (this.potentialPayout == null && this.stake != null && this.odds != null) {
            this.potentialPayout = calculatePotentialPayout(this.stake, this.odds);
        }
        if (this.actualPayout == null) {
            this.actualPayout = this.potentialPayout;
        }
        if (this.actualPayout != null && this.stake != null) {
            this.profitLoss = this.actualPayout.subtract(this.stake);
        } else {
            this.profitLoss = BigDecimal.ZERO;
        }
        this.settledAt = LocalDateTime.now();
        // Check if this win triggers W-2G
        checkIfTriggersW2G();
    }
    
    public void markAsLost() {
        this.status = BetStatus.LOST;
        this.actualPayout = BigDecimal.ZERO;
        this.profitLoss = this.stake.negate();
        this.settledAt = LocalDateTime.now();
    }
    
    public void markAsPush() {
        this.status = BetStatus.PUSH;
        this.actualPayout = this.stake;
        this.profitLoss = BigDecimal.ZERO;
        this.settledAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Sport getSport() { return sport; }
    public void setSport(Sport sport) { this.sport = sport; }
    
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    
    public BetType getBetType() { return betType; }
    public void setBetType(BetType betType) { this.betType = betType; }
    
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
    
    public BetStatus getStatus() { return status; }
    public void setStatus(BetStatus status) { this.status = status; }
    
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

    public String getSportsbookLocation() { return sportsbookLocation; }
    public void setSportsbookLocation(String sportsbookLocation) { this.sportsbookLocation = sportsbookLocation; }

    public Boolean getTriggersW2G() { return triggersW2G; }
    public void setTriggersW2G(Boolean triggersW2G) { this.triggersW2G = triggersW2G; }

    public Boolean getW2gIssued() { return w2gIssued; }
    public void setW2gIssued(Boolean w2gIssued) { this.w2gIssued = w2gIssued; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Bet{" +
                "id=" + id +
                ", sport=" + sport +
                ", eventName='" + eventName + '\'' +
                ", betType=" + betType +
                ", selection='" + selection + '\'' +
                ", stake=" + stake +
                ", odds=" + odds +
                ", status=" + status +
                ", sportsbookName='" + sportsbookName + '\'' +
                '}';
    }

    /**
     * Checks if this winning bet triggers W-2G form requirements
     * W-2G required if: winnings >= $600 AND payout is 300x or more the wager
     */
    public boolean checkIfTriggersW2G() {
        if (status == BetStatus.WON && actualPayout != null && stake != null) {
            BigDecimal netWinnings = actualPayout.subtract(stake);
            boolean meetsAmountThreshold = netWinnings.compareTo(BigDecimal.valueOf(600)) >= 0;

            BigDecimal ratio = actualPayout.divide(stake, 2, RoundingMode.HALF_UP);
            boolean meets300xThreshold = ratio.compareTo(BigDecimal.valueOf(300)) >= 0;

            this.triggersW2G = meetsAmountThreshold && meets300xThreshold;
            return this.triggersW2G;
        }
        this.triggersW2G = false;
        return false;
    }

    /**
     * Gets the tax year for this bet (based on settlement date, or placement date if unsettled)
     */
    public int getTaxYear() {
        return (settledAt != null ? settledAt : placedAt).getYear();
    }

    // CLV Calculation Methods
    public Double calculateCLV() {
        if (this.closingOdds == null || this.odds == null) {
            return null;
        }
        
        double yourOddsDecimal = OddsConversionUtils.americanToDecimal(this.odds.intValue());
        double closingOddsDecimal = OddsConversionUtils.americanToDecimal(this.closingOdds.intValue());
        
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bet that = (Bet) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    }

