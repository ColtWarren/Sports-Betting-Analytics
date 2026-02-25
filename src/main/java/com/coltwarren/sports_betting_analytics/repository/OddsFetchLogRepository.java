package com.coltwarren.sports_betting_analytics.repository;

import com.coltwarren.sports_betting_analytics.model.odds.OddsFetchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OddsFetchLogRepository extends JpaRepository<OddsFetchLog, Long> {

    List<OddsFetchLog> findByStatusOrderByCreatedAtDesc(String status);

    List<OddsFetchLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);

    List<OddsFetchLog> findBySportAndCreatedAtAfterOrderByCreatedAtDesc(String sport, LocalDateTime since);
}
