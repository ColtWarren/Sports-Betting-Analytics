package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.Bet;
import com.coltwarren.sports_betting_analytics.model.User;
import com.coltwarren.sports_betting_analytics.repository.BetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * BetService - Business Logic Layer
 * 
 * This service layer sits between the Controller (REST API) and Repository (Database).
 * It contains all business logic for managing bets.
 * 
 * @Service annotation makes this a Spring-managed bean
 * @Transactional ensures database operations are atomic (all-or-nothing)
 * 
 * @author Colt Warren
 * @version 1.0
 */
@Service
@Transactional
public class BetService {

    private final BetRepository betRepository;
    private final TaxReportService taxReportService;
    private final UserContextService userContextService;

    @Autowired
    public BetService(BetRepository betRepository, TaxReportService taxReportService,
                      UserContextService userContextService) {
        this.betRepository = betRepository;
        this.taxReportService = taxReportService;
        this.userContextService = userContextService;
    }
    
    // ============================================
    // CREATE OPERATIONS
    // ============================================
    
    /**
     * Create and save a new bet
     * 
     * @param bet - Bet object to save
     * @return Saved bet (with generated ID)
     */
    public Bet createBet(Bet bet) {
        validateBet(bet);
        bet.setUser(requireCurrentUser());
        return betRepository.save(bet);
    }
    
    /**
     * Quick bet creation with required fields only
     * 
     * @param sport - Sport type (e.g., "NFL")
     * @param eventName - Event description
     * @param betType - Type of bet
     * @param selection - What was bet on
     * @param stake - Amount wagered
     * @param odds - American odds
     * @param sportsbookName - Which sportsbook
     * @return Saved bet
     */
    public Bet createBet(String sport, String eventName, String betType, String selection,
                        BigDecimal stake, BigDecimal odds, String sportsbookName) {
        
        Bet bet = new Bet(sport, eventName, betType, selection, stake, odds, sportsbookName);
        bet.setUser(requireCurrentUser());
        return betRepository.save(bet);
    }
    
    // ============================================
    // READ OPERATIONS
    // ============================================
    
    /**
     * Get all bets
     * 
     * @return List of all bets
     */
    public List<Bet> getAllBets() {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return List.of();
        return betRepository.findByUserIdOrderByPlacedAtDesc(userId);
    }
    
    /**
     * Find bet by ID
     * 
     * @param id - Bet ID
     * @return Optional containing bet if found
     */
    public Optional<Bet> getBetById(Long id) {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return Optional.empty();
        return betRepository.findByIdAndUserId(id, userId);
    }
    
    /**
     * Get all pending bets
     * 
     * @return List of pending bets
     */
    public List<Bet> getPendingBets() {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return List.of();
        return betRepository.findByUserIdAndStatus(userId, "PENDING");
    }
    
    /**
     * Get all settled bets (won, lost, pushed)
     * 
     * @return List of settled bets
     */
    public List<Bet> getSettledBets() {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return List.of();
        List<Bet> userBets = betRepository.findByUserId(userId);
        return userBets.stream()
            .filter(b -> List.of("WON", "LOST", "PUSH").contains(b.getStatus()))
            .toList();
    }
    
    /**
     * Get bets from a specific sportsbook
     * 
     * @param sportsbookName - Sportsbook name
     * @return List of bets from that sportsbook
     */
    public List<Bet> getBetsBySportsbook(String sportsbookName) {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return List.of();
        return betRepository.findByUserId(userId).stream()
            .filter(b -> sportsbookName.equals(b.getSportsbookName()))
            .toList();
    }
    
    /**
     * Get bets for a specific sport
     * 
     * @param sport - Sport name
     * @return List of bets on that sport
     */
    public List<Bet> getBetsBySport(String sport) {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return List.of();
        return betRepository.findByUserId(userId).stream()
            .filter(b -> sport.equals(b.getSport()))
            .toList();
    }
    
    /**
     * Get bets by type
     * 
     * @param betType - Bet type (MONEYLINE, SPREAD, etc.)
     * @return List of bets of that type
     */
    public List<Bet> getBetsByType(String betType) {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return List.of();
        return betRepository.findByUserId(userId).stream()
            .filter(b -> betType.equals(b.getBetType()))
            .toList();
    }
    
    /**
     * Get bets placed within a date range
     * 
     * @param start - Start date/time
     * @param end - End date/time
     * @return List of bets in that range
     */
    public List<Bet> getBetsByDateRange(LocalDateTime start, LocalDateTime end) {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return List.of();
        return betRepository.findByUserId(userId).stream()
            .filter(b -> b.getPlacedAt() != null && !b.getPlacedAt().isBefore(start) && !b.getPlacedAt().isAfter(end))
            .toList();
    }
    
    /**
     * Get recent bets
     * 
     * @return List of recent bets (ordered by date)
     */
    public List<Bet> getRecentBets() {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return List.of();
        return betRepository.findByUserIdOrderByPlacedAtDesc(userId);
    }
    
    // ============================================
    // UPDATE OPERATIONS
    // ============================================
    
    /**
     * Update an existing bet
     * 
     * @param id - Bet ID to update
     * @param updatedBet - Bet with new values
     * @return Updated bet
     * @throws RuntimeException if bet not found
     */
    public Bet updateBet(Long id, Bet updatedBet) {
        User currentUser = requireCurrentUser();
        Bet existingBet = betRepository.findByIdAndUserId(id, currentUser.getId())
            .orElseThrow(() -> new RuntimeException("Bet not found with id: " + id));
        
        // Update fields (only if not null)
        if (updatedBet.getSport() != null) existingBet.setSport(updatedBet.getSport());
        if (updatedBet.getEventName() != null) existingBet.setEventName(updatedBet.getEventName());
        if (updatedBet.getBetType() != null) existingBet.setBetType(updatedBet.getBetType());
        if (updatedBet.getSelection() != null) existingBet.setSelection(updatedBet.getSelection());
        if (updatedBet.getStake() != null) existingBet.setStake(updatedBet.getStake());
        if (updatedBet.getOdds() != null) existingBet.setOdds(updatedBet.getOdds());
        if (updatedBet.getSportsbookName() != null) existingBet.setSportsbookName(updatedBet.getSportsbookName());
        if (updatedBet.getNotes() != null) existingBet.setNotes(updatedBet.getNotes());
        
        return betRepository.save(existingBet);
    }
    
    /**
     * Mark a bet as won
     * 
     * @param id - Bet ID
     * @return Updated bet
     * @throws RuntimeException if bet not found
     */
    public Bet markBetAsWon(Long id) {
        Bet bet = betRepository.findByIdAndUserId(id, requireCurrentUser().getId())
            .orElseThrow(() -> new RuntimeException("Bet not found with id: " + id));

        bet.markAsWon();
        Bet savedBet = betRepository.save(bet);

        // Automatically create W-2G form if bet triggers W-2G requirement
        taxReportService.createW2GFormFromBet(savedBet);

        return savedBet;
    }
    
    /**
     * Mark a bet as lost
     * 
     * @param id - Bet ID
     * @return Updated bet
     * @throws RuntimeException if bet not found
     */
    public Bet markBetAsLost(Long id) {
        Bet bet = betRepository.findByIdAndUserId(id, requireCurrentUser().getId())
            .orElseThrow(() -> new RuntimeException("Bet not found with id: " + id));
        
        bet.markAsLost();
        return betRepository.save(bet);
    }
    
    /**
     * Mark a bet as push (tie)
     * 
     * @param id - Bet ID
     * @return Updated bet
     * @throws RuntimeException if bet not found
     */
    public Bet markBetAsPush(Long id) {
        Bet bet = betRepository.findByIdAndUserId(id, requireCurrentUser().getId())
            .orElseThrow(() -> new RuntimeException("Bet not found with id: " + id));
        
        bet.markAsPush();
        return betRepository.save(bet);
    }
    
    /**
     * Update closing odds and calculate if we beat closing line
     * 
     * @param id - Bet ID
     * @param closingOdds - Closing line odds
     * @return Updated bet
     */
    public Bet updateClosingOdds(Long id, BigDecimal closingOdds) {
        Bet bet = betRepository.findByIdAndUserId(id, requireCurrentUser().getId())
            .orElseThrow(() -> new RuntimeException("Bet not found with id: " + id));
        
        bet.setClosingOdds(closingOdds);
        
        // Calculate if we beat closing line
        // For negative odds: more negative is worse (e.g., -120 is worse than -110)
        // For positive odds: more positive is better (e.g., +150 is better than +120)
        boolean beatLine = false;
        BigDecimal ourOdds = bet.getOdds();
        
        if (ourOdds.compareTo(BigDecimal.ZERO) < 0 && closingOdds.compareTo(BigDecimal.ZERO) < 0) {
            // Both negative: we beat line if our odds are less negative (closer to 0)
            beatLine = ourOdds.compareTo(closingOdds) > 0;
        } else if (ourOdds.compareTo(BigDecimal.ZERO) > 0 && closingOdds.compareTo(BigDecimal.ZERO) > 0) {
            // Both positive: we beat line if our odds are more positive
            beatLine = ourOdds.compareTo(closingOdds) > 0;
        }
        
        bet.setBeatClosingLine(beatLine);
        return betRepository.save(bet);
    }
    
    // ============================================
    // DELETE OPERATIONS
    // ============================================
    
    /**
     * Delete a bet by ID
     * 
     * @param id - Bet ID to delete
     * @throws RuntimeException if bet not found
     */
    public void deleteBet(Long id) {
        User currentUser = requireCurrentUser();
        betRepository.findByIdAndUserId(id, currentUser.getId())
            .orElseThrow(() -> new RuntimeException("Bet not found with id: " + id));
        betRepository.deleteById(id);
    }
    
    /**
     * Delete all bets (USE WITH CAUTION!)
     * 
     * @return Number of bets deleted
     */
    public long deleteAllBets() {
        long count = betRepository.count();
        betRepository.deleteAll();
        return count;
    }
    
    // ============================================
    // ANALYTICS & STATISTICS
    // ============================================
    
    /**
     * Calculate total profit/loss across all bets
     * 
     * @return Total profit (positive) or loss (negative)
     */
    public BigDecimal calculateTotalProfitLoss() {
        return getAllBets().stream()
            .map(Bet::getProfitLoss)
            .filter(pl -> pl != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculate profit/loss for a specific sportsbook
     * 
     * @param sportsbookName - Sportsbook name
     * @return Profit/loss for that sportsbook
     */
    public BigDecimal calculateProfitLossBySportsbook(String sportsbookName) {
        return getAllBets().stream()
            .filter(b -> sportsbookName.equals(b.getSportsbookName()) && b.getProfitLoss() != null)
            .map(Bet::getProfitLoss)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculate profit/loss for a specific sport
     * 
     * @param sport - Sport name
     * @return Profit/loss for that sport
     */
    public BigDecimal calculateProfitLossBySport(String sport) {
        return getAllBets().stream()
            .filter(b -> sport.equals(b.getSport()) && b.getProfitLoss() != null)
            .map(Bet::getProfitLoss)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculate win rate (percentage of bets won)
     * 
     * @return Win rate as percentage (0-100)
     */
    public Double calculateWinRate() {
        List<Bet> settled = getAllBets().stream()
            .filter(b -> "WON".equals(b.getStatus()) || "LOST".equals(b.getStatus()))
            .toList();
        if (settled.isEmpty()) return 0.0;
        long won = settled.stream().filter(b -> "WON".equals(b.getStatus())).count();
        return (double) won / settled.size() * 100;
    }
    
    /**
     * Calculate ROI (Return on Investment)
     * 
     * @return ROI as percentage (e.g., 5.0 = 5%)
     */
    public Double calculateROI() {
        List<Bet> bets = getAllBets().stream()
            .filter(b -> b.getProfitLoss() != null && b.getStake() != null)
            .toList();
        if (bets.isEmpty()) return 0.0;
        BigDecimal totalProfit = bets.stream().map(Bet::getProfitLoss).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalStake = bets.stream().map(Bet::getStake).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalStake.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return totalProfit.divide(totalStake, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
    }
    
    /**
     * Calculate total amount staked
     * 
     * @return Total stake across all bets
     */
    public BigDecimal calculateTotalStaked() {
        return getAllBets().stream()
            .map(Bet::getStake)
            .filter(s -> s != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Get count of bets by status
     * 
     * @param status - Status to count
     * @return Number of bets with that status
     */
    public long countBetsByStatus(String status) {
        return getAllBets().stream()
            .filter(b -> status.equals(b.getStatus()))
            .count();
    }
    
    /**
     * Get count of bets by sportsbook
     * 
     * @param sportsbookName - Sportsbook to count
     * @return Number of bets at that sportsbook
     */
    public long countBetsBySportsbook(String sportsbookName) {
        return getAllBets().stream()
            .filter(b -> sportsbookName.equals(b.getSportsbookName()))
            .count();
    }
    
    /**
     * Get best performing sportsbook
     * 
     * @return List of sportsbooks ordered by profit (best first)
     */
    public List<String> getBestPerformingSportsbooks() {
        return getAllBets().stream()
            .filter(b -> b.getProfitLoss() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                Bet::getSportsbookName,
                java.util.stream.Collectors.reducing(BigDecimal.ZERO, Bet::getProfitLoss, BigDecimal::add)))
            .entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .map(java.util.Map.Entry::getKey)
            .toList();
    }
    
    /**
     * Get most profitable sport
     * 
     * @return List of sports ordered by profit (best first)
     */
    public List<String> getMostProfitableSports() {
        return getAllBets().stream()
            .filter(b -> b.getProfitLoss() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                Bet::getSport,
                java.util.stream.Collectors.reducing(BigDecimal.ZERO, Bet::getProfitLoss, BigDecimal::add)))
            .entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .map(java.util.Map.Entry::getKey)
            .toList();
    }
    
    /**
     * Get comprehensive betting statistics
     * 
     * @return BettingStats object with all statistics
     */
    public BettingStats getComprehensiveStats() {
        return new BettingStats(
            betRepository.count(),
            countBetsByStatus("PENDING"),
            countBetsByStatus("WON"),
            countBetsByStatus("LOST"),
            countBetsByStatus("PUSH"),
            calculateTotalStaked(),
            calculateTotalProfitLoss(),
            calculateWinRate(),
            calculateROI()
        );
    }
    
    // ============================================
    // VALIDATION
    // ============================================
    
    /**
     * Validate bet before saving
     * 
     * @param bet - Bet to validate
     * @throws IllegalArgumentException if validation fails
     */
    private User requireCurrentUser() {
        return userContextService.getCurrentUser()
            .orElseThrow(() -> new RuntimeException("Authentication required"));
    }

    private void validateBet(Bet bet) {
        if (bet.getStake() == null || bet.getStake().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stake must be greater than zero");
        }
        
        if (bet.getOdds() == null) {
            throw new IllegalArgumentException("Odds cannot be null");
        }
        
        if (bet.getSport() == null || bet.getSport().trim().isEmpty()) {
            throw new IllegalArgumentException("Sport cannot be empty");
        }
        
        if (bet.getEventName() == null || bet.getEventName().trim().isEmpty()) {
            throw new IllegalArgumentException("Event name cannot be empty");
        }
        
        if (bet.getBetType() == null || bet.getBetType().trim().isEmpty()) {
            throw new IllegalArgumentException("Bet type cannot be empty");
        }
        
        if (bet.getSelection() == null || bet.getSelection().trim().isEmpty()) {
            throw new IllegalArgumentException("Selection cannot be empty");
        }
        
        if (bet.getSportsbookName() == null || bet.getSportsbookName().trim().isEmpty()) {
            throw new IllegalArgumentException("Sportsbook name cannot be empty");
        }
    }
    
    // ============================================
    // INNER CLASS: BETTING STATS
    // ============================================
    
    /**
     * Inner class to hold comprehensive betting statistics
     */
    public static class BettingStats {
        private final long totalBets;
        private final long pendingBets;
        private final long wonBets;
        private final long lostBets;
        private final long pushedBets;
        private final BigDecimal totalStaked;
        private final BigDecimal totalProfitLoss;
        private final Double winRate;
        private final Double roi;
        
        public BettingStats(long totalBets, long pendingBets, long wonBets, long lostBets,
                          long pushedBets, BigDecimal totalStaked, BigDecimal totalProfitLoss,
                          Double winRate, Double roi) {
            this.totalBets = totalBets;
            this.pendingBets = pendingBets;
            this.wonBets = wonBets;
            this.lostBets = lostBets;
            this.pushedBets = pushedBets;
            this.totalStaked = totalStaked;
            this.totalProfitLoss = totalProfitLoss;
            this.winRate = winRate;
            this.roi = roi;
        }
        
        // Getters
        public long getTotalBets() { return totalBets; }
        public long getPendingBets() { return pendingBets; }
        public long getWonBets() { return wonBets; }
        public long getLostBets() { return lostBets; }
        public long getPushedBets() { return pushedBets; }
        public BigDecimal getTotalStaked() { return totalStaked; }
        public BigDecimal getTotalProfitLoss() { return totalProfitLoss; }
        public Double getWinRate() { return winRate; }
        public Double getRoi() { return roi; }
    }
}