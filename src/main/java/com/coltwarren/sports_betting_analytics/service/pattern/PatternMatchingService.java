package com.coltwarren.sports_betting_analytics.service.pattern;

import com.coltwarren.sports_betting_analytics.model.pattern.BettingPattern;
import com.coltwarren.sports_betting_analytics.model.pattern.PatternMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Pattern Matching Service
 *
 * Matches discovered patterns against today's games to find betting opportunities.
 * Integrates with live odds data to identify actionable bets.
 */
@Service
@Slf4j
public class PatternMatchingService {

    @Autowired
    private PatternDiscoveryService patternDiscoveryService;

    // Cache discovered patterns
    private Map<String, List<BettingPattern>> patternCache = new HashMap<>();

    /**
     * Find all pattern matches for today's games
     */
    public List<PatternMatch> findPatternMatches(String league, List<GameOdds> todaysGames) {
        List<PatternMatch> matches = new ArrayList<>();

        // Get patterns for this league (discover or use cache)
        List<BettingPattern> patterns = getPatterns(league);

        if (patterns.isEmpty()) {
            log.info("No profitable patterns found for {}", league);
            return matches;
        }

        log.info("Checking {} games against {} patterns", todaysGames.size(), patterns.size());

        for (GameOdds game : todaysGames) {
            for (BettingPattern pattern : patterns) {
                PatternMatch match = checkPatternMatch(game, pattern);
                if (match != null) {
                    matches.add(match);
                }
            }
        }

        // Sort by confidence
        matches.sort(Comparator.comparing(PatternMatch::getCombinedConfidence).reversed());

        return matches;
    }

    /**
     * Check if a game matches a pattern
     */
    private PatternMatch checkPatternMatch(GameOdds game, BettingPattern pattern) {
        Double relevantOdds = getRelevantOdds(game, pattern.getBetType());
        if (relevantOdds == null) return null;

        // Check odds range
        if (pattern.getMinOdds() != null && relevantOdds < pattern.getMinOdds()) return null;
        if (pattern.getMaxOdds() != null && relevantOdds >= pattern.getMaxOdds()) return null;

        // Match found!
        PatternMatch match = new PatternMatch();
        match.setGameId(game.getGameId());
        match.setHomeTeam(game.getHomeTeam());
        match.setAwayTeam(game.getAwayTeam());
        match.setGameTime(game.getGameTime());
        match.setSport("Soccer");
        match.setLeague(pattern.getLeague());
        match.setPattern(pattern);
        match.setRecommendedBet(formatBetRecommendation(pattern.getBetType(), game));
        match.setCurrentOdds(relevantOdds);

        // Calculate expected value
        double impliedProb = 1.0 / relevantOdds;
        double estimatedProb = pattern.getWinRate() / 100.0;
        double ev = (estimatedProb * (relevantOdds - 1)) - ((1 - estimatedProb) * 1);
        match.setExpectedValue(ev * 100);

        // Calculate Kelly
        double kelly = (estimatedProb * relevantOdds - 1) / (relevantOdds - 1);
        match.setSuggestedKelly(Math.max(0, Math.min(0.05, kelly * 0.5))); // Half-Kelly, max 5%

        match.setMatchConfidence(pattern.getConfidence());
        match.setMatchReason(String.format("Pattern: %s (%.1f%% ROI over %d games)",
            pattern.getName(), pattern.getRoi(), pattern.getSampleSize()));

        // Combined confidence (can be enhanced with weather/injuries/public betting)
        match.setCombinedConfidence(pattern.getConfidence());

        return match;
    }

    /**
     * Get patterns for a league (from cache or discover)
     */
    private List<BettingPattern> getPatterns(String league) {
        if (!patternCache.containsKey(league)) {
            patternCache.put(league, patternDiscoveryService.discoverPatterns(league));
        }
        return patternCache.get(league);
    }

    private Double getRelevantOdds(GameOdds game, String betType) {
        return switch (betType) {
            case "H" -> game.getHomeOdds();
            case "D" -> game.getDrawOdds();
            case "A" -> game.getAwayOdds();
            case "OVER_2.5" -> game.getOver25Odds();
            case "UNDER_2.5" -> game.getUnder25Odds();
            default -> null;
        };
    }

    private String formatBetRecommendation(String betType, GameOdds game) {
        return switch (betType) {
            case "H" -> game.getHomeTeam() + " to Win";
            case "D" -> "Draw";
            case "A" -> game.getAwayTeam() + " to Win";
            case "OVER_2.5" -> "Over 2.5 Goals";
            case "UNDER_2.5" -> "Under 2.5 Goals";
            case "DOUBLE_CHANCE_HD" -> game.getHomeTeam() + " or Draw";
            default -> betType;
        };
    }

    /**
     * Refresh pattern cache for a league
     */
    public void refreshPatterns(String league) {
        patternCache.remove(league);
        getPatterns(league);
    }

    /**
     * Clear all cached patterns
     */
    public void clearCache() {
        patternCache.clear();
    }

    /**
     * Get cached patterns for a league
     */
    public List<BettingPattern> getCachedPatterns(String league) {
        return patternCache.getOrDefault(league, new ArrayList<>());
    }

    /**
     * Inner class for game odds data
     */
    public static class GameOdds {
        private String gameId;
        private String homeTeam;
        private String awayTeam;
        private LocalDateTime gameTime;
        private Double homeOdds;
        private Double drawOdds;
        private Double awayOdds;
        private Double over25Odds;
        private Double under25Odds;

        // Getters and setters
        public String getGameId() { return gameId; }
        public void setGameId(String gameId) { this.gameId = gameId; }
        public String getHomeTeam() { return homeTeam; }
        public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }
        public String getAwayTeam() { return awayTeam; }
        public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }
        public LocalDateTime getGameTime() { return gameTime; }
        public void setGameTime(LocalDateTime gameTime) { this.gameTime = gameTime; }
        public Double getHomeOdds() { return homeOdds; }
        public void setHomeOdds(Double homeOdds) { this.homeOdds = homeOdds; }
        public Double getDrawOdds() { return drawOdds; }
        public void setDrawOdds(Double drawOdds) { this.drawOdds = drawOdds; }
        public Double getAwayOdds() { return awayOdds; }
        public void setAwayOdds(Double awayOdds) { this.awayOdds = awayOdds; }
        public Double getOver25Odds() { return over25Odds; }
        public void setOver25Odds(Double over25Odds) { this.over25Odds = over25Odds; }
        public Double getUnder25Odds() { return under25Odds; }
        public void setUnder25Odds(Double under25Odds) { this.under25Odds = under25Odds; }
    }
}
