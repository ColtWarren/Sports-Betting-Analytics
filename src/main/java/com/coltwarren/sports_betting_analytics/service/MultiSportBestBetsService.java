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

    /**
     * Returns best bets filtered by sport (or all if sport is null/empty/"ALL"),
     * along with sport counts for the full unfiltered set.
     */
    public Map<String, Object> getBestBetsFiltered(String sport) {
        List<Map<String, Object>> allBets = getBestBetsAcrossAllSports();

        List<Map<String, Object>> filteredBets = allBets;
        if (sport != null && !sport.isEmpty() && !sport.equalsIgnoreCase("ALL")) {
            filteredBets = allBets.stream()
                .filter(bet -> sport.equalsIgnoreCase((String) bet.get("sport")))
                .collect(Collectors.toList());
        }

        Map<String, Long> sportCounts = allBets.stream()
            .collect(Collectors.groupingBy(
                bet -> (String) bet.get("sport"),
                Collectors.counting()
            ));

        Map<String, Object> result = new HashMap<>();
        result.put("totalBets", allBets.size());
        result.put("filteredBets", filteredBets.size());
        result.put("bets", filteredBets);
        result.put("sportCounts", sportCounts);
        result.put("appliedFilter", sport != null ? sport : "ALL");
        return result;
    }
}
