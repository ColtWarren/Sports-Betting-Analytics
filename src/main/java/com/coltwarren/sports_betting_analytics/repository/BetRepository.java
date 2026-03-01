package com.coltwarren.sports_betting_analytics.repository;

import com.coltwarren.sports_betting_analytics.model.Bet;
import com.coltwarren.sports_betting_analytics.model.BetStatus;
import com.coltwarren.sports_betting_analytics.model.BetType;
import com.coltwarren.sports_betting_analytics.model.Sport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    // ============================================

    List<Bet> findByStatus(BetStatus status);

    List<Bet> findBySportsbookName(String sportsbookName);

    List<Bet> findBySport(Sport sport);

    List<Bet> findByBetType(BetType betType);

    List<Bet> findByPlacedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Bet> findBySettledAtBetween(LocalDateTime start, LocalDateTime end);

    default List<Bet> findPendingBets() {
        return findByStatus(BetStatus.PENDING);
    }

    List<Bet> findByStatusIn(List<BetStatus> statuses);

    List<Bet> findByBeatClosingLine(Boolean beatClosingLine);

    // ============================================
    // USER-SCOPED QUERIES
    // ============================================

    List<Bet> findByUserId(Long userId);

    List<Bet> findByUserIdAndStatus(Long userId, BetStatus status);

    List<Bet> findByUserIdAndStatusIn(Long userId, List<BetStatus> statuses);

    List<Bet> findByUserIdAndSportsbookName(Long userId, String sportsbookName);

    List<Bet> findByUserIdAndSport(Long userId, Sport sport);

    List<Bet> findByUserIdAndBetType(Long userId, BetType betType);

    List<Bet> findByUserIdAndPlacedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    List<Bet> findByUserIdOrderByPlacedAtDesc(Long userId, Pageable pageable);

    Optional<Bet> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") BetStatus status);

    @Query("SELECT SUM(b.profitLoss) FROM Bet b WHERE b.user.id = :userId AND b.profitLoss IS NOT NULL")
    BigDecimal calculateTotalProfitLossByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(b.stake) FROM Bet b WHERE b.user.id = :userId AND b.stake IS NOT NULL")
    BigDecimal calculateTotalStakedByUserId(@Param("userId") Long userId);

    @Query("SELECT CAST(COUNT(CASE WHEN b.status = 'WON' THEN 1 END) AS double) / NULLIF(COUNT(b), 0) FROM Bet b WHERE b.user.id = :userId AND b.status IN ('WON', 'LOST')")
    Double calculateWinRateByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(b.profitLoss) / NULLIF(SUM(b.stake), 0) FROM Bet b WHERE b.user.id = :userId AND b.profitLoss IS NOT NULL")
    Double calculateROIByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);

    List<Bet> findByStakeGreaterThanEqual(BigDecimal stake);

    long countByStatus(BetStatus status);

    long countBySportsbookName(String sportsbookName);

    // ============================================
    // CUSTOM QUERIES WITH @Query ANNOTATION
    // ============================================

    @Query("SELECT SUM(b.profitLoss) FROM Bet b WHERE b.profitLoss IS NOT NULL")
    BigDecimal calculateTotalProfitLoss();

    @Query("SELECT SUM(b.profitLoss) FROM Bet b WHERE b.sportsbookName = :sportsbookName AND b.profitLoss IS NOT NULL")
    BigDecimal calculateProfitLossBySportsbook(String sportsbookName);

    @Query("SELECT SUM(b.profitLoss) FROM Bet b WHERE b.sport = :sport AND b.profitLoss IS NOT NULL")
    BigDecimal calculateProfitLossBySport(@Param("sport") Sport sport);

    @Query("SELECT CAST(COUNT(CASE WHEN b.status = 'WON' THEN 1 END) AS double) / NULLIF(COUNT(b), 0) FROM Bet b WHERE b.status IN ('WON', 'LOST')")
    Double calculateWinRate();

    @Query("SELECT SUM(b.profitLoss) / NULLIF(SUM(b.stake), 0) FROM Bet b WHERE b.profitLoss IS NOT NULL")
    Double calculateROI();

    @Query("SELECT SUM(b.stake) FROM Bet b")
    BigDecimal calculateTotalStaked();

    @Query("SELECT b FROM Bet b ORDER BY b.placedAt DESC")
    List<Bet> findRecentBets(Pageable pageable);

    @Query("SELECT b.sportsbookName FROM Bet b WHERE b.profitLoss IS NOT NULL GROUP BY b.sportsbookName ORDER BY SUM(b.profitLoss) DESC")
    List<String> findBestPerformingSportsbooks();

    @Query("SELECT b.sport FROM Bet b WHERE b.profitLoss IS NOT NULL GROUP BY b.sport ORDER BY SUM(b.profitLoss) DESC")
    List<Sport> findMostProfitableSports();

    // ============================================
    // USER-SCOPED AGGREGATE QUERIES (#43)
    // ============================================

    @Query("SELECT SUM(b.profitLoss) FROM Bet b WHERE b.user.id = :userId AND b.sportsbookName = :sportsbookName AND b.profitLoss IS NOT NULL")
    BigDecimal calculateProfitLossBySportsbookByUserId(@Param("userId") Long userId, @Param("sportsbookName") String sportsbookName);

    @Query("SELECT SUM(b.profitLoss) FROM Bet b WHERE b.user.id = :userId AND b.sport = :sport AND b.profitLoss IS NOT NULL")
    BigDecimal calculateProfitLossBySportByUserId(@Param("userId") Long userId, @Param("sport") Sport sport);

    @Query("SELECT b.sportsbookName FROM Bet b WHERE b.user.id = :userId AND b.profitLoss IS NOT NULL GROUP BY b.sportsbookName ORDER BY SUM(b.profitLoss) DESC")
    List<String> findBestPerformingSportsbooksByUserId(@Param("userId") Long userId);

    @Query("SELECT b.sport FROM Bet b WHERE b.user.id = :userId AND b.profitLoss IS NOT NULL GROUP BY b.sport ORDER BY SUM(b.profitLoss) DESC")
    List<Sport> findMostProfitableSportsByUserId(@Param("userId") Long userId);

    // ============================================
    // JOIN FETCH QUERIES (#44 - N+1 prevention)
    // ============================================

    @Query("SELECT b FROM Bet b LEFT JOIN FETCH b.user WHERE b.user.id = :userId AND b.settledAt BETWEEN :start AND :end")
    List<Bet> findByUserIdAndSettledAtBetweenWithUser(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
