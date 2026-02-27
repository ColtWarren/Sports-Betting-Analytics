package com.coltwarren.sports_betting_analytics.repository;

import com.coltwarren.sports_betting_analytics.model.odds.CachedOdds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CachedOddsRepository extends JpaRepository<CachedOdds, Long> {

    Optional<CachedOdds> findBySportAndLeagueAndEventId(String sport, String league, String eventId);

    List<CachedOdds> findBySportAndExpiresAtAfter(String sport, LocalDateTime now);

    List<CachedOdds> findBySportAndLeagueAndExpiresAtAfter(String sport, String league, LocalDateTime now);

    List<CachedOdds> findByLeagueAndCommenceTimeAfter(String league, LocalDateTime now);

    @Query("SELECT c FROM CachedOdds c WHERE c.sport = :sport AND c.commenceTime > :now ORDER BY c.commenceTime")
    List<CachedOdds> findUpcomingBySport(@Param("sport") String sport, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM CachedOdds c WHERE c.expiresAt < :cutoff")
    void deleteExpired(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(LocalDateTime now);
}
