package com.coltwarren.sports_betting_analytics.repository;

import com.coltwarren.sports_betting_analytics.model.Bankroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankrollRepository extends JpaRepository<Bankroll, Long> {
    
    List<Bankroll> findAllByOrderByRecordedAtDesc();
    
    @Query("SELECT b FROM Bankroll b ORDER BY b.recordedAt DESC LIMIT 1")
    Optional<Bankroll> findLatestBankroll();
    
    @Query("SELECT SUM(b.amount) FROM Bankroll b WHERE b.transactionType = 'DEPOSIT'")
    BigDecimal getTotalDeposits();
    
    @Query("SELECT SUM(b.amount) FROM Bankroll b WHERE b.transactionType = 'WITHDRAWAL'")
    BigDecimal getTotalWithdrawals();
}
