package com.coltwarren.sports_betting_analytics.service.analyzer;

import com.coltwarren.sports_betting_analytics.service.KellyCriterionService;
import com.coltwarren.sports_betting_analytics.service.LiveGameService;
import com.coltwarren.sports_betting_analytics.service.ai.ClaudeAIService;
import com.coltwarren.sports_betting_analytics.service.college.WNCAAWDataService;
import com.coltwarren.sports_betting_analytics.service.odds.MultiSportOddsService;
import com.coltwarren.sports_betting_analytics.service.wnba.WNBADataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Analyzer for standard American sports (NFL, NBA, MLB, NHL, CFB, CBB, WNBA, WCBB).
 * Handles 2-way betting with spread/ML/total markets.
 */
@Service
@Slf4j
public class StandardSportBetAnalyzer {

    private static final Set<String> OUTDOOR_SPORTS = Set.of("NFL", "CFB", "MLB");

    private final MultiSportOddsService multiSportOddsService;
    private final ClaudeAIService claudeAIService;
    private final LiveGameService liveGameService;
    private final KellyCriterionService kellyCriterionService;
    private final BetEnrichmentService enrichmentService;
    private final WNBADataService wnbaDataService;
    private final WNCAAWDataService wcbbDataService;

    public StandardSportBetAnalyzer(MultiSportOddsService multiSportOddsService,
                                     ClaudeAIService claudeAIService,
                                     LiveGameService liveGameService,
                                     KellyCriterionService kellyCriterionService,
                                     BetEnrichmentService enrichmentService,
                                     WNBADataService wnbaDataService,
                                     WNCAAWDataService wcbbDataService) {
        this.multiSportOddsService = multiSportOddsService;
        this.claudeAIService = claudeAIService;
        this.liveGameService = liveGameService;
        this.kellyCriterionService = kellyCriterionService;
        this.enrichmentService = enrichmentService;
        this.wnbaDataService = wnbaDataService;
        this.wcbbDataService = wcbbDataService;
    }

    /**
     * Analyze a standard sport and return quality bets.
     */
    public List<Map<String, Object>> analyze(String sport) {
        try {
            log.info("Analyzing {}...", sport);

            List<Map<String, Object>> games = liveGameService.getLiveGames(sport);
            if (games.isEmpty()) {
                log.info("No games found for {}", sport);
                return Collections.emptyList();
            }
            log.info("Found {} games for {}", games.size(), sport);

            List<Map<String, Object>> oddsData = multiSportOddsService.getOddsForSport(sport);
            if (oddsData.isEmpty()) {
                log.info("No odds found for {}", sport);
                return Collections.emptyList();
            }
            log.info("Found odds for {} games in {}", oddsData.size(), sport);

            List<Map<String, Object>> sportBets = new ArrayList<>();
            int gamesAnalyzed = 0;

            for (Map<String, Object> game : games) {
                if (gamesAnalyzed >= 5) break;

                try {
                    String homeTeam = (String) game.get("homeTeam");
                    String awayTeam = (String) game.get("awayTeam");
                    if (homeTeam == null || awayTeam == null) continue;

                    Map<String, Object> gameOdds = findOddsForGame(oddsData, homeTeam, awayTeam);
                    if (gameOdds == null || !gameOdds.containsKey("bestOdds")) continue;

                    // Gather injury data before AI analysis
                    BetEnrichmentService.InjuryContext injuryCtx =
                        enrichmentService.gatherInjuryContext(sport, homeTeam, awayTeam);

                    // AI analysis with injury context
                    String analysis = getQuickAnalysis(sport, homeTeam, awayTeam, injuryCtx.promptText());
                    double confidence = BetAnalysisUtils.extractConfidence(analysis);

                    if (confidence >= getMinConfidenceThreshold(sport)) {
                        Map<String, Object> bet = buildBet(sport, game, gameOdds, analysis, confidence);

                        if (OUTDOOR_SPORTS.contains(sport)) {
                            enrichmentService.enrichWithWeather(bet, game, homeTeam, sport);
                        }
                        enrichmentService.enrichWithPublicBetting(bet, gameOdds, sport, homeTeam, awayTeam);
                        enrichmentService.enrichWithInjuries(bet, injuryCtx.matchupInjuries());

                        sportBets.add(bet);
                    }

                    gamesAnalyzed++;

                } catch (Exception e) {
                    log.error("Error analyzing game: {}", e.getMessage());
                }
            }

            log.info("Found {} quality bets for {}", sportBets.size(), sport);
            return sportBets;

        } catch (Exception e) {
            log.error("Error analyzing sport {}: {}", sport, e.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, Object> buildBet(String sport, Map<String, Object> game,
                                          Map<String, Object> gameOdds, String analysis, double confidence) {
        Map<String, Object> bet = new HashMap<>();
        bet.put("sport", sport);
        bet.put("homeTeam", game.get("homeTeam"));
        bet.put("awayTeam", game.get("awayTeam"));
        bet.put("gameTime", game.get("gameTime"));
        bet.put("bestOdds", gameOdds.get("bestOdds"));
        bet.put("analysis", analysis);
        bet.put("confidence", confidence);
        bet.put("recommendation", BetAnalysisUtils.extractRecommendation(analysis));
        bet.put("kellyPercent", calculateKellyForBet(gameOdds, confidence, sport));
        return bet;
    }

    private Map<String, Object> findOddsForGame(List<Map<String, Object>> oddsData,
                                                 String homeTeam, String awayTeam) {
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

    private String getQuickAnalysis(String sport, String homeTeam, String awayTeam, String injuryContext) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append(String.format("Quick betting analysis for %s game: %s vs %s.\n\n",
                sport, awayTeam, homeTeam));

            if (injuryContext != null && !injuryContext.isEmpty()) {
                prompt.append("INJURY REPORT (factor this into your recommendation):\n");
                prompt.append(injuryContext).append("\n");
            }

            if ("WNBA".equals(sport)) {
                prompt.append(getWNBAContext(homeTeam, awayTeam));
            } else if ("WCBB".equals(sport)) {
                prompt.append(getWCBBContext(homeTeam, awayTeam));
            }

            prompt.append("Provide:\n");
            prompt.append("1. Recommended bet (spread/total/ML)\n");
            prompt.append("2. Confidence score (1-10)\n");
            prompt.append("3. Brief 2-sentence reason\n\n");
            prompt.append("Format: RECOMMENDATION: [bet] | CONFIDENCE: [score] | REASON: [reason]");

            String analysis = claudeAIService.callClaudeAPI(prompt.toString());
            return analysis != null ? analysis : "No analysis available";

        } catch (Exception e) {
            log.error("Error getting AI analysis: {}", e.getMessage());
            return "Analysis unavailable";
        }
    }

    private String getWNBAContext(String homeTeam, String awayTeam) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("WNBA CONTEXT: Markets are less efficient than NBA - edges are more frequent. ");
        ctx.append("Star availability (Wilson, Clark, Stewart) shifts lines 3-5 pts. ");
        ctx.append("Rest days critical in 40-game season.\n");

        try {
            appendTeamStats(ctx, homeTeam, wnbaDataService.getTeamStats(homeTeam));
            appendTeamStats(ctx, awayTeam, wnbaDataService.getTeamStats(awayTeam));
        } catch (Exception e) {
            // Stats unavailable - context still useful
        }

        ctx.append("\n");
        return ctx.toString();
    }

    private String getWCBBContext(String homeTeam, String awayTeam) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("WCBB CONTEXT: Women's college basketball markets are significantly less efficient - largest edges in basketball betting. ");
        ctx.append("Watch for inflated lines on dominant programs (South Carolina, UConn). ");
        ctx.append("Historic powerhouses attract disproportionate public money.\n");

        try {
            int season = wcbbDataService.getCurrentSeason();
            appendTeamStatsWithSRS(ctx, homeTeam, wcbbDataService.getTeamStats(homeTeam, season));
            appendTeamStatsWithSRS(ctx, awayTeam, wcbbDataService.getTeamStats(awayTeam, season));
        } catch (Exception e) {
            // Stats unavailable - context still useful
        }

        ctx.append("\n");
        return ctx.toString();
    }

    private void appendTeamStats(StringBuilder ctx, String team, Map<String, Object> stats) {
        if (stats != null) {
            ctx.append(String.format("%s: %d-%d, %.1f PPG, ATS %.1f%%, Trend: %s\n",
                team, stats.get("wins"), stats.get("losses"),
                stats.get("pointsPerGame"), stats.get("atsPercentage"),
                stats.get("bettingTrend")));
        }
    }

    private void appendTeamStatsWithSRS(StringBuilder ctx, String team, Map<String, Object> stats) {
        if (stats != null) {
            ctx.append(String.format("%s: %d-%d, %.1f PPG, ATS %.1f%%, SRS: %.1f, Trend: %s\n",
                team, stats.get("wins"), stats.get("losses"),
                stats.get("pointsPerGame"), stats.get("atsPercentage"),
                stats.get("srsRating"), stats.get("bettingTrend")));
        }
    }

    private double getMinConfidenceThreshold(String sport) {
        if ("WCBB".equals(sport)) {
            return 6.0; // WCBB markets are less efficient — lower threshold captures more edges
        }
        return 7.0;
    }

    private double calculateKellyForBet(Map<String, Object> gameOdds, double confidence, String sport) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> bestOdds = (Map<String, Object>) gameOdds.get("bestOdds");
            if (bestOdds == null) return 0.0;

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

            if (odds == null) return 0.0;

            double winProbability = 0.50 + (confidence - 7.0) * 0.05;
            winProbability = Math.max(0.50, Math.min(0.70, winProbability));

            Map<String, Object> kellyResult = kellyCriterionService.calculateKelly(odds, winProbability, true, sport);
            Double kellyPercentage = (Double) kellyResult.get("kellyPercentage");
            return kellyPercentage != null ? kellyPercentage : 0.0;

        } catch (Exception e) {
            log.error("Error calculating Kelly: {}", e.getMessage());
            return 0.0;
        }
    }
}
