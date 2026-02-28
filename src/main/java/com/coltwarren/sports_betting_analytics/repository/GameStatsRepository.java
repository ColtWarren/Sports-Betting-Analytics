package com.coltwarren.sports_betting_analytics.repository;

import com.coltwarren.sports_betting_analytics.model.GameStats;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameStatsRepository extends JpaRepository<GameStats, Long> {
    
    // Find games by team (home or away)
    @Query("SELECT g FROM GameStats g WHERE g.homeTeam = :team OR g.awayTeam = :team ORDER BY g.gameTime DESC")
    List<GameStats> findByTeam(@Param("team") String team, Pageable pageable);
    
    // Find recent games by team
    @Query("SELECT g FROM GameStats g WHERE (g.homeTeam = :team OR g.awayTeam = :team) AND g.gameTime >= :since ORDER BY g.gameTime DESC")
    List<GameStats> findRecentByTeam(@Param("team") String team, @Param("since") LocalDateTime since);
    
    // Get ATS record for a team
    @Query("SELECT COUNT(g) FROM GameStats g WHERE (g.homeTeam = :team AND g.homeCoveredSpread = true) OR (g.awayTeam = :team AND g.awayCoveredSpread = true)")
    Long countATSWins(@Param("team") String team);
    
    @Query("SELECT COUNT(g) FROM GameStats g WHERE ((g.homeTeam = :team AND g.homeCoveredSpread = false) OR (g.awayTeam = :team AND g.awayCoveredSpread = false)) AND g.homeCoveredSpread IS NOT NULL")
    Long countATSLosses(@Param("team") String team);
    
    // Get over/under record
    @Query("SELECT COUNT(g) FROM GameStats g WHERE (g.homeTeam = :team OR g.awayTeam = :team) AND g.wentOver = true")
    Long countOvers(@Param("team") String team);
    
    @Query("SELECT COUNT(g) FROM GameStats g WHERE (g.homeTeam = :team OR g.awayTeam = :team) AND g.wentOver = false")
    Long countUnders(@Param("team") String team);
    
    // Head to head
    @Query("SELECT g FROM GameStats g WHERE (g.homeTeam = :team1 AND g.awayTeam = :team2) OR (g.homeTeam = :team2 AND g.awayTeam = :team1) ORDER BY g.gameTime DESC")
    List<GameStats> findHeadToHead(@Param("team1") String team1, @Param("team2") String team2, Pageable pageable);
}
