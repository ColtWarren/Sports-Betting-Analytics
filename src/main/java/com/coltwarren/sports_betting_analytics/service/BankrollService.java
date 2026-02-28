package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.repository.BankrollRepository;
import com.coltwarren.sports_betting_analytics.model.Bankroll;
import com.coltwarren.sports_betting_analytics.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserContextService userContextService;

    public BankrollService(BankrollRepository bankrollRepository, BetService betService,
                           UserContextService userContextService) {
        this.bankrollRepository = bankrollRepository;
        this.betService = betService;
        this.userContextService = userContextService;
    }

    private User requireCurrentUser() {
        return userContextService.getCurrentUser()
            .orElseThrow(() -> new RuntimeException("Authentication required"));
    }
    
    @Transactional
    public Bankroll recordDeposit(BigDecimal amount, String notes) {
        Bankroll bankroll = new Bankroll(amount, "DEPOSIT");
        bankroll.setNotes(notes);
        bankroll.setUser(requireCurrentUser());
        return bankrollRepository.save(bankroll);
    }
    
    @Transactional
    public Bankroll recordWithdrawal(BigDecimal amount, String notes) {
        Bankroll bankroll = new Bankroll(amount.negate(), "WITHDRAWAL");
        bankroll.setNotes(notes);
        bankroll.setUser(requireCurrentUser());
        return bankrollRepository.save(bankroll);
    }
    
    public BigDecimal getCurrentBankroll() {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return BigDecimal.ZERO;
        List<Bankroll> transactions = bankrollRepository.findByUserId(userId);
        BigDecimal totalTransactions = transactions.stream()
            .map(Bankroll::getAmount)
            .filter(a -> a != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profitLoss = betService.calculateTotalProfitLoss();
        return totalTransactions.add(profitLoss);
    }
    
    public Map<String, Object> getBankrollStats() {
        Map<String, Object> stats = new HashMap<>();

        Long userId = userContextService.getCurrentUserId();
        if (userId == null) {
            stats.put("currentBankroll", BigDecimal.ZERO);
            stats.put("totalDeposits", BigDecimal.ZERO);
            stats.put("totalWithdrawals", BigDecimal.ZERO);
            stats.put("profitLoss", BigDecimal.ZERO);
            stats.put("trueROI", BigDecimal.ZERO);
            stats.put("growth", BigDecimal.ZERO);
            return stats;
        }

        List<Bankroll> transactions = bankrollRepository.findByUserId(userId);
        BigDecimal totalDeposits = transactions.stream()
            .filter(t -> "DEPOSIT".equals(t.getTransactionType()))
            .map(Bankroll::getAmount)
            .filter(a -> a != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWithdrawals = transactions.stream()
            .filter(t -> "WITHDRAWAL".equals(t.getTransactionType()))
            .map(t -> t.getAmount().abs())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profitLoss = betService.calculateTotalProfitLoss();
        BigDecimal currentBankroll = getCurrentBankroll();

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
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return List.of();
        return bankrollRepository.findByUserIdOrderByRecordedAtDesc(userId);
    }
    
    public BigDecimal getStartingBankroll() {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return BigDecimal.ZERO;
        List<Bankroll> transactions = bankrollRepository.findByUserId(userId);
        BigDecimal totalDeposits = transactions.stream()
            .filter(t -> "DEPOSIT".equals(t.getTransactionType()))
            .map(Bankroll::getAmount)
            .filter(a -> a != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWithdrawals = transactions.stream()
            .filter(t -> "WITHDRAWAL".equals(t.getTransactionType()))
            .map(t -> t.getAmount().abs())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalDeposits.subtract(totalWithdrawals);
    }
}
