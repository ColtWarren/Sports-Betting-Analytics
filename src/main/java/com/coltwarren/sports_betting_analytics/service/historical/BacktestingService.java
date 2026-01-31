package com.coltwarren.sports_betting_analytics.service.historical;

import com.coltwarren.sports_betting_analytics.model.historical.*;
import com.coltwarren.sports_betting_analytics.repository.HistoricalMatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Backtesting Service
 *
 * Runs betting strategies against historical data to evaluate performance.
 * Supports Kelly Criterion with various fraction sizes and custom filters.
 */
@Service
@Slf4j
public class BacktestingService {

    @Autowired
    private HistoricalMatchRepository matchRepository;

    // Default settings
    private static final double DEFAULT_BANKROLL = 1000.0;
    private static final double MAX_KELLY = 0.05;      // Max 5% of bankroll per bet
    private static final double MIN_EDGE = 0.02;       // Minimum 2% edge required
    private static final double MIN_ODDS = 1.50;       // Minimum odds to consider
    private static final double MAX_ODDS = 10.0;       // Maximum odds to consider

    /**
     * Run Kelly Criterion backtest on a league
     */
    public BacktestResult runKellyBacktest(String league, Double kellyFraction,
                                            Double startingBankroll, Double minEdge) {

        // Use defaults if not provided
        double bankroll = startingBankroll != null ? startingBankroll : DEFAULT_BANKROLL;
        double fraction = kellyFraction != null ? kellyFraction : 0.25; // Quarter Kelly default
        double minEdgeRequired = minEdge != null ? minEdge : MIN_EDGE;

        BacktestResult result = new BacktestResult();
        result.setStrategyName(getStrategyName(fraction));
        result.setLeague(league);
        result.setStartingBankroll(bankroll);

        // Get all matches ordered by date
        List<HistoricalMatch> matches = matchRepository.findByLeagueOrderByDateAsc(league);
        if (matches.isEmpty()) {
            log.warn("No matches found for league: {}", league);
            return result;
        }

        result.setTotalMatches(matches.size());
        result.setDateRange(formatDateRange(matches));

        double currentBankroll = bankroll;
        double maxBankroll = bankroll;
        double maxDrawdown = 0.0;
        List<Double> dailyReturns = new ArrayList<>();
        Map<String, MonthlyResult> monthlyMap = new LinkedHashMap<>();

        for (HistoricalMatch match : matches) {
            // Skip if missing essential data
            if (match.getHomeOdds() == null || match.getResult() == null) continue;

            // Find value bet
            ValueBet valueBet = findValueBet(match, minEdgeRequired);
            if (valueBet == null) continue;

            // Calculate Kelly stake
            double kellyPercent = calculateKelly(valueBet.probability, valueBet.odds);
            double adjustedKelly = Math.min(kellyPercent * fraction, MAX_KELLY);

            if (adjustedKelly <= 0) continue;

            double stake = currentBankroll * adjustedKelly;
            if (stake < 1.0) continue; // Minimum $1 bet

            // Create bet record
            BacktestBet bet = new BacktestBet();
            bet.setDate(match.getMatchDate());
            bet.setHomeTeam(match.getHomeTeam());
            bet.setAwayTeam(match.getAwayTeam());
            bet.setBetType(valueBet.betType);
            bet.setOdds(valueBet.odds);
            bet.setStake(stake);
            bet.setKellyPercent(adjustedKelly * 100);
            bet.setExpectedValue(valueBet.edge * stake);
            bet.setConfidence(valueBet.probability * 100);

            // Determine outcome
            boolean won = didBetWin(match, valueBet.betType);
            bet.setWon(won);
            bet.setActualResult(match.getResult());

            double profit;
            if (won) {
                profit = stake * (valueBet.odds - 1);
                result.setWins(result.getWins() + 1);
            } else {
                profit = -stake;
                result.setLosses(result.getLosses() + 1);
            }

            bet.setProfit(profit);
            currentBankroll += profit;
            bet.setBankrollAfter(currentBankroll);

            // Track bet type counts
            switch (valueBet.betType) {
                case "H" -> result.setHomeBets(result.getHomeBets() + 1);
                case "D" -> result.setDrawBets(result.getDrawBets() + 1);
                case "A" -> result.setAwayBets(result.getAwayBets() + 1);
            }

            result.getBets().add(bet);
            result.setTotalBets(result.getTotalBets() + 1);

            // Track drawdown
            if (currentBankroll > maxBankroll) {
                maxBankroll = currentBankroll;
            }
            double drawdown = (maxBankroll - currentBankroll) / maxBankroll * 100;
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown;
            }

            // Track daily returns for Sharpe
            dailyReturns.add(profit / stake);

            // Track monthly results
            String monthKey = YearMonth.from(match.getMatchDate()).toString();
            MonthlyResult monthly = monthlyMap.computeIfAbsent(monthKey, k -> {
                MonthlyResult mr = new MonthlyResult();
                mr.setMonth(k);
                return mr;
            });
            monthly.setBets(monthly.getBets() + 1);
            if (won) monthly.setWins(monthly.getWins() + 1);
            monthly.setProfit(monthly.getProfit() + profit);
            monthly.setEndingBankroll(currentBankroll);
        }

        // Calculate final stats
        result.setFinalBankroll(currentBankroll);
        result.setMaxDrawdown(maxDrawdown);
        result.calculateStats();

        // Calculate average odds
        if (!result.getBets().isEmpty()) {
            double avgOdds = result.getBets().stream()
                .mapToDouble(BacktestBet::getOdds)
                .average()
                .orElse(0);
            result.setAvgOdds(avgOdds);

            double avgKelly = result.getBets().stream()
                .mapToDouble(BacktestBet::getKellyPercent)
                .average()
                .orElse(0);
            result.setAvgKellyPercent(avgKelly);
        }

        // Calculate Sharpe Ratio
        result.setSharpeRatio(calculateSharpeRatio(dailyReturns));

        // Calculate monthly ROIs
        for (MonthlyResult monthly : monthlyMap.values()) {
            if (monthly.getBets() > 0) {
                double monthlyRoi = monthly.getProfit() / bankroll * 100;
                monthly.setRoi(monthlyRoi);
            }
        }
        result.setMonthlyResults(new ArrayList<>(monthlyMap.values()));

        log.info("Backtest complete: {}", result.getSummary());
        return result;
    }

    /**
     * Find value bet in a match using implied probabilities
     */
    private ValueBet findValueBet(HistoricalMatch match, double minEdge) {
        // Calculate implied probabilities from odds
        double homeImplied = 1.0 / match.getHomeOdds();
        double drawImplied = match.getDrawOdds() != null ? 1.0 / match.getDrawOdds() : 0;
        double awayImplied = match.getAwayOdds() != null ? 1.0 / match.getAwayOdds() : 0;

        // Normalize (remove overround/margin)
        double total = homeImplied + drawImplied + awayImplied;
        if (total <= 0) return null;

        double homeProb = homeImplied / total;
        double drawProb = drawImplied / total;
        double awayProb = awayImplied / total;

        // Check each outcome for value
        // Using a simple model: assume true probability is slightly higher than implied
        // This simulates having an edge through superior information

        ValueBet best = null;

        // Home bet value check
        if (match.getHomeOdds() >= MIN_ODDS && match.getHomeOdds() <= MAX_ODDS) {
            double estimatedProb = estimateTrueProb(homeProb, "H", match);
            double edge = estimatedProb - (1.0 / match.getHomeOdds());
            if (edge >= minEdge && (best == null || edge > best.edge)) {
                best = new ValueBet("H", match.getHomeOdds(), estimatedProb, edge);
            }
        }

        // Draw bet value check
        if (match.getDrawOdds() != null &&
            match.getDrawOdds() >= MIN_ODDS && match.getDrawOdds() <= MAX_ODDS) {
            double estimatedProb = estimateTrueProb(drawProb, "D", match);
            double edge = estimatedProb - (1.0 / match.getDrawOdds());
            if (edge >= minEdge && (best == null || edge > best.edge)) {
                best = new ValueBet("D", match.getDrawOdds(), estimatedProb, edge);
            }
        }

        // Away bet value check
        if (match.getAwayOdds() != null &&
            match.getAwayOdds() >= MIN_ODDS && match.getAwayOdds() <= MAX_ODDS) {
            double estimatedProb = estimateTrueProb(awayProb, "A", match);
            double edge = estimatedProb - (1.0 / match.getAwayOdds());
            if (edge >= minEdge && (best == null || edge > best.edge)) {
                best = new ValueBet("A", match.getAwayOdds(), estimatedProb, edge);
            }
        }

        return best;
    }

    /**
     * Estimate true probability with simple adjustments
     * In real usage, this would use machine learning models
     */
    private double estimateTrueProb(double impliedProb, String betType, HistoricalMatch match) {
        // Simple adjustment: draws are historically undervalued
        // Home teams slightly overvalued, away teams slightly undervalued
        double adjustment = switch (betType) {
            case "H" -> -0.02;  // Home slightly overvalued
            case "D" -> 0.03;   // Draws undervalued
            case "A" -> 0.01;   // Away slightly undervalued
            default -> 0;
        };

        // Additional adjustment for extreme odds (longshots more undervalued)
        double odds = switch (betType) {
            case "H" -> match.getHomeOdds();
            case "D" -> match.getDrawOdds() != null ? match.getDrawOdds() : 3.0;
            case "A" -> match.getAwayOdds() != null ? match.getAwayOdds() : 3.0;
            default -> 2.0;
        };

        if (odds > 4.0) {
            adjustment += 0.02; // Longshots more undervalued
        }

        return Math.min(0.95, Math.max(0.05, impliedProb + adjustment));
    }

    /**
     * Calculate Kelly Criterion stake percentage
     */
    private double calculateKelly(double probability, double odds) {
        // Kelly formula: (bp - q) / b
        // where b = odds - 1, p = probability of winning, q = 1 - p
        double b = odds - 1;
        double p = probability;
        double q = 1 - p;

        double kelly = (b * p - q) / b;
        return Math.max(0, kelly);
    }

    /**
     * Check if bet won
     */
    private boolean didBetWin(HistoricalMatch match, String betType) {
        return betType.equals(match.getResult());
    }

    /**
     * Calculate Sharpe Ratio for risk-adjusted returns
     */
    private double calculateSharpeRatio(List<Double> returns) {
        if (returns.isEmpty()) return 0;

        double mean = returns.stream().mapToDouble(d -> d).average().orElse(0);
        double variance = returns.stream()
            .mapToDouble(r -> Math.pow(r - mean, 2))
            .average()
            .orElse(0);
        double stdDev = Math.sqrt(variance);

        if (stdDev == 0) return 0;

        // Annualized (assuming ~200 betting days per year)
        return (mean / stdDev) * Math.sqrt(200);
    }

    /**
     * Compare different Kelly strategies
     */
    public Map<String, BacktestResult> compareStrategies(String league) {
        Map<String, BacktestResult> results = new LinkedHashMap<>();

        // Quarter Kelly (conservative)
        results.put("Quarter Kelly", runKellyBacktest(league, 0.25, DEFAULT_BANKROLL, MIN_EDGE));

        // Half Kelly (moderate)
        results.put("Half Kelly", runKellyBacktest(league, 0.50, DEFAULT_BANKROLL, MIN_EDGE));

        // Full Kelly (aggressive)
        results.put("Full Kelly", runKellyBacktest(league, 1.0, DEFAULT_BANKROLL, MIN_EDGE));

        // Flat betting (1% per bet for comparison)
        results.put("Flat 1%", runFlatBacktest(league, 0.01, DEFAULT_BANKROLL, MIN_EDGE));

        return results;
    }

    /**
     * Run flat betting backtest for comparison
     */
    public BacktestResult runFlatBacktest(String league, double stakePercent,
                                           double startingBankroll, double minEdge) {
        BacktestResult result = new BacktestResult();
        result.setStrategyName(String.format("Flat %.0f%%", stakePercent * 100));
        result.setLeague(league);
        result.setStartingBankroll(startingBankroll);

        List<HistoricalMatch> matches = matchRepository.findByLeagueOrderByDateAsc(league);
        if (matches.isEmpty()) return result;

        result.setTotalMatches(matches.size());
        result.setDateRange(formatDateRange(matches));

        double currentBankroll = startingBankroll;
        double maxBankroll = startingBankroll;
        double maxDrawdown = 0.0;

        for (HistoricalMatch match : matches) {
            if (match.getHomeOdds() == null || match.getResult() == null) continue;

            ValueBet valueBet = findValueBet(match, minEdge);
            if (valueBet == null) continue;

            double stake = startingBankroll * stakePercent; // Flat stake
            if (stake > currentBankroll) continue; // Can't bet more than we have

            BacktestBet bet = new BacktestBet();
            bet.setDate(match.getMatchDate());
            bet.setHomeTeam(match.getHomeTeam());
            bet.setAwayTeam(match.getAwayTeam());
            bet.setBetType(valueBet.betType);
            bet.setOdds(valueBet.odds);
            bet.setStake(stake);
            bet.setKellyPercent(stakePercent * 100);

            boolean won = didBetWin(match, valueBet.betType);
            bet.setWon(won);
            bet.setActualResult(match.getResult());

            double profit = won ? stake * (valueBet.odds - 1) : -stake;
            bet.setProfit(profit);

            if (won) {
                result.setWins(result.getWins() + 1);
            } else {
                result.setLosses(result.getLosses() + 1);
            }

            currentBankroll += profit;
            bet.setBankrollAfter(currentBankroll);
            result.getBets().add(bet);
            result.setTotalBets(result.getTotalBets() + 1);

            // Track bet types
            switch (valueBet.betType) {
                case "H" -> result.setHomeBets(result.getHomeBets() + 1);
                case "D" -> result.setDrawBets(result.getDrawBets() + 1);
                case "A" -> result.setAwayBets(result.getAwayBets() + 1);
            }

            // Track drawdown
            if (currentBankroll > maxBankroll) {
                maxBankroll = currentBankroll;
            }
            double drawdown = (maxBankroll - currentBankroll) / maxBankroll * 100;
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown;
            }
        }

        result.setFinalBankroll(currentBankroll);
        result.setMaxDrawdown(maxDrawdown);
        result.calculateStats();

        if (!result.getBets().isEmpty()) {
            result.setAvgOdds(result.getBets().stream()
                .mapToDouble(BacktestBet::getOdds).average().orElse(0));
        }

        return result;
    }

    /**
     * Get best performing months
     */
    public List<MonthlyResult> getBestMonths(BacktestResult result, int limit) {
        return result.getMonthlyResults().stream()
            .sorted((a, b) -> Double.compare(b.getProfit(), a.getProfit()))
            .limit(limit)
            .toList();
    }

    /**
     * Get worst performing months
     */
    public List<MonthlyResult> getWorstMonths(BacktestResult result, int limit) {
        return result.getMonthlyResults().stream()
            .sorted(Comparator.comparingDouble(MonthlyResult::getProfit))
            .limit(limit)
            .toList();
    }

    /**
     * Get biggest winning bets
     */
    public List<BacktestBet> getBiggestWins(BacktestResult result, int limit) {
        return result.getBets().stream()
            .filter(BacktestBet::getWon)
            .sorted((a, b) -> Double.compare(b.getProfit(), a.getProfit()))
            .limit(limit)
            .toList();
    }

    /**
     * Get biggest losing bets
     */
    public List<BacktestBet> getBiggestLosses(BacktestResult result, int limit) {
        return result.getBets().stream()
            .filter(b -> !b.getWon())
            .sorted(Comparator.comparingDouble(BacktestBet::getProfit))
            .limit(limit)
            .toList();
    }

    /**
     * Analyze performance by odds range
     */
    public Map<String, Map<String, Object>> analyzeByOddsRange(BacktestResult result) {
        Map<String, Map<String, Object>> analysis = new LinkedHashMap<>();

        // Define odds ranges
        double[][] ranges = {
            {1.5, 2.0},
            {2.0, 2.5},
            {2.5, 3.0},
            {3.0, 4.0},
            {4.0, 10.0}
        };

        for (double[] range : ranges) {
            String key = String.format("%.1f-%.1f", range[0], range[1]);

            List<BacktestBet> betsInRange = result.getBets().stream()
                .filter(b -> b.getOdds() >= range[0] && b.getOdds() < range[1])
                .toList();

            if (!betsInRange.isEmpty()) {
                int wins = (int) betsInRange.stream().filter(BacktestBet::getWon).count();
                double profit = betsInRange.stream().mapToDouble(BacktestBet::getProfit).sum();
                double staked = betsInRange.stream().mapToDouble(BacktestBet::getStake).sum();

                Map<String, Object> stats = new LinkedHashMap<>();
                stats.put("bets", betsInRange.size());
                stats.put("wins", wins);
                stats.put("winRate", (double) wins / betsInRange.size() * 100);
                stats.put("profit", profit);
                stats.put("roi", staked > 0 ? profit / staked * 100 : 0);

                analysis.put(key, stats);
            }
        }

        return analysis;
    }

    private String getStrategyName(double fraction) {
        if (fraction >= 1.0) return "Full Kelly";
        if (fraction >= 0.5) return "Half Kelly";
        if (fraction >= 0.25) return "Quarter Kelly";
        return String.format("%.0f%% Kelly", fraction * 100);
    }

    private String formatDateRange(List<HistoricalMatch> matches) {
        if (matches.isEmpty()) return "N/A";
        LocalDate first = matches.get(0).getMatchDate();
        LocalDate last = matches.get(matches.size() - 1).getMatchDate();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
        return first.format(fmt) + " - " + last.format(fmt);
    }

    /**
     * Inner class for value bet identification
     */
    private static class ValueBet {
        String betType;
        double odds;
        double probability;
        double edge;

        ValueBet(String betType, double odds, double probability, double edge) {
            this.betType = betType;
            this.odds = odds;
            this.probability = probability;
            this.edge = edge;
        }
    }
}
