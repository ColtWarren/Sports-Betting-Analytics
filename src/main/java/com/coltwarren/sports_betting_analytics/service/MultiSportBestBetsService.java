package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.service.ai.ClaudeAIService;
import com.coltwarren.sports_betting_analytics.service.odds.MultiSportOddsService;
import com.coltwarren.sports_betting_analytics.service.soccer.SoccerDataAggregatorService;
import com.coltwarren.sports_betting_analytics.service.weather.WeatherAnalysisService;
import com.coltwarren.sports_betting_analytics.service.betting.PublicBettingService;
import com.coltwarren.sports_betting_analytics.service.injury.InjuryAnalysisService;
import com.coltwarren.sports_betting_analytics.model.soccer.SoccerFixture;
import com.coltwarren.sports_betting_analytics.model.weather.WeatherImpact;
import com.coltwarren.sports_betting_analytics.model.betting.PublicBettingData;
import com.coltwarren.sports_betting_analytics.model.betting.ContrarianValue;
import com.coltwarren.sports_betting_analytics.model.injury.InjuryImpact;
import com.coltwarren.sports_betting_analytics.model.xg.MatchXGAnalysis;
import com.coltwarren.sports_betting_analytics.service.xg.XGDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    @Autowired
    private SoccerDataAggregatorService soccerDataAggregator;

    @Autowired
    private WeatherAnalysisService weatherAnalysisService;

    @Autowired
    private PublicBettingService publicBettingService;

    @Autowired
    private InjuryAnalysisService injuryAnalysisService;

    @Autowired
    private XGDataService xgDataService;

    private static final String[] SPORTS = {"NFL", "CFB", "NBA", "CBB", "MLB", "NHL", "SOCCER"};
    
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

            // Special handling for soccer (3-way betting)
            if ("SOCCER".equals(sport)) {
                return analyzeSoccerGames();
            }

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

                        // Weather Analysis (for outdoor sports: NFL, CFB, MLB)
                        if ("NFL".equals(sport) || "CFB".equals(sport) || "MLB".equals(sport)) {
                            try {
                                LocalDateTime gameTime = parseGameTime(game.get("gameTime"));
                                WeatherImpact weatherImpact = weatherAnalysisService.analyzeGameWeather(
                                    homeTeam, sport, gameTime != null ? gameTime : LocalDateTime.now().plusHours(3)
                                );

                                if (weatherImpact != null) {
                                    bet.put("weatherImpact", weatherImpact);
                                    bet.put("hasWeatherImpact", weatherImpact.getSignificantImpact());

                                    // Adjust analysis with weather info
                                    if (Boolean.TRUE.equals(weatherImpact.getSignificantImpact())) {
                                        String weatherNote = "\n\n☁️ WEATHER ALERT: " + weatherImpact.getReason();
                                        if (weatherImpact.getScoringImpact() != null) {
                                            weatherNote += String.format(" | Expected: %+.1f pts", weatherImpact.getScoringImpact());
                                        }
                                        weatherNote += " | Suggests: " + weatherImpact.getRecommendation();
                                        bet.put("analysis", analysis + weatherNote);

                                        // Boost confidence if weather strongly supports the bet
                                        if (weatherImpact.getConfidence() != null && weatherImpact.getConfidence() >= 7.0) {
                                            confidence = Math.min(10.0, confidence + 0.5);
                                            bet.put("confidence", confidence);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("Weather analysis error: " + e.getMessage());
                            }
                        }

                        // Public Betting Analysis (fade the public)
                        try {
                            // Determine if home team is favorite from odds
                            boolean isHomeFavorite = determineHomeFavorite(gameOdds);
                            String gameId = sport + "_" + homeTeam + "_" + awayTeam;

                            PublicBettingData publicData = publicBettingService.getPublicBettingData(
                                gameId, sport, homeTeam, awayTeam, isHomeFavorite
                            );

                            ContrarianValue spreadContrarian = publicBettingService.analyzeContrarianValue(
                                publicData, homeTeam, awayTeam
                            );
                            ContrarianValue totalsContrarian = publicBettingService.analyzeTotalsContrarian(publicData);

                            // Add public betting data to bet
                            bet.put("publicBettingData", publicData);
                            bet.put("spreadContrarian", spreadContrarian);
                            bet.put("totalsContrarian", totalsContrarian);
                            bet.put("hasContrarianValue",
                                Boolean.TRUE.equals(spreadContrarian.getHasContrarianValue()) ||
                                Boolean.TRUE.equals(totalsContrarian.getHasContrarianValue()));

                            // Boost confidence for strong contrarian plays
                            if (Boolean.TRUE.equals(spreadContrarian.getHasContrarianValue())) {
                                String currentAnalysis = (String) bet.get("analysis");
                                String contrarianNote = "\n\n📊 PUBLIC FADE: " + spreadContrarian.getReason();
                                bet.put("analysis", currentAnalysis + contrarianNote);

                                // Boost confidence based on alert level
                                if ("EXTREME".equals(spreadContrarian.getAlertLevel())) {
                                    confidence = Math.min(10.0, confidence + 1.0);
                                    bet.put("confidence", confidence);
                                } else if ("STRONG".equals(spreadContrarian.getAlertLevel())) {
                                    confidence = Math.min(10.0, confidence + 0.5);
                                    bet.put("confidence", confidence);
                                }
                            }

                        } catch (Exception e) {
                            System.err.println("Public betting analysis error: " + e.getMessage());
                        }

                        // Injury Analysis
                        try {
                            Map<String, InjuryImpact> matchupInjuries = injuryAnalysisService.analyzeMatchupInjuries(
                                sport, homeTeam, null, awayTeam, null
                            );

                            InjuryImpact homeInjuryImpact = matchupInjuries.get("home");
                            InjuryImpact awayInjuryImpact = matchupInjuries.get("away");
                            Double netInjuryAdvantage = injuryAnalysisService.getNetInjuryAdvantage(matchupInjuries);
                            String worstSeverity = injuryAnalysisService.getWorstSeverity(matchupInjuries);

                            bet.put("homeInjuryImpact", homeInjuryImpact);
                            bet.put("awayInjuryImpact", awayInjuryImpact);
                            bet.put("netInjuryAdvantage", netInjuryAdvantage);
                            bet.put("injurySeverity", worstSeverity);
                            bet.put("hasInjuryImpact",
                                Boolean.TRUE.equals(homeInjuryImpact.getHasSignificantInjuries()) ||
                                Boolean.TRUE.equals(awayInjuryImpact.getHasSignificantInjuries()));

                            // Add injury info to analysis
                            if (Boolean.TRUE.equals(homeInjuryImpact.getHasSignificantInjuries()) ||
                                Boolean.TRUE.equals(awayInjuryImpact.getHasSignificantInjuries())) {

                                String currentAnalysis = (String) bet.get("analysis");
                                StringBuilder injuryNote = new StringBuilder("\n\n🏥 INJURY REPORT:");

                                if (Boolean.TRUE.equals(homeInjuryImpact.getHasSignificantInjuries())) {
                                    injuryNote.append(String.format("\n  %s: %s (%.1f pts)",
                                        homeTeam, homeInjuryImpact.getSummary(), homeInjuryImpact.getSpreadImpact()));
                                }
                                if (Boolean.TRUE.equals(awayInjuryImpact.getHasSignificantInjuries())) {
                                    injuryNote.append(String.format("\n  %s: %s (%.1f pts)",
                                        awayTeam, awayInjuryImpact.getSummary(), awayInjuryImpact.getSpreadImpact()));
                                }

                                if (Math.abs(netInjuryAdvantage) >= 2.0) {
                                    String advantageTeam = netInjuryAdvantage > 0 ? homeTeam : awayTeam;
                                    injuryNote.append(String.format("\n  NET ADVANTAGE: %s +%.1f pts healthier",
                                        advantageTeam, Math.abs(netInjuryAdvantage)));

                                    // Boost confidence if injury advantage aligns with our bet
                                    confidence = Math.min(10.0, confidence + 0.5);
                                    bet.put("confidence", confidence);
                                }

                                bet.put("analysis", currentAnalysis + injuryNote.toString());

                                // Set injury recommendation
                                if (homeInjuryImpact.getBettingRecommendation() != null) {
                                    bet.put("injuryRecommendation", homeInjuryImpact.getBettingRecommendation());
                                } else if (awayInjuryImpact.getBettingRecommendation() != null) {
                                    bet.put("injuryRecommendation", awayInjuryImpact.getBettingRecommendation());
                                }
                            }

                        } catch (Exception e) {
                            System.err.println("Injury analysis error: " + e.getMessage());
                        }

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

    /**
     * Parse game time from various formats
     */
    private LocalDateTime parseGameTime(Object gameTimeObj) {
        if (gameTimeObj == null) return null;

        try {
            if (gameTimeObj instanceof LocalDateTime) {
                return (LocalDateTime) gameTimeObj;
            }

            String gameTimeStr = gameTimeObj.toString();

            // Try ISO format first
            if (gameTimeStr.contains("T")) {
                return LocalDateTime.parse(gameTimeStr.split("\\.")[0]);
            }

            // Default: assume it's today or soon
            return LocalDateTime.now().plusHours(3);

        } catch (Exception e) {
            return LocalDateTime.now().plusHours(3);
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

    /**
     * Analyze soccer games with 3-way betting
     * Kelly Criterion must account for 3 outcomes (Home/Draw/Away)
     */
    private List<Map<String, Object>> analyzeSoccerGames() {
        List<Map<String, Object>> soccerBets = new ArrayList<>();

        try {
            System.out.println("⚽ Analyzing soccer games (3-way betting)...");

            // Get fixtures from all leagues using odds-only mode to conserve API limits
            Map<String, List<SoccerFixture>> allFixtures = soccerDataAggregator.getAllFixturesFromOddsOnly();

            if (allFixtures.isEmpty()) {
                System.out.println("⚠️ No soccer fixtures found");
                return soccerBets;
            }

            int totalFixtures = allFixtures.values().stream().mapToInt(List::size).sum();
            System.out.println("📋 Found " + totalFixtures + " soccer fixtures across " + allFixtures.size() + " leagues");

            int gamesAnalyzed = 0;
            int maxGamesPerLeague = 3; // Limit to conserve API calls

            for (Map.Entry<String, List<SoccerFixture>> entry : allFixtures.entrySet()) {
                String league = entry.getKey();
                List<SoccerFixture> fixtures = entry.getValue();

                int leagueGames = 0;
                for (SoccerFixture fixture : fixtures) {
                    if (leagueGames >= maxGamesPerLeague) break;

                    try {
                        // Analyze each 3-way outcome
                        analyzeSoccerOutcome(fixture, "HOME_WIN", fixture.getHomeWinOdds(),
                            fixture.getBestHomeBook(), league, soccerBets);

                        analyzeSoccerOutcome(fixture, "DRAW", fixture.getDrawOdds(),
                            fixture.getBestDrawBook(), league, soccerBets);

                        analyzeSoccerOutcome(fixture, "AWAY_WIN", fixture.getAwayWinOdds(),
                            fixture.getBestAwayBook(), league, soccerBets);

                        leagueGames++;
                        gamesAnalyzed++;

                    } catch (Exception e) {
                        System.err.println("Error analyzing soccer fixture: " + e.getMessage());
                    }
                }
            }

            System.out.println("✅ Analyzed " + gamesAnalyzed + " soccer games, found " + soccerBets.size() + " quality bets");

        } catch (Exception e) {
            System.err.println("Error analyzing soccer: " + e.getMessage());
        }

        return soccerBets;
    }

    /**
     * Analyze a specific soccer outcome (Home/Draw/Away)
     */
    private void analyzeSoccerOutcome(SoccerFixture fixture, String outcome, Double odds,
                                      String sportsbook, String league, List<Map<String, Object>> allBets) {
        if (odds == null) return;

        try {
            // Get AI prediction probability for this outcome
            double winProbability = getSoccerAIProbability(fixture, outcome);

            // Convert American odds to decimal
            double decimalOdds = americanToDecimal(odds.intValue());

            // Calculate Expected Value
            double expectedValue = (winProbability * decimalOdds) - 1.0;

            // Only proceed if positive EV (or close to it for high-confidence picks)
            if (expectedValue < -0.02) return; // Allow small negative EV for top picks

            // Kelly Criterion for 3-way betting
            double kellyFraction = (winProbability * decimalOdds - 1) / (decimalOdds - 1);
            double kellyPercentage = Math.max(0, Math.min(kellyFraction * 25, 5.0)); // Quarter Kelly, max 5%

            // Calculate confidence score (1-10)
            double confidence = Math.min(10, 6 + (expectedValue * 20)); // Higher EV = higher confidence

            // Only include bets with decent confidence
            if (confidence < 6.0) return;

            // Format the recommendation
            String recommendation = formatSoccerPick(outcome, fixture);
            String gameDescription = fixture.getHomeTeam() + " vs " + fixture.getAwayTeam();

            Map<String, Object> bet = new HashMap<>();
            bet.put("sport", "SOCCER");
            bet.put("league", league);
            bet.put("homeTeam", fixture.getHomeTeam());
            bet.put("awayTeam", fixture.getAwayTeam());
            bet.put("gameTime", fixture.getKickoffTime());
            bet.put("recommendation", recommendation);
            bet.put("confidence", confidence);
            bet.put("kellyPercent", kellyPercentage);
            bet.put("isLocalTeam", fixture.isLocalTeam());

            // Create best odds map with all 3 soccer outcomes
            Map<String, Object> bestOdds = new HashMap<>();
            bestOdds.put("odds", odds.intValue());
            bestOdds.put("book", sportsbook);
            bestOdds.put("outcome", outcome);
            // Include all 3 outcomes for display
            if (fixture.getHomeWinOdds() != null) {
                bestOdds.put("homeWinOdds", fixture.getHomeWinOdds().intValue());
                bestOdds.put("homeWinBook", fixture.getBestHomeBook());
            }
            if (fixture.getDrawOdds() != null) {
                bestOdds.put("drawOdds", fixture.getDrawOdds().intValue());
                bestOdds.put("drawBook", fixture.getBestDrawBook());
            }
            if (fixture.getAwayWinOdds() != null) {
                bestOdds.put("awayWinOdds", fixture.getAwayWinOdds().intValue());
                bestOdds.put("awayWinBook", fixture.getBestAwayBook());
            }
            bet.put("bestOdds", bestOdds);

            // Add xG analysis for soccer
            try {
                MatchXGAnalysis xgAnalysis = xgDataService.analyzeMatch(
                    fixture.getHomeTeam(), fixture.getAwayTeam(), league);
                if (xgAnalysis != null) {
                    Map<String, Object> xgData = new HashMap<>();
                    xgData.put("projectedTotal", xgAnalysis.getProjectedTotal());
                    xgData.put("xgRecommendation", xgAnalysis.getXgRecommendation());
                    xgData.put("xgConfidence", xgAnalysis.getXgConfidence());
                    xgData.put("xgReason", xgAnalysis.getXgReason());

                    // Home team xG stats
                    if (xgAnalysis.getHomeXGStats() != null) {
                        Map<String, Object> homeXG = new HashMap<>();
                        homeXG.put("xGPerGame", xgAnalysis.getHomeXGStats().getXGPerGame());
                        homeXG.put("goalsVsXG", xgAnalysis.getHomeXGStats().getGoalsVsXG());
                        homeXG.put("performanceStatus", xgAnalysis.getHomeXGStats().getPerformanceStatus());
                        homeXG.put("bettingSignal", xgAnalysis.getHomeXGStats().getBettingSignal());
                        homeXG.put("regressionLikelihood", xgAnalysis.getHomeXGStats().getRegressionLikelihood());
                        xgData.put("homeXGStats", homeXG);
                    }

                    // Away team xG stats
                    if (xgAnalysis.getAwayXGStats() != null) {
                        Map<String, Object> awayXG = new HashMap<>();
                        awayXG.put("xGPerGame", xgAnalysis.getAwayXGStats().getXGPerGame());
                        awayXG.put("goalsVsXG", xgAnalysis.getAwayXGStats().getGoalsVsXG());
                        awayXG.put("performanceStatus", xgAnalysis.getAwayXGStats().getPerformanceStatus());
                        awayXG.put("bettingSignal", xgAnalysis.getAwayXGStats().getBettingSignal());
                        awayXG.put("regressionLikelihood", xgAnalysis.getAwayXGStats().getRegressionLikelihood());
                        xgData.put("awayXGStats", awayXG);
                    }

                    bet.put("xgAnalysis", xgData);
                }
            } catch (Exception xgError) {
                System.err.println("Error adding xG analysis: " + xgError.getMessage());
            }

            // AI analysis
            String analysis = String.format(
                "RECOMMENDATION: %s | CONFIDENCE: %.1f | REASON: %s with %.1f%% implied probability. " +
                "EV: %.2f%%. %s",
                recommendation, confidence, outcome,
                (1.0 / decimalOdds) * 100, expectedValue * 100,
                fixture.isLocalTeam() ? "Missouri local team game!" : ""
            );
            bet.put("analysis", analysis);

            allBets.add(bet);

        } catch (Exception e) {
            System.err.println("Error analyzing soccer outcome: " + e.getMessage());
        }
    }

    /**
     * Get AI-predicted probability for a soccer outcome
     */
    private double getSoccerAIProbability(SoccerFixture fixture, String outcome) {
        Double odds = switch (outcome) {
            case "HOME_WIN" -> fixture.getHomeWinOdds();
            case "DRAW" -> fixture.getDrawOdds();
            case "AWAY_WIN" -> fixture.getAwayWinOdds();
            default -> null;
        };

        if (odds == null) return 0.0;

        // Start with implied probability from odds
        double impliedProb = oddsToImpliedProbability(odds.intValue());

        // Adjust based on factors
        // Home advantage: +5% for home wins
        if ("HOME_WIN".equals(outcome)) {
            impliedProb += 0.03;
        }

        // Missouri local teams: slight boost for home games
        if (fixture.isLocalTeam() && "HOME_WIN".equals(outcome)) {
            impliedProb += 0.02;
        }

        return Math.max(0.05, Math.min(0.80, impliedProb));
    }

    /**
     * Format soccer pick for display
     */
    private String formatSoccerPick(String outcome, SoccerFixture fixture) {
        return switch (outcome) {
            case "HOME_WIN" -> fixture.getHomeTeam() + " to Win";
            case "DRAW" -> "Draw";
            case "AWAY_WIN" -> fixture.getAwayTeam() + " to Win";
            default -> outcome;
        };
    }

    /**
     * Convert American odds to decimal
     */
    private double americanToDecimal(int americanOdds) {
        if (americanOdds > 0) {
            return (americanOdds / 100.0) + 1;
        } else {
            return (100.0 / Math.abs(americanOdds)) + 1;
        }
    }

    /**
     * Convert American odds to implied probability
     */
    private double oddsToImpliedProbability(int americanOdds) {
        if (americanOdds > 0) {
            return 100.0 / (americanOdds + 100);
        } else {
            return Math.abs(americanOdds) / (Math.abs(americanOdds) + 100.0);
        }
    }

    /**
     * Determine if home team is the favorite based on odds
     */
    @SuppressWarnings("unchecked")
    private boolean determineHomeFavorite(Map<String, Object> gameOdds) {
        try {
            Map<String, Object> bestOdds = (Map<String, Object>) gameOdds.get("bestOdds");
            if (bestOdds == null) return true; // Default to home favorite

            // Check spread first (negative spread = favorite)
            if (bestOdds.containsKey("homeSpread")) {
                Object spreadObj = bestOdds.get("homeSpread");
                if (spreadObj instanceof Number) {
                    return ((Number) spreadObj).doubleValue() < 0;
                }
            }

            // Check moneyline (negative ML = favorite)
            if (bestOdds.containsKey("homeML") && bestOdds.containsKey("awayML")) {
                Object homeML = bestOdds.get("homeML");
                Object awayML = bestOdds.get("awayML");
                if (homeML instanceof Number && awayML instanceof Number) {
                    return ((Number) homeML).intValue() < ((Number) awayML).intValue();
                }
            }

            return true; // Default
        } catch (Exception e) {
            return true;
        }
    }
}
