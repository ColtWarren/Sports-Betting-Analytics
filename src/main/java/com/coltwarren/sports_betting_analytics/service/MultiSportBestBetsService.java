package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.service.ai.ClaudeAIService;
import com.coltwarren.sports_betting_analytics.service.odds.MultiSportOddsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class MultiSportBestBetsService {
    
    @Autowired
    private MultiSportOddsService multiSportOddsService;

    @Autowired
    private ClaudeAIService claudeAIService;

    @Autowired
    private LiveGameService liveGameService;

    @Autowired
    private KellyCriterionService kellyCriterionService;

    private static final String[] SPORTS = {"NFL", "CFB", "NBA", "CBB", "MLB", "NHL"};
    
    public List<Map<String, Object>> getBestBetsAcrossAllSports() {
        try {
            System.out.println("🔥 Starting multi-sport best bets analysis...");
            
            // Use parallel processing to analyze all sports simultaneously
            ExecutorService executor = Executors.newFixedThreadPool(6);
            List<Future<List<Map<String, Object>>>> futures = new ArrayList<>();
            
            for (String sport : SPORTS) {
                Future<List<Map<String, Object>>> future = executor.submit(() -> analyzeSport(sport));
                futures.add(future);
            }
            
            // Collect all bets from all sports
            List<Map<String, Object>> allBets = new ArrayList<>();
            for (Future<List<Map<String, Object>>> future : futures) {
                try {
                    List<Map<String, Object>> sportBets = future.get(30, TimeUnit.SECONDS);
                    allBets.addAll(sportBets);
                } catch (Exception e) {
                    System.err.println("Error getting sport bets: " + e.getMessage());
                }
            }
            
            executor.shutdown();
            
            System.out.println("📊 Found " + allBets.size() + " total bets across all sports");
            
            // Sort by confidence score
            allBets.sort((a, b) -> {
                Double scoreA = (Double) a.getOrDefault("confidence", 0.0);
                Double scoreB = (Double) b.getOrDefault("confidence", 0.0);
                return scoreB.compareTo(scoreA);
            });
            
            // Return top 20 bets
            return allBets.stream().limit(20).collect(Collectors.toList());
            
        } catch (Exception e) {
            System.err.println("Error getting best bets: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    
    private List<Map<String, Object>> analyzeSport(String sport) {
        try {
            System.out.println("🏈 Analyzing " + sport + "...");
            
            // Get live and upcoming games
            List<Map<String, Object>> games = liveGameService.getLiveGames(sport);
            
            if (games.isEmpty()) {
                System.out.println("⚠️ No games found for " + sport);
                return Collections.emptyList();
            }
            
            System.out.println("📋 Found " + games.size() + " games for " + sport);
            
            // Get odds for this sport
            List<Map<String, Object>> oddsData = multiSportOddsService.getOddsForSport(sport);
            
            if (oddsData.isEmpty()) {
                System.out.println("⚠️ No odds found for " + sport);
                return Collections.emptyList();
            }
            
            System.out.println("💰 Found odds for " + oddsData.size() + " games in " + sport);
            
            List<Map<String, Object>> sportBets = new ArrayList<>();
            
            // Analyze each game (limit to first 5 games per sport to avoid API overuse)
            int gamesAnalyzed = 0;
            for (Map<String, Object> game : games) {
                if (gamesAnalyzed >= 5) break;
                
                try {
                    String homeTeam = (String) game.get("homeTeam");
                    String awayTeam = (String) game.get("awayTeam");
                    
                    if (homeTeam == null || awayTeam == null) continue;
                    
                    // Find odds for this game
                    Map<String, Object> gameOdds = findOddsForGame(oddsData, homeTeam, awayTeam);
                    
                    if (gameOdds == null || !gameOdds.containsKey("bestOdds")) {
                        continue;
                    }
                    
                    // Quick AI analysis
                    String analysis = getQuickAnalysis(sport, homeTeam, awayTeam);
                    double confidence = extractConfidence(analysis);
                    
                    if (confidence >= 7.0) {
                        Map<String, Object> bet = new HashMap<>();
                        bet.put("sport", sport);
                        bet.put("homeTeam", homeTeam);
                        bet.put("awayTeam", awayTeam);
                        bet.put("gameTime", game.get("gameTime"));
                        bet.put("bestOdds", gameOdds.get("bestOdds"));
                        bet.put("analysis", analysis);
                        bet.put("confidence", confidence);
                        bet.put("recommendation", extractRecommendation(analysis));

                        // Calculate Kelly Criterion percentage
                        double kellyPercent = calculateKellyForBet(gameOdds, confidence);
                        bet.put("kellyPercent", kellyPercent);

                        sportBets.add(bet);
                    }
                    
                    gamesAnalyzed++;
                    
                } catch (Exception e) {
                    System.err.println("Error analyzing game: " + e.getMessage());
                }
            }
            
            System.out.println("✅ Found " + sportBets.size() + " quality bets for " + sport);
            return sportBets;
            
        } catch (Exception e) {
            System.err.println("Error analyzing sport " + sport + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private Map<String, Object> findOddsForGame(List<Map<String, Object>> oddsData, String homeTeam, String awayTeam) {
        for (Map<String, Object> odds : oddsData) {
            String oddsHome = (String) odds.get("homeTeam");
            String oddsAway = (String) odds.get("awayTeam");
            
            if (teamsMatch(oddsHome, homeTeam) && teamsMatch(oddsAway, awayTeam)) {
                return odds;
            }
        }
        return null;
    }
    
    private boolean teamsMatch(String team1, String team2) {
        if (team1 == null || team2 == null) return false;
        
        String t1 = team1.toLowerCase().replaceAll("[^a-z]", "");
        String t2 = team2.toLowerCase().replaceAll("[^a-z]", "");
        
        return t1.contains(t2) || t2.contains(t1);
    }
    
    private String getQuickAnalysis(String sport, String homeTeam, String awayTeam) {
        try {
            String prompt = String.format(
                "Quick betting analysis for %s game: %s vs %s.\n\n" +
                "Provide:\n" +
                "1. Recommended bet (spread/total/ML)\n" +
                "2. Confidence score (1-10)\n" +
                "3. Brief 2-sentence reason\n\n" +
                "Format: RECOMMENDATION: [bet] | CONFIDENCE: [score] | REASON: [reason]",
                sport, awayTeam, homeTeam
            );
            
            String analysis = claudeAIService.callClaudeAPI(prompt);
            return analysis != null ? analysis : "No analysis available";
            
        } catch (Exception e) {
            System.err.println("Error getting AI analysis: " + e.getMessage());
            return "Analysis unavailable";
        }
    }
    
    private double extractConfidence(String analysis) {
        try {
            // Look for "CONFIDENCE: X" or "Confidence: X/10"
            String lower = analysis.toLowerCase();
            
            if (lower.contains("confidence:")) {
                int start = lower.indexOf("confidence:") + 11;
                String sub = analysis.substring(start).trim();
                
                // Extract first number
                String[] parts = sub.split("[^0-9.]");
                if (parts.length > 0 && !parts[0].isEmpty()) {
                    return Double.parseDouble(parts[0]);
                }
            }
            
            return 7.0; // Default
            
        } catch (Exception e) {
            return 7.0;
        }
    }
    
    private String extractRecommendation(String analysis) {
        try {
            if (analysis.toLowerCase().contains("recommendation:")) {
                int start = analysis.toLowerCase().indexOf("recommendation:") + 15;
                String sub = analysis.substring(start);

                int end = sub.indexOf("|");
                if (end > 0) {
                    return sub.substring(0, end).trim();
                } else {
                    return sub.split("\n")[0].trim();
                }
            }

            return "See analysis";

        } catch (Exception e) {
            return "See analysis";
        }
    }

    private double calculateKellyForBet(Map<String, Object> gameOdds, double confidence) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> bestOdds = (Map<String, Object>) gameOdds.get("bestOdds");

            if (bestOdds == null) {
                return 0.0;
            }

            // Get the best odds available (try ML first, then spread)
            Integer odds = null;
            if (bestOdds.containsKey("homeML")) {
                odds = (Integer) bestOdds.get("homeML");
            } else if (bestOdds.containsKey("awayML")) {
                odds = (Integer) bestOdds.get("awayML");
            } else if (bestOdds.containsKey("homeSpreadOdds")) {
                odds = (Integer) bestOdds.get("homeSpreadOdds");
            } else if (bestOdds.containsKey("awaySpreadOdds")) {
                odds = (Integer) bestOdds.get("awaySpreadOdds");
            }

            if (odds == null) {
                return 0.0;
            }

            // Convert confidence (1-10) to win probability (50-70%)
            // Confidence 7 = 55%, Confidence 8 = 60%, Confidence 9 = 65%, Confidence 10 = 70%
            double winProbability = 0.50 + (confidence - 7.0) * 0.05;
            winProbability = Math.max(0.50, Math.min(0.70, winProbability));

            // Calculate Kelly (Quarter Kelly for safety)
            Map<String, Object> kellyResult = kellyCriterionService.calculateKelly(odds, winProbability, true);

            Double kellyPercentage = (Double) kellyResult.get("kellyPercentage");
            return kellyPercentage != null ? kellyPercentage : 0.0;

        } catch (Exception e) {
            System.err.println("Error calculating Kelly: " + e.getMessage());
            return 0.0;
        }
    }
}
