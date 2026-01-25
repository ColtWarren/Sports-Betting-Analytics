package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.Sportsbook;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Sportsbook Service
 * Manages the 8 Missouri-licensed sportsbooks
 *
 * LEGAL COMPLIANCE: Only Missouri Gaming Commission licensed sportsbooks
 */
@Service
public class SportsbookService {

    // Missouri-Licensed Sportsbooks (matching MultiSportOddsService filter)
    private static final List<Sportsbook> SPORTSBOOKS = new ArrayList<>();

    static {
        // Initialize the 8 Missouri-licensed sportsbooks

        SPORTSBOOKS.add(new Sportsbook(
            "DraftKings",
            4.5,
            "Bet $5, Get $150 in Bonus Bets",
            "https://www.draftkings.com",
            "Industry leader with extensive betting markets, competitive odds, and excellent mobile app. Best for daily fantasy sports crossover.",
            "🟢",
            "Best Overall"
        ));

        SPORTSBOOKS.add(new Sportsbook(
            "FanDuel",
            4.5,
            "Bet $5, Get $150 in Bonus Bets",
            "https://www.fanduel.com",
            "Top-rated sportsbook with user-friendly interface, Same Game Parlays, and competitive pricing. Great promotions and rewards program.",
            "🔵",
            "Best App"
        ));

        SPORTSBOOKS.add(new Sportsbook(
            "BetMGM",
            4.0,
            "Up to $1,500 First Bet Offer",
            "https://www.betmgm.com",
            "MGM Resorts-backed sportsbook with excellent live betting features, parlay boosts, and casino integration. Strong odds on major sports.",
            "🟡",
            "Best Live Betting"
        ));

        SPORTSBOOKS.add(new Sportsbook(
            "Caesars Sportsbook",
            4.0,
            "Up to $1,000 First Bet on Caesars",
            "https://www.caesars.com/sportsbook",
            "Caesars Entertainment sportsbook with robust rewards program, competitive odds, and excellent customer service. Great for high rollers.",
            "🏛️",
            "Best Rewards"
        ));

        SPORTSBOOKS.add(new Sportsbook(
            "bet365",
            4.5,
            "Bet $1, Get $365 in Bonus Bets",
            "https://www.bet365.com",
            "International powerhouse with comprehensive betting options, early cashout features, and competitive lines. Best for soccer and international sports.",
            "🌐",
            "Best International"
        ));

        SPORTSBOOKS.add(new Sportsbook(
            "Fanatics Sportsbook",
            3.5,
            "Up to $1,000 in Bonus Bets",
            "https://www.fanaticssportsbook.com",
            "New sportsbook backed by Fanatics with innovative FanCash rewards program. Seamless integration with sports merchandise and memorabilia.",
            "⚡",
            "Best Rewards Program"
        ));

        SPORTSBOOKS.add(new Sportsbook(
            "Circa Sports",
            4.0,
            "Risk-Free Bet up to $1,000",
            "https://www.circasports.com",
            "Las Vegas-based sportsbook known for sharp lines and industry-leading limits. Popular with professional bettors and sharp players.",
            "🎰",
            "Best for Sharp Bettors"
        ));

        SPORTSBOOKS.add(new Sportsbook(
            "theScore Bet",
            3.5,
            "First Bet up to $500",
            "https://www.thescorebet.com",
            "Sports media + betting combined for seamless experience. Real-time scores and betting in one app. Great for in-game betting and live updates.",
            "📱",
            "Best for Live Scores"
        ));
    }

    /**
     * Get all Missouri-licensed sportsbooks
     * @return List of all 8 licensed sportsbooks
     */
    public List<Sportsbook> getAllSportsbooks() {
        return new ArrayList<>(SPORTSBOOKS);
    }

    /**
     * Get sportsbook by name
     * @param name Sportsbook name
     * @return Sportsbook or null if not found
     */
    public Sportsbook getByName(String name) {
        return SPORTSBOOKS.stream()
            .filter(sb -> sb.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get total count of licensed sportsbooks
     * @return Count (should always be 8 for Missouri)
     */
    public int getCount() {
        return SPORTSBOOKS.size();
    }

    /**
     * Get average rating across all sportsbooks
     * @return Average rating
     */
    public double getAverageRating() {
        return SPORTSBOOKS.stream()
            .mapToDouble(Sportsbook::getRating)
            .average()
            .orElse(0.0);
    }
}
