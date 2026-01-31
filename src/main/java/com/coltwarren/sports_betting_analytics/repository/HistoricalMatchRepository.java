package com.coltwarren.sports_betting_analytics.repository;

import com.coltwarren.sports_betting_analytics.model.historical.HistoricalMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Historical Match Repository
 *
 * Data access for historical match data.
 * Supports querying by league, season, date range, and more.
 */
@Repository
public interface HistoricalMatchRepository extends JpaRepository<HistoricalMatch, Long> {

    List<HistoricalMatch> findByLeague(String league);

    List<HistoricalMatch> findBySeason(String season);

    List<HistoricalMatch> findByLeagueAndSeason(String league, String season);

    List<HistoricalMatch> findByMatchDateBetween(LocalDate start, LocalDate end);

    List<HistoricalMatch> findByLeagueAndMatchDateBetweenOrderByMatchDateAsc(
        String league, LocalDate start, LocalDate end);

    @Query("SELECT DISTINCT h.league FROM HistoricalMatch h")
    List<String> findAllLeagues();

    @Query("SELECT DISTINCT h.season FROM HistoricalMatch h WHERE h.league = ?1 ORDER BY h.season DESC")
    List<String> findSeasonsByLeague(String league);

    @Query("SELECT COUNT(h) FROM HistoricalMatch h WHERE h.league = ?1")
    Long countByLeague(String league);

    @Query("SELECT h FROM HistoricalMatch h WHERE h.league = ?1 ORDER BY h.matchDate ASC")
    List<HistoricalMatch> findByLeagueOrderByDateAsc(String league);

    // For finding value bets in history
    @Query("SELECT h FROM HistoricalMatch h WHERE h.homeOdds > ?1 AND h.result = 'H'")
    List<HistoricalMatch> findHomeUnderdogWins(Double minOdds);

    @Query("SELECT h FROM HistoricalMatch h WHERE h.awayOdds > ?1 AND h.result = 'A'")
    List<HistoricalMatch> findAwayUnderdogWins(Double minOdds);

    // Find draws (often undervalued)
    @Query("SELECT h FROM HistoricalMatch h WHERE h.result = 'D' AND h.drawOdds > ?1")
    List<HistoricalMatch> findDrawsAboveOdds(Double minOdds);

    // Find high-scoring games
    @Query("SELECT h FROM HistoricalMatch h WHERE (h.homeGoals + h.awayGoals) > ?1")
    List<HistoricalMatch> findHighScoringGames(Integer minGoals);

    // Count by result type
    @Query("SELECT h.result, COUNT(h) FROM HistoricalMatch h WHERE h.league = ?1 GROUP BY h.result")
    List<Object[]> countResultsByLeague(String league);

    // Average odds by league
    @Query("SELECT AVG(h.homeOdds), AVG(h.drawOdds), AVG(h.awayOdds) FROM HistoricalMatch h WHERE h.league = ?1")
    List<Object[]> averageOddsByLeague(String league);

    // Check if data exists for league/season
    boolean existsByLeagueAndSeason(String league, String season);
}
