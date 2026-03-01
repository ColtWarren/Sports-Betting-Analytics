package com.coltwarren.sports_betting_analytics.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "w2g_forms", indexes = {
    @Index(name = "idx_w2g_user_id", columnList = "user_id"),
    @Index(name = "idx_w2g_tax_year", columnList = "tax_year"),
    @Index(name = "idx_w2g_bet_id", columnList = "bet_id"),
    @Index(name = "idx_w2g_user_year", columnList = "user_id, tax_year")
})
public class W2GForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bet_id")
    private Bet bet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "date_won", nullable = false)
    private LocalDate dateWon;

    @Column(name = "gross_winnings", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossWinnings; // Total payout including stake

    @Column(name = "wager_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal wagerAmount; // Original stake

    @Column(name = "net_winnings", nullable = false, precision = 10, scale = 2)
    private BigDecimal netWinnings; // Gross - Wager

    @Column(name = "sportsbook_name", nullable = false, length = 50)
    private String sportsbookName;

    @Column(name = "sportsbook_tin", length = 20)
    private String sportsbookTIN; // Tax ID Number

    @Column(name = "payer_address", length = 200)
    private String payerAddress;

    @Column(name = "federal_withholding")
    private Boolean federalWithholding = false; // 24% withheld for large wins

    @Column(name = "federal_withholding_amount", precision = 10, scale = 2)
    private BigDecimal federalWithholdingAmount;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Column(name = "form_number", length = 50)
    private String formNumber; // W-2G tracking number from sportsbook

    @Column(name = "form_received")
    private Boolean formReceived = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public W2GForm() {}

    public W2GForm(Bet bet, LocalDate dateWon, BigDecimal grossWinnings,
                   BigDecimal wagerAmount, String sportsbookName, Integer taxYear) {
        this.bet = bet;
        this.dateWon = dateWon;
        this.grossWinnings = grossWinnings;
        this.wagerAmount = wagerAmount;
        this.netWinnings = grossWinnings.subtract(wagerAmount);
        this.sportsbookName = sportsbookName;
        this.taxYear = taxYear;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bet getBet() {
        return bet;
    }

    public void setBet(Bet bet) {
        this.bet = bet;
    }

    public LocalDate getDateWon() {
        return dateWon;
    }

    public void setDateWon(LocalDate dateWon) {
        this.dateWon = dateWon;
    }

    public BigDecimal getGrossWinnings() {
        return grossWinnings;
    }

    public void setGrossWinnings(BigDecimal grossWinnings) {
        this.grossWinnings = grossWinnings;
        if (this.wagerAmount != null) {
            this.netWinnings = grossWinnings.subtract(this.wagerAmount);
        }
    }

    public BigDecimal getWagerAmount() {
        return wagerAmount;
    }

    public void setWagerAmount(BigDecimal wagerAmount) {
        this.wagerAmount = wagerAmount;
        if (this.grossWinnings != null) {
            this.netWinnings = this.grossWinnings.subtract(wagerAmount);
        }
    }

    public BigDecimal getNetWinnings() {
        return netWinnings;
    }

    public void setNetWinnings(BigDecimal netWinnings) {
        this.netWinnings = netWinnings;
    }

    public String getSportsbookName() {
        return sportsbookName;
    }

    public void setSportsbookName(String sportsbookName) {
        this.sportsbookName = sportsbookName;
    }

    public String getSportsbookTIN() {
        return sportsbookTIN;
    }

    public void setSportsbookTIN(String sportsbookTIN) {
        this.sportsbookTIN = sportsbookTIN;
    }

    public String getPayerAddress() {
        return payerAddress;
    }

    public void setPayerAddress(String payerAddress) {
        this.payerAddress = payerAddress;
    }

    public Boolean getFederalWithholding() {
        return federalWithholding;
    }

    public void setFederalWithholding(Boolean federalWithholding) {
        this.federalWithholding = federalWithholding;
    }

    public BigDecimal getFederalWithholdingAmount() {
        return federalWithholdingAmount;
    }

    public void setFederalWithholdingAmount(BigDecimal federalWithholdingAmount) {
        this.federalWithholdingAmount = federalWithholdingAmount;
    }

    public Integer getTaxYear() {
        return taxYear;
    }

    public void setTaxYear(Integer taxYear) {
        this.taxYear = taxYear;
    }

    public String getFormNumber() {
        return formNumber;
    }

    public void setFormNumber(String formNumber) {
        this.formNumber = formNumber;
    }

    public Boolean getFormReceived() {
        return formReceived;
    }

    public void setFormReceived(Boolean formReceived) {
        this.formReceived = formReceived;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
