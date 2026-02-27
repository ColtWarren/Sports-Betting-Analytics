package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.model.historical.*;
import com.coltwarren.sports_betting_analytics.service.historical.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Backtest Controller
 *
 * REST endpoints for historical data import and backtesting.
 * Allows users to import soccer data and test betting strategies.
 */
@RestController
@RequestMapping("/api/backtest")
@Slf4j
public class BacktestController {

    private final HistoricalDataImportService importService;
    private final BacktestingService backtestingService;

    public BacktestController(HistoricalDataImportService importService,
                              BacktestingService backtestingService) {
        this.importService = importService;
        this.backtestingService = backtestingService;
    }

    // ==================== DATA IMPORT ENDPOINTS ====================

    /**
     * Import data for a specific league and season
     * Example: POST /api/backtest/import/EPL/2324
     */
    @PostMapping("/import/{league}/{season}")
    public ResponseEntity<Map<String, Object>> importLeagueSeason(
            @PathVariable String league,
            @PathVariable String season) {

        log.info("Import request for {} season {}", league, season);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("league", league);
        response.put("season", season);

        int count = importService.importLeagueSeason(league, season);

        if (count > 0) {
            response.put("status", "success");
            response.put("matchesImported", count);
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "no_data");
            response.put("message", "No new data imported (may already exist or not available)");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Import all seasons for a league
     * Example: POST /api/backtest/import/EPL
     */
    @PostMapping("/import/{league}")
    public ResponseEntity<Map<String, Object>> importAllSeasons(@PathVariable String league) {
        log.info("Import all seasons for {}", league);

        Map<String, Integer> results = importService.importAllSeasonsForLeague(league);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("league", league);
        response.put("seasonResults", results);
        response.put("totalMatches", results.values().stream().mapToInt(Integer::intValue).sum());
        response.put("seasonsImported", results.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Import all leagues, all seasons
     * Example: POST /api/backtest/import/all
     */
    @PostMapping("/import/all")
    public ResponseEntity<Map<String, Object>> importAll() {
        log.info("Import ALL historical data");

        Map<String, Map<String, Integer>> results = importService.importAllData();

        int totalMatches = results.values().stream()
            .flatMap(m -> m.values().stream())
            .mapToInt(Integer::intValue)
            .sum();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", results);
        response.put("totalMatches", totalMatches);
        response.put("leaguesImported", results.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get import statistics
     * Example: GET /api/backtest/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getImportStats() {
        return ResponseEntity.ok(importService.getImportStats());
    }

    /**
     * Get available leagues
     * Example: GET /api/backtest/leagues
     */
    @GetMapping("/leagues")
    public ResponseEntity<Set<String>> getAvailableLeagues() {
        return ResponseEntity.ok(importService.getAvailableLeagues());
    }

    /**
     * Clear data for a league
     * Example: DELETE /api/backtest/data/{league}
     */
    @DeleteMapping("/data/{league}")
    public ResponseEntity<Map<String, Object>> clearLeagueData(@PathVariable String league) {
        int deleted = importService.clearLeagueData(league);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("league", league);
        response.put("matchesDeleted", deleted);

        return ResponseEntity.ok(response);
    }

    // ==================== BACKTESTING ENDPOINTS ====================

    /**
     * Run Kelly Criterion backtest
     * Example: GET /api/backtest/run/EPL?kellyFraction=0.25&bankroll=1000
     */
    @GetMapping("/run/{league}")
    public ResponseEntity<BacktestResult> runBacktest(
            @PathVariable String league,
            @RequestParam(required = false, defaultValue = "0.25") Double kellyFraction,
            @RequestParam(required = false, defaultValue = "1000") Double bankroll,
            @RequestParam(required = false, defaultValue = "0.02") Double minEdge) {

        log.info("Running backtest for {} with {}% Kelly, ${} bankroll",
            league, kellyFraction * 100, bankroll);

        BacktestResult result = backtestingService.runKellyBacktest(
            league, kellyFraction, bankroll, minEdge);

        return ResponseEntity.ok(result);
    }

    /**
     * Compare different Kelly strategies
     * Example: GET /api/backtest/compare/EPL
     */
    @GetMapping("/compare/{league}")
    public ResponseEntity<Map<String, Object>> compareStrategies(@PathVariable String league) {
        log.info("Comparing strategies for {}", league);

        Map<String, BacktestResult> results = backtestingService.compareStrategies(league);

        // Create summary comparison
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("league", league);

        List<Map<String, Object>> comparison = new ArrayList<>();
        for (Map.Entry<String, BacktestResult> entry : results.entrySet()) {
            BacktestResult r = entry.getValue();
            Map<String, Object> strategyStats = new LinkedHashMap<>();
            strategyStats.put("strategy", entry.getKey());
            strategyStats.put("totalBets", r.getTotalBets());
            strategyStats.put("winRate", r.getWinRate());
            strategyStats.put("roi", r.getRoi());
            strategyStats.put("finalBankroll", r.getFinalBankroll());
            strategyStats.put("maxDrawdown", r.getMaxDrawdown());
            strategyStats.put("sharpeRatio", r.getSharpeRatio());
            strategyStats.put("profitable", r.isProfitable());
            comparison.add(strategyStats);
        }

        response.put("comparison", comparison);
        response.put("fullResults", results);

        // Determine best strategy by ROI
        String bestStrategy = results.entrySet().stream()
            .max(Comparator.comparingDouble(e -> e.getValue().getRoi() != null ? e.getValue().getRoi() : 0))
            .map(Map.Entry::getKey)
            .orElse("N/A");
        response.put("bestByROI", bestStrategy);

        // Determine best by Sharpe (risk-adjusted)
        String bestBySharpe = results.entrySet().stream()
            .max(Comparator.comparingDouble(e -> e.getValue().getSharpeRatio() != null ? e.getValue().getSharpeRatio() : 0))
            .map(Map.Entry::getKey)
            .orElse("N/A");
        response.put("bestBySharpe", bestBySharpe);

        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed analysis for a backtest
     * Example: GET /api/backtest/analyze/EPL
     */
    @GetMapping("/analyze/{league}")
    public ResponseEntity<Map<String, Object>> analyzeBacktest(
            @PathVariable String league,
            @RequestParam(required = false, defaultValue = "0.25") Double kellyFraction) {

        BacktestResult result = backtestingService.runKellyBacktest(
            league, kellyFraction, 1000.0, 0.02);

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("summary", result.getSummary());
        analysis.put("league", league);
        analysis.put("strategy", result.getStrategyName());
        analysis.put("dateRange", result.getDateRange());

        // Core stats
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalBets", result.getTotalBets());
        stats.put("wins", result.getWins());
        stats.put("losses", result.getLosses());
        stats.put("winRate", String.format("%.1f%%", result.getWinRate()));
        stats.put("roi", String.format("%.1f%%", result.getRoi()));
        stats.put("profit", String.format("$%.2f", result.getTotalProfit()));
        stats.put("finalBankroll", String.format("$%.2f", result.getFinalBankroll()));
        stats.put("maxDrawdown", String.format("%.1f%%", result.getMaxDrawdown()));
        stats.put("sharpeRatio", String.format("%.2f", result.getSharpeRatio()));
        analysis.put("stats", stats);

        // Bet breakdown
        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("homeBets", result.getHomeBets());
        breakdown.put("drawBets", result.getDrawBets());
        breakdown.put("awayBets", result.getAwayBets());
        breakdown.put("avgOdds", String.format("%.2f", result.getAvgOdds()));
        breakdown.put("avgKellyPercent", String.format("%.2f%%", result.getAvgKellyPercent()));
        analysis.put("betBreakdown", breakdown);

        // Best/worst months
        analysis.put("bestMonths", backtestingService.getBestMonths(result, 3));
        analysis.put("worstMonths", backtestingService.getWorstMonths(result, 3));

        // Biggest wins/losses
        analysis.put("biggestWins", backtestingService.getBiggestWins(result, 5));
        analysis.put("biggestLosses", backtestingService.getBiggestLosses(result, 5));

        // By odds range
        analysis.put("byOddsRange", backtestingService.analyzeByOddsRange(result));

        // Monthly results
        analysis.put("monthlyResults", result.getMonthlyResults());

        return ResponseEntity.ok(analysis);
    }

    /**
     * Quick overview of all leagues
     * Example: GET /api/backtest/overview
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        Map<String, Object> stats = importService.getImportStats();
        overview.put("dataStats", stats);

        // Run quick backtests on available leagues
        @SuppressWarnings("unchecked")
        List<String> leagues = (List<String>) stats.get("leagues");

        if (leagues != null && !leagues.isEmpty()) {
            List<Map<String, Object>> leagueSummaries = new ArrayList<>();

            for (String league : leagues) {
                BacktestResult result = backtestingService.runKellyBacktest(
                    league, 0.25, 1000.0, 0.02);

                if (result.getTotalBets() > 0) {
                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("league", league);
                    summary.put("matches", result.getTotalMatches());
                    summary.put("bets", result.getTotalBets());
                    summary.put("winRate", result.getWinRate());
                    summary.put("roi", result.getRoi());
                    summary.put("profitable", result.isProfitable());
                    leagueSummaries.add(summary);
                }
            }

            overview.put("leagueResults", leagueSummaries);
        }

        return ResponseEntity.ok(overview);
    }

    /**
     * Get monthly performance chart data
     * Example: GET /api/backtest/chart/EPL
     */
    @GetMapping("/chart/{league}")
    public ResponseEntity<Map<String, Object>> getChartData(@PathVariable String league) {
        BacktestResult result = backtestingService.runKellyBacktest(
            league, 0.25, 1000.0, 0.02);

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("league", league);

        // Bankroll progression
        List<Map<String, Object>> bankrollProgression = new ArrayList<>();
        double runningBankroll = 1000.0;
        bankrollProgression.add(Map.of("date", "Start", "bankroll", runningBankroll));

        for (BacktestBet bet : result.getBets()) {
            runningBankroll = bet.getBankrollAfter();
            bankrollProgression.add(Map.of(
                "date", bet.getDate().toString(),
                "bankroll", runningBankroll
            ));
        }
        chartData.put("bankrollProgression", bankrollProgression);

        // Monthly ROI
        List<Map<String, Object>> monthlyRoi = new ArrayList<>();
        for (MonthlyResult mr : result.getMonthlyResults()) {
            monthlyRoi.add(Map.of(
                "month", mr.getMonth(),
                "roi", mr.getRoi(),
                "profit", mr.getProfit(),
                "bets", mr.getBets()
            ));
        }
        chartData.put("monthlyRoi", monthlyRoi);

        // Win rate by bet type
        Map<String, Object> winRateByType = new LinkedHashMap<>();
        long homeWins = result.getBets().stream()
            .filter(b -> "H".equals(b.getBetType()) && b.getWon()).count();
        long drawWins = result.getBets().stream()
            .filter(b -> "D".equals(b.getBetType()) && b.getWon()).count();
        long awayWins = result.getBets().stream()
            .filter(b -> "A".equals(b.getBetType()) && b.getWon()).count();

        winRateByType.put("home", Map.of(
            "bets", result.getHomeBets(),
            "wins", homeWins,
            "winRate", result.getHomeBets() > 0 ? (double) homeWins / result.getHomeBets() * 100 : 0
        ));
        winRateByType.put("draw", Map.of(
            "bets", result.getDrawBets(),
            "wins", drawWins,
            "winRate", result.getDrawBets() > 0 ? (double) drawWins / result.getDrawBets() * 100 : 0
        ));
        winRateByType.put("away", Map.of(
            "bets", result.getAwayBets(),
            "wins", awayWins,
            "winRate", result.getAwayBets() > 0 ? (double) awayWins / result.getAwayBets() * 100 : 0
        ));
        chartData.put("winRateByType", winRateByType);

        return ResponseEntity.ok(chartData);
    }
}
