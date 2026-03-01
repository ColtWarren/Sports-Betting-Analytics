package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.Bet;
import com.coltwarren.sports_betting_analytics.model.BetStatus;
import com.coltwarren.sports_betting_analytics.model.BetType;
import com.coltwarren.sports_betting_analytics.model.Sport;
import com.coltwarren.sports_betting_analytics.model.User;
import com.coltwarren.sports_betting_analytics.repository.BetRepository;
import org.springframework.data.domain.PageRequest;
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
        
        Bet bet = new Bet(Sport.valueOf(sport), eventName, BetType.valueOf(betType), selection, stake, odds, sportsbookName);
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
    private static final int DEFAULT_BET_LIMIT = 500;

    public List<Bet> getAllBets() {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return List.of();
        return betRepository.findByUserIdOrderByPlacedAtDesc(userId, PageRequest.of(0, DEFAULT_BET_LIMIT));
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
        return betRepository.findByUserIdAndStatus(userId, BetStatus.PENDING);
    }
    
    /**
     * Get all settled bets (won, lost, pushed)
     *
     * @return List of settled bets
     */
    public List<Bet> getSettledBets() {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return List.of();
        return betRepository.findByUserIdAndStatusIn(userId, List.of(BetStatus.WON, BetStatus.LOST, BetStatus.PUSH));
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
        return betRepository.findByUserIdAndSportsbookName(userId, sportsbookName);
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
        return betRepository.findByUserIdAndSport(userId, Sport.valueOf(sport));
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
        return betRepository.findByUserIdAndBetType(userId, BetType.valueOf(betType));
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
        return betRepository.findByUserIdAndPlacedAtBetween(userId, start, end);
    }
    
    /**
     * Get recent bets
     * 
     * @return List of recent bets (ordered by date)
     */
    public List<Bet> getRecentBets() {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return List.of();
        return betRepository.findByUserIdOrderByPlacedAtDesc(userId, PageRequest.of(0, 50));
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
     * Calculate total profit/loss across all bets (single DB aggregate query).
     */
    public BigDecimal calculateTotalProfitLoss() {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return BigDecimal.ZERO;
        BigDecimal result = betRepository.calculateTotalProfitLossByUserId(userId);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Calculate win rate (percentage of bets won) via DB aggregate.
     *
     * @return Win rate as percentage (0-100)
     */
    public Double calculateWinRate() {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return 0.0;
        Double rate = betRepository.calculateWinRateByUserId(userId);
        return rate != null ? rate * 100 : 0.0;
    }

    /**
     * Calculate ROI (Return on Investment) via DB aggregate.
     *
     * @return ROI as percentage (e.g., 5.0 = 5%)
     */
    public Double calculateROI() {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return 0.0;
        Double roi = betRepository.calculateROIByUserId(userId);
        return roi != null ? roi * 100 : 0.0;
    }

    /**
     * Calculate total amount staked via DB aggregate.
     */
    public BigDecimal calculateTotalStaked() {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return BigDecimal.ZERO;
        BigDecimal result = betRepository.calculateTotalStakedByUserId(userId);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Get count of bets by status via DB COUNT query.
     */
    public long countBetsByStatus(BetStatus status) {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) return 0;
        return betRepository.countByUserIdAndStatus(userId, status);
    }

    /**
     * Get comprehensive betting statistics.
     * Uses DB-level aggregate queries — no entity loading required.
     * Before: 8 separate queries each loading ALL bets into memory.
     * After:  6 lightweight COUNT/SUM queries, zero entity hydration.
     */
    public BettingStats getComprehensiveStats() {
        Long userId = userContextService.getCurrentUserId();
        if (userId == null) {
            return new BettingStats(0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0.0);
        }

        return new BettingStats(
            betRepository.countByUserId(userId),
            betRepository.countByUserIdAndStatus(userId, BetStatus.PENDING),
            betRepository.countByUserIdAndStatus(userId, BetStatus.WON),
            betRepository.countByUserIdAndStatus(userId, BetStatus.LOST),
            betRepository.countByUserIdAndStatus(userId, BetStatus.PUSH),
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
        
        if (bet.getSport() == null) {
            throw new IllegalArgumentException("Sport cannot be empty");
        }
        
        if (bet.getEventName() == null || bet.getEventName().trim().isEmpty()) {
            throw new IllegalArgumentException("Event name cannot be empty");
        }
        
        if (bet.getBetType() == null) {
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