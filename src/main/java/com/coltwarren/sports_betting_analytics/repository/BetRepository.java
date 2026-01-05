package com.coltwarren.sports_betting_analytics.repository;

import com.coltwarren.sports_betting_analytics.model.Bet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BetRepository - Data Access Layer for Bet Entity
 * 
 * Spring Data JPA automatically implements these methods!
 * No need to write SQL - Spring generates it for you.
 * 
 * @author Colt Warren
 * @version 1.0
 */
@Repository
public interface BetRepository extends JpaRepository<Bet, Long> {
    
    // ============================================
    // AUTOMATIC CRUD METHODS (FROM JpaRepository)
    // ============================================
    // save(Bet bet) - Insert or Update
    // findById(Long id) - Find by ID
    // findAll() - Get all bets
    // deleteById(Long id) - Delete by ID
    // count() - Count all bets
    
    // ============================================
    // CUSTOM QUERY METHODS
    // Spring Data JPA generates SQL from method names!
    // ============================================
    
    /**
     * Find all bets with a specific status
     * Generated SQL: SELECT * FROM bets WHERE status = ?
     * 
     * @param status - "PENDING", "WON", "LOST", "PUSH", "VOID"
     * @return List of bets with that status
     */
    List<Bet> findByStatus(String status);
    
    /**
     * Find all bets from a specific sportsbook
     * Generated SQL: SELECT * FROM bets WHERE sportsbook_name = ?
     * 
     * @param sportsbookName - "FANDUEL", "DRAFTKINGS", etc.
     * @return List of bets from that sportsbook
     */
    List<Bet> findBySportsbookName(String sportsbookName);
    
    /**
     * Find all bets on a specific sport
     * Generated SQL: SELECT * FROM bets WHERE sport = ?
     * 
     * @param sport - "NFL", "NCAAF", "NBA", etc.
     * @return List of bets on that sport
     */
    List<Bet> findBySport(String sport);
    
    /**
     * Find all bets of a specific type
     * Generated SQL: SELECT * FROM bets WHERE bet_type = ?
     * 
     * @param betType - "MONEYLINE", "SPREAD", "TOTAL_OVER", "TOTAL_UNDER"
     * @return List of bets of that type
     */
    List<Bet> findByBetType(String betType);
    
    /**
     * Find bets placed within a date range
     * Generated SQL: SELECT * FROM bets WHERE placed_at BETWEEN ? AND ?
     * 
     * @param start - Start date/time
     * @param end - End date/time
     * @return List of bets placed in that range
     */
    List<Bet> findByPlacedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find all pending bets (convenience method)
     * Same as findByStatus("PENDING")
     * 
     * @return List of pending bets
     */
    default List<Bet> findPendingBets() {
        return findByStatus("PENDING");
    }
    
    /**
     * Find all settled bets (won, lost, or pushed)
     * Generated SQL: SELECT * FROM bets WHERE status IN ('WON', 'LOST', 'PUSH')
     * 
     * @return List of settled bets
     */
    List<Bet> findByStatusIn(List<String> statuses);
    
    /**
     * Find bets where we beat the closing line
     * Generated SQL: SELECT * FROM bets WHERE beat_closing_line = true
     * 
     * @param beatClosingLine - true for bets that beat closing line
     * @return List of bets that beat (or didn't beat) closing line
     */
    List<Bet> findByBeatClosingLine(Boolean beatClosingLine);
    
    /**
     * Find bets with stake greater than or equal to amount
     * Generated SQL: SELECT * FROM bets WHERE stake >= ?
     * 
     * @param stake - Minimum stake amount
     * @return List of bets with stake >= amount
     */
    List<Bet> findByStakeGreaterThanEqual(BigDecimal stake);
    
    /**
     * Count bets by status
     * Generated SQL: SELECT COUNT(*) FROM bets WHERE status = ?
     * 
     * @param status - Status to count
     * @return Number of bets with that status
     */
    long countByStatus(String status);
    
    /**
     * Count bets by sportsbook
     * Generated SQL: SELECT COUNT(*) FROM bets WHERE sportsbook_name = ?
     * 
     * @param sportsbookName - Sportsbook to count
     * @return Number of bets at that sportsbook
     */
    long countBySportsbookName(String sportsbookName);
    
    // ============================================
    // CUSTOM QUERIES WITH @Query ANNOTATION
    // For complex queries that can't be expressed with method names
    // ============================================
    
    /**
     * Calculate total profit/loss across all settled bets
     * Custom JPQL query
     * 
     * @return Total profit (positive) or loss (negative)
     */
    @Query("SELECT SUM(b.profitLoss) FROM Bet b WHERE b.profitLoss IS NOT NULL")
    BigDecimal calculateTotalProfitLoss();
    
    /**
     * Calculate total profit/loss for a specific sportsbook
     * 
     * @param sportsbookName - Sportsbook name
     * @return Total profit/loss for that sportsbook
     */
    @Query("SELECT SUM(b.profitLoss) FROM Bet b WHERE b.sportsbookName = :sportsbookName AND b.profitLoss IS NOT NULL")
    BigDecimal calculateProfitLossBySportsbook(String sportsbookName);
    
    /**
     * Calculate total profit/loss for a specific sport
     * 
     * @param sport - Sport name
     * @return Total profit/loss for that sport
     */
    @Query("SELECT SUM(b.profitLoss) FROM Bet b WHERE b.sport = :sport AND b.profitLoss IS NOT NULL")
    BigDecimal calculateProfitLossBySport(String sport);
    
    /**
     * Get win rate (percentage of bets won)
     * Returns value between 0 and 1 (e.g., 0.55 = 55% win rate)
     * 
     * @return Win rate as decimal
     */
    @Query("SELECT CAST(COUNT(CASE WHEN b.status = 'WON' THEN 1 END) AS double) / COUNT(b) FROM Bet b WHERE b.status IN ('WON', 'LOST')")
    Double calculateWinRate();
    
    /**
     * Calculate ROI (Return on Investment)
     * ROI = Total Profit / Total Stake
     * 
     * @return ROI as decimal (e.g., 0.05 = 5% ROI)
     */
    @Query("SELECT SUM(b.profitLoss) / SUM(b.stake) FROM Bet b WHERE b.profitLoss IS NOT NULL")
    Double calculateROI();
    
    /**
     * Get total amount staked across all bets
     * 
     * @return Total stake amount
     */
    @Query("SELECT SUM(b.stake) FROM Bet b")
    BigDecimal calculateTotalStaked();
    
    /**
     * Find recent bets (last N bets, ordered by placement date)
     * 
     * @param limit - Number of recent bets to retrieve
     * @return List of recent bets
     */
    @Query("SELECT b FROM Bet b ORDER BY b.placedAt DESC")
    List<Bet> findRecentBets();
    
    /**
     * Find best performing sportsbook by profit
     * Returns sportsbook names ordered by total profit (highest first)
     * 
     * @return List of sportsbook names ordered by profit
     */
    @Query("SELECT b.sportsbookName FROM Bet b WHERE b.profitLoss IS NOT NULL GROUP BY b.sportsbookName ORDER BY SUM(b.profitLoss) DESC")
    List<String> findBestPerformingSportsbooks();
    
    /**
     * Find most profitable sport
     * Returns sport names ordered by total profit (highest first)
     * 
     * @return List of sport names ordered by profit
     */
    @Query("SELECT b.sport FROM Bet b WHERE b.profitLoss IS NOT NULL GROUP BY b.sport ORDER BY SUM(b.profitLoss) DESC")
    List<String> findMostProfitableSports();
}
    
