package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.Bet;
import com.coltwarren.sports_betting_analytics.model.User;
import com.coltwarren.sports_betting_analytics.model.W2GForm;
import com.coltwarren.sports_betting_analytics.repository.BetRepository;
import com.coltwarren.sports_betting_analytics.repository.W2GFormRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaxReportService {

    @Autowired
    private BetRepository betRepository;

    @Autowired
    private W2GFormRepository w2gFormRepository;

    @Autowired
    private UserContextService userContextService;

    private User requireCurrentUser() {
        return userContextService.getCurrentUser()
            .orElseThrow(() -> new RuntimeException("Authentication required"));
    }

    /**
     * Get all settled bets for a specific tax year
     */
    public List<Bet> getBetsForTaxYear(int year) {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return List.of();
        LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(year, 12, 31, 23, 59);
        return betRepository.findBySettledAtBetween(start, end).stream()
            .filter(b -> b.getUser() != null && userId.equals(b.getUser().getId()))
            .toList();
    }

    /**
     * Calculate total winnings for a tax year (sum of all winning bets)
     */
    public BigDecimal getTotalWinnings(int year) {
        List<Bet> bets = getBetsForTaxYear(year);
        return bets.stream()
            .filter(b -> "WON".equals(b.getStatus()))
            .map(Bet::getProfitLoss)
            .filter(pl -> pl != null && pl.compareTo(BigDecimal.ZERO) > 0)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total losses for a tax year (sum of all losing bets)
     */
    public BigDecimal getTotalLosses(int year) {
        List<Bet> bets = getBetsForTaxYear(year);
        return bets.stream()
            .filter(b -> "LOST".equals(b.getStatus()))
            .map(b -> b.getProfitLoss() != null ? b.getProfitLoss().abs() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total amount wagered for a tax year
     */
    public BigDecimal getTotalWagered(int year) {
        List<Bet> bets = getBetsForTaxYear(year);
        return bets.stream()
            .map(Bet::getStake)
            .filter(stake -> stake != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get net profit/loss for a tax year
     */
    public BigDecimal getNetProfitLoss(int year) {
        return getTotalWinnings(year).subtract(getTotalLosses(year));
    }

    /**
     * Get all bets that triggered W-2G forms
     */
    public List<Bet> getW2GBets(int year) {
        List<Bet> bets = getBetsForTaxYear(year);
        return bets.stream()
            .filter(b -> Boolean.TRUE.equals(b.getTriggersW2G()))
            .collect(Collectors.toList());
    }

    /**
     * Calculate deductible losses (capped at total winnings per IRS rules)
     */
    public BigDecimal getDeductibleLosses(int year) {
        BigDecimal winnings = getTotalWinnings(year);
        BigDecimal losses = getTotalLosses(year);
        // You can only deduct losses up to the amount of your winnings
        return losses.min(winnings);
    }

    /**
     * Calculate estimated tax liability (using 24% bracket as example)
     */
    public BigDecimal getEstimatedTax(int year) {
        BigDecimal taxableWinnings = getTotalWinnings(year);
        // Assuming 24% tax bracket (user should adjust based on actual bracket)
        return taxableWinnings.multiply(new BigDecimal("0.24")).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Get comprehensive tax summary for a year
     */
    public Map<String, Object> getTaxSummary(int year) {
        Map<String, Object> summary = new HashMap<>();

        summary.put("taxYear", year);
        summary.put("totalWagered", getTotalWagered(year));
        summary.put("totalWinnings", getTotalWinnings(year));
        summary.put("totalLosses", getTotalLosses(year));
        summary.put("netProfitLoss", getNetProfitLoss(year));
        summary.put("deductibleLosses", getDeductibleLosses(year));
        summary.put("estimatedTax", getEstimatedTax(year));

        List<Bet> w2gBets = getW2GBets(year);
        summary.put("w2gBetCount", w2gBets.size());
        summary.put("w2gBets", w2gBets);

        Long userId = userContextService.getCurrentUserId();
        List<W2GForm> w2gForms = userId != null
            ? w2gFormRepository.findByUserIdAndTaxYear(userId, year)
            : List.of();
        summary.put("w2gFormsReceived", w2gForms.stream().filter(W2GForm::getFormReceived).count());
        summary.put("w2gFormsPending", w2gForms.stream().filter(f -> !f.getFormReceived()).count());

        List<Bet> allBets = getBetsForTaxYear(year);
        long wonBets = allBets.stream().filter(b -> "WON".equals(b.getStatus())).count();
        long totalBets = allBets.size();
        double winRate = totalBets > 0 ? (double) wonBets / totalBets * 100 : 0;
        summary.put("totalBets", totalBets);
        summary.put("wonBets", wonBets);
        summary.put("winRate", winRate);

        return summary;
    }

    /**
     * Automatically create W-2G form entry when bet is marked as won and triggers W-2G
     */
    public void createW2GFormFromBet(Bet bet) {
        if (Boolean.TRUE.equals(bet.getTriggersW2G())) {
            // Check if W-2G form already exists for this bet
            W2GForm existing = w2gFormRepository.findByBetId(bet.getId());
            if (existing == null) {
                W2GForm form = new W2GForm(
                    bet.getId(),
                    bet.getSettledAt().toLocalDate(),
                    bet.getActualPayout(),
                    bet.getStake(),
                    bet.getSportsbookName(),
                    bet.getTaxYear()
                );
                if (bet.getUser() != null) {
                    form.setUser(bet.getUser());
                }
                w2gFormRepository.save(form);
            }
        }
    }
}
