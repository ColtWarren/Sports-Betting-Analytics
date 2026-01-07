package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.repository.BankrollRepository;
import com.coltwarren.sports_betting_analytics.model.Bankroll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BankrollService {
    
    private final BankrollRepository bankrollRepository;
    private final BetService betService;
    
    @Autowired
    public BankrollService(BankrollRepository bankrollRepository, BetService betService) {
        this.bankrollRepository = bankrollRepository;
        this.betService = betService;
    }
    
    public Bankroll recordDeposit(BigDecimal amount, String notes) {
        Bankroll bankroll = new Bankroll(amount, "DEPOSIT");
        bankroll.setNotes(notes);
        return bankrollRepository.save(bankroll);
    }
    
    public Bankroll recordWithdrawal(BigDecimal amount, String notes) {
        Bankroll bankroll = new Bankroll(amount.negate(), "WITHDRAWAL");
        bankroll.setNotes(notes);
        return bankrollRepository.save(bankroll);
    }
    
    public BigDecimal getCurrentBankroll() {
        BigDecimal totalDeposits = bankrollRepository.getTotalDeposits();
        BigDecimal totalWithdrawals = bankrollRepository.getTotalWithdrawals();
        BigDecimal profitLoss = betService.calculateTotalProfitLoss();
        
        if (totalDeposits == null) totalDeposits = BigDecimal.ZERO;
        if (totalWithdrawals == null) totalWithdrawals = BigDecimal.ZERO;
        if (profitLoss == null) profitLoss = BigDecimal.ZERO;
        
        return totalDeposits.subtract(totalWithdrawals).add(profitLoss);
    }
    
    public Map<String, Object> getBankrollStats() {
        Map<String, Object> stats = new HashMap<>();
        
        BigDecimal totalDeposits = bankrollRepository.getTotalDeposits();
        BigDecimal totalWithdrawals = bankrollRepository.getTotalWithdrawals();
        BigDecimal profitLoss = betService.calculateTotalProfitLoss();
        BigDecimal currentBankroll = getCurrentBankroll();
        
        if (totalDeposits == null) totalDeposits = BigDecimal.ZERO;
        if (totalWithdrawals == null) totalWithdrawals = BigDecimal.ZERO;
        if (profitLoss == null) profitLoss = BigDecimal.ZERO;
        
        stats.put("currentBankroll", currentBankroll);
        stats.put("totalDeposits", totalDeposits);
        stats.put("totalWithdrawals", totalWithdrawals);
        stats.put("profitLoss", profitLoss);
        
        // Calculate true ROI (profit / deposits)
        if (totalDeposits.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal trueROI = profitLoss
                .divide(totalDeposits, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            stats.put("trueROI", trueROI);
        } else {
            stats.put("trueROI", BigDecimal.ZERO);
        }
        
        // Calculate growth percentage
        BigDecimal startingBankroll = totalDeposits.subtract(totalWithdrawals);
        if (startingBankroll.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal growth = currentBankroll
                .subtract(startingBankroll)
                .divide(startingBankroll, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            stats.put("growth", growth);
        } else {
            stats.put("growth", BigDecimal.ZERO);
        }
        
        return stats;
    }
    
    public List<Bankroll> getAllTransactions() {
        return bankrollRepository.findAllByOrderByRecordedAtDesc();
    }
    
    public BigDecimal getStartingBankroll() {
        BigDecimal totalDeposits = bankrollRepository.getTotalDeposits();
        BigDecimal totalWithdrawals = bankrollRepository.getTotalWithdrawals();
        
        if (totalDeposits == null) totalDeposits = BigDecimal.ZERO;
        if (totalWithdrawals == null) totalWithdrawals = BigDecimal.ZERO;
        
        return totalDeposits.subtract(totalWithdrawals);
    }
}
