package com.coltwarren.sports_betting_analytics.repository;

import com.coltwarren.sports_betting_analytics.model.W2GForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface W2GFormRepository extends JpaRepository<W2GForm, Long> {

    // Find all W-2G forms for a specific tax year
    List<W2GForm> findByTaxYear(Integer taxYear);

    // Find W-2G form for a specific bet
    W2GForm findByBetId(Long betId);

    // Find all unreceived forms for a tax year
    List<W2GForm> findByTaxYearAndFormReceivedFalse(Integer taxYear);

    // Count unreceived forms
    @Query("SELECT COUNT(w) FROM W2GForm w WHERE w.taxYear = ?1 AND w.formReceived = false")
    Long countUnreceivedForms(Integer taxYear);

    // Get total W-2G winnings for a tax year
    @Query("SELECT COALESCE(SUM(w.netWinnings), 0) FROM W2GForm w WHERE w.taxYear = ?1")
    java.math.BigDecimal getTotalW2GWinnings(Integer taxYear);

    // ============================================
    // USER-SCOPED QUERIES
    // ============================================

    List<W2GForm> findByUserId(Long userId);

    List<W2GForm> findByUserIdAndTaxYear(Long userId, Integer taxYear);

    Optional<W2GForm> findByIdAndUserId(Long id, Long userId);
}
