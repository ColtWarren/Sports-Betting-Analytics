package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.service.analyzer.SoccerBetAnalyzer;
import com.coltwarren.sports_betting_analytics.service.analyzer.StandardSportBetAnalyzer;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Orchestrator that coordinates bet analysis across all sports.
 * Delegates to sport-specific analyzers and aggregates results.
 */
@Service
@Slf4j
public class MultiSportBestBetsService {

    private static final String[] STANDARD_SPORTS = {"NFL", "CFB", "NBA", "WNBA", "CBB", "WCBB", "MLB", "NHL"};
    private static final int MAX_RESULTS = 20;

    private final ExecutorService executor = Executors.newFixedThreadPool(6);
    private final StandardSportBetAnalyzer standardAnalyzer;
    private final SoccerBetAnalyzer soccerAnalyzer;

    public MultiSportBestBetsService(StandardSportBetAnalyzer standardAnalyzer,
                                      SoccerBetAnalyzer soccerAnalyzer) {
        this.standardAnalyzer = standardAnalyzer;
        this.soccerAnalyzer = soccerAnalyzer;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public List<Map<String, Object>> getBestBetsAcrossAllSports() {
        try {
            log.info("Starting multi-sport best bets analysis...");

            List<Future<List<Map<String, Object>>>> futures = new ArrayList<>();

            // Submit standard sports
            for (String sport : STANDARD_SPORTS) {
                futures.add(executor.submit(() -> standardAnalyzer.analyze(sport)));
            }

            // Submit soccer (3-way betting, separate analyzer)
            futures.add(executor.submit(soccerAnalyzer::analyze));

            // Collect results (gracefully skip failures)
            List<Map<String, Object>> allBets = new ArrayList<>();
            String[] allSports = new String[STANDARD_SPORTS.length + 1];
            System.arraycopy(STANDARD_SPORTS, 0, allSports, 0, STANDARD_SPORTS.length);
            allSports[STANDARD_SPORTS.length] = "SOCCER";

            for (int i = 0; i < futures.size(); i++) {
                String sport = allSports[i];
                try {
                    List<Map<String, Object>> sportBets = futures.get(i).get(30, TimeUnit.SECONDS);
                    if (sportBets != null) {
                        allBets.addAll(sportBets);
                    }
                } catch (TimeoutException e) {
                    log.warn("Timeout fetching {} bets - skipping", sport);
                } catch (Exception e) {
                    log.warn("Error fetching {} bets (skipping): {}", sport, e.getMessage());
                }
            }

            log.info("Found {} total bets across all sports", allBets.size());

            // Sort by confidence score, return top results
            allBets.sort((a, b) -> {
                Double scoreA = (Double) a.getOrDefault("confidence", 0.0);
                Double scoreB = (Double) b.getOrDefault("confidence", 0.0);
                return scoreB.compareTo(scoreA);
            });

            return allBets.stream().limit(MAX_RESULTS).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting best bets: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
