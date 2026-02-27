package com.coltwarren.sports_betting_analytics.service.pattern;

import com.coltwarren.sports_betting_analytics.model.historical.HistoricalMatch;
import com.coltwarren.sports_betting_analytics.model.pattern.BettingPattern;
import com.coltwarren.sports_betting_analytics.repository.HistoricalMatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pattern Discovery Service
 *
 * Automatically discovers profitable betting patterns from historical data.
 * Analyzes odds ranges, draw rates, underdog performance, seasonal trends, and more.
 */
@Service
@Slf4j
public class PatternDiscoveryService {

    private final HistoricalMatchRepository matchRepository;

    // Minimum requirements for a valid pattern
    private static final int MIN_SAMPLE_SIZE = 50;
    private static final double MIN_ROI = 3.0;  // Minimum 3% ROI
    private static final double MIN_WIN_RATE_FOR_FAVORITES = 52.0;
    private static final double MIN_WIN_RATE_FOR_UNDERDOGS = 28.0;

    public PatternDiscoveryService(HistoricalMatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    /**
     * Discover all profitable patterns in a league
     */
    public List<BettingPattern> discoverPatterns(String league) {
        List<BettingPattern> patterns = new ArrayList<>();

        List<HistoricalMatch> matches = matchRepository.findByLeagueOrderByDateAsc(league);
        if (matches.isEmpty()) {
            log.warn("No matches found for pattern discovery in {}", league);
            return patterns;
        }

        log.info("Discovering patterns from {} matches in {}", matches.size(), league);

        // Pattern 1: Odds range analysis (we know 3.0-4.0 works!)
        patterns.addAll(discoverOddsRangePatterns(matches, league));

        // Pattern 2: Draw patterns by odds
        patterns.addAll(discoverDrawPatterns(matches, league));

        // Pattern 3: Home underdog patterns
        patterns.addAll(discoverHomeUnderdogPatterns(matches, league));

        // Pattern 4: Away favorite patterns
        patterns.addAll(discoverAwayFavoritePatterns(matches, league));

        // Pattern 5: Seasonal patterns (month-based)
        patterns.addAll(discoverSeasonalPatterns(matches, league));

        // Pattern 6: Goal patterns (over/under)
        patterns.addAll(discoverGoalPatterns(matches, league));

        // Filter to only profitable, statistically significant patterns
        List<BettingPattern> profitable = patterns.stream()
            .filter(p -> p.getSampleSize() >= MIN_SAMPLE_SIZE)
            .filter(p -> p.getRoi() >= MIN_ROI)
            .filter(p -> p.getIsStatisticallySignificant())
            .sorted(Comparator.comparing(BettingPattern::getRoi).reversed())
            .collect(Collectors.toList());

        log.info("Found {} profitable patterns in {}", profitable.size(), league);
        return profitable;
    }

    /**
     * Pattern: Bet type by odds range
     */
    private List<BettingPattern> discoverOddsRangePatterns(List<HistoricalMatch> matches, String league) {
        List<BettingPattern> patterns = new ArrayList<>();

        // Define odds ranges to test
        double[][] oddsRanges = {
            {1.0, 1.5}, {1.5, 2.0}, {2.0, 2.5}, {2.5, 3.0},
            {3.0, 3.5}, {3.5, 4.0}, {4.0, 5.0}, {5.0, 10.0}
        };

        String[] betTypes = {"H", "D", "A"};

        for (double[] range : oddsRanges) {
            for (String betType : betTypes) {
                BettingPattern pattern = analyzeOddsRange(matches, league, betType, range[0], range[1]);
                if (pattern != null) {
                    patterns.add(pattern);
                }
            }
        }

        return patterns;
    }

    private BettingPattern analyzeOddsRange(List<HistoricalMatch> matches, String league,
                                           String betType, double minOdds, double maxOdds) {
        List<HistoricalMatch> filtered = matches.stream()
            .filter(m -> {
                Double odds = getOddsForBetType(m, betType);
                return odds != null && odds >= minOdds && odds < maxOdds;
            })
            .collect(Collectors.toList());

        if (filtered.size() < MIN_SAMPLE_SIZE) return null;

        int wins = 0;
        double totalProfit = 0;
        double totalOdds = 0;

        for (HistoricalMatch m : filtered) {
            Double odds = getOddsForBetType(m, betType);
            boolean won = m.getResult().equals(betType);

            if (won) {
                wins++;
                totalProfit += (odds - 1); // Profit on 1 unit
            } else {
                totalProfit -= 1; // Lost 1 unit
            }
            totalOdds += odds;
        }

        double roi = (totalProfit / filtered.size()) * 100;
        double winRate = (double) wins / filtered.size() * 100;
        double avgOdds = totalOdds / filtered.size();

        // Check if pattern is profitable
        if (roi < MIN_ROI) return null;

        String betName = betType.equals("H") ? "Home" : betType.equals("A") ? "Away" : "Draw";

        BettingPattern pattern = new BettingPattern();
        pattern.setPatternId(String.format("%s_%s_%.1f-%.1f", league, betType, minOdds, maxOdds));
        pattern.setName(String.format("%s wins at %.1f-%.1f odds", betName, minOdds, maxOdds));
        pattern.setDescription(String.format("Bet on %s when odds are between %.2f and %.2f", betName, minOdds, maxOdds));
        pattern.setSport("Soccer");
        pattern.setLeague(league);
        pattern.setBetType(betType);
        pattern.setMinOdds(minOdds);
        pattern.setMaxOdds(maxOdds);
        pattern.setSampleSize(filtered.size());
        pattern.setWins(wins);
        pattern.setLosses(filtered.size() - wins);
        pattern.setWinRate(winRate);
        pattern.setRoi(roi);
        pattern.setAvgOdds(avgOdds);
        pattern.setProfitUnits(totalProfit);
        pattern.setConditions(List.of(
            String.format("Odds between %.2f and %.2f", minOdds, maxOdds),
            String.format("Bet type: %s", betName)
        ));
        pattern.calculateStats();
        pattern.setIsActive(true);
        pattern.setConfidence(calculateConfidence(pattern));

        return pattern;
    }

    /**
     * Pattern: Draw-specific analysis
     */
    private List<BettingPattern> discoverDrawPatterns(List<HistoricalMatch> matches, String league) {
        List<BettingPattern> patterns = new ArrayList<>();

        // Draws when home is slight favorite (1.8-2.5 home odds)
        List<HistoricalMatch> slightFavorite = matches.stream()
            .filter(m -> m.getHomeOdds() != null && m.getHomeOdds() >= 1.8 && m.getHomeOdds() <= 2.5)
            .filter(m -> m.getDrawOdds() != null && m.getDrawOdds() >= 3.0 && m.getDrawOdds() <= 4.0)
            .collect(Collectors.toList());

        BettingPattern drawPattern = analyzeDraws(slightFavorite, league,
            "Draws in tight matches", "Home slight favorite (1.8-2.5), Draw at 3.0-4.0");
        if (drawPattern != null) patterns.add(drawPattern);

        // Draws when match is very even (all odds 2.5-4.0)
        List<HistoricalMatch> evenMatches = matches.stream()
            .filter(m -> m.getHomeOdds() != null && m.getHomeOdds() >= 2.5 && m.getHomeOdds() <= 4.0)
            .filter(m -> m.getAwayOdds() != null && m.getAwayOdds() >= 2.5 && m.getAwayOdds() <= 4.0)
            .collect(Collectors.toList());

        BettingPattern evenDrawPattern = analyzeDraws(evenMatches, league,
            "Draws in even matchups", "Both teams 2.5-4.0 odds");
        if (evenDrawPattern != null) patterns.add(evenDrawPattern);

        return patterns;
    }

    private BettingPattern analyzeDraws(List<HistoricalMatch> matches, String league,
                                        String name, String condition) {
        if (matches.size() < MIN_SAMPLE_SIZE) return null;

        int wins = 0;
        double totalProfit = 0;
        double totalOdds = 0;

        for (HistoricalMatch m : matches) {
            boolean won = "D".equals(m.getResult());
            double odds = m.getDrawOdds();

            if (won) {
                wins++;
                totalProfit += (odds - 1);
            } else {
                totalProfit -= 1;
            }
            totalOdds += odds;
        }

        double roi = (totalProfit / matches.size()) * 100;
        if (roi < MIN_ROI) return null;

        BettingPattern pattern = new BettingPattern();
        pattern.setPatternId(league + "_DRAW_" + name.replaceAll(" ", "_"));
        pattern.setName(name);
        pattern.setDescription("Bet on Draw: " + condition);
        pattern.setSport("Soccer");
        pattern.setLeague(league);
        pattern.setBetType("D");
        pattern.setSampleSize(matches.size());
        pattern.setWins(wins);
        pattern.setLosses(matches.size() - wins);
        pattern.setWinRate((double) wins / matches.size() * 100);
        pattern.setRoi(roi);
        pattern.setAvgOdds(totalOdds / matches.size());
        pattern.setProfitUnits(totalProfit);
        pattern.setConditions(List.of(condition, "Bet: Draw"));
        pattern.calculateStats();
        pattern.setIsActive(true);
        pattern.setConfidence(calculateConfidence(pattern));

        return pattern;
    }

    /**
     * Pattern: Home underdog value
     */
    private List<BettingPattern> discoverHomeUnderdogPatterns(List<HistoricalMatch> matches, String league) {
        List<BettingPattern> patterns = new ArrayList<>();

        // Home underdogs at 3.0-5.0 odds
        List<HistoricalMatch> homeUnderdogs = matches.stream()
            .filter(m -> m.getHomeOdds() != null && m.getHomeOdds() >= 3.0 && m.getHomeOdds() <= 5.0)
            .collect(Collectors.toList());

        if (homeUnderdogs.size() >= MIN_SAMPLE_SIZE) {
            int wins = (int) homeUnderdogs.stream().filter(m -> "H".equals(m.getResult())).count();
            double totalProfit = 0;
            double totalOdds = 0;

            for (HistoricalMatch m : homeUnderdogs) {
                if ("H".equals(m.getResult())) {
                    totalProfit += (m.getHomeOdds() - 1);
                } else {
                    totalProfit -= 1;
                }
                totalOdds += m.getHomeOdds();
            }

            double roi = (totalProfit / homeUnderdogs.size()) * 100;

            if (roi >= MIN_ROI) {
                BettingPattern pattern = new BettingPattern();
                pattern.setPatternId(league + "_HOME_UNDERDOG_3-5");
                pattern.setName("Home Underdogs (3.0-5.0 odds)");
                pattern.setDescription("Back home teams priced as underdogs");
                pattern.setSport("Soccer");
                pattern.setLeague(league);
                pattern.setBetType("H");
                pattern.setMinOdds(3.0);
                pattern.setMaxOdds(5.0);
                pattern.setSampleSize(homeUnderdogs.size());
                pattern.setWins(wins);
                pattern.setLosses(homeUnderdogs.size() - wins);
                pattern.setWinRate((double) wins / homeUnderdogs.size() * 100);
                pattern.setRoi(roi);
                pattern.setAvgOdds(totalOdds / homeUnderdogs.size());
                pattern.setProfitUnits(totalProfit);
                pattern.setConditions(List.of("Home team odds 3.0-5.0", "Bet: Home win"));
                pattern.calculateStats();
                pattern.setIsActive(true);
                pattern.setConfidence(calculateConfidence(pattern));
                patterns.add(pattern);
            }
        }

        return patterns;
    }

    /**
     * Pattern: Away favorites (often overvalued)
     */
    private List<BettingPattern> discoverAwayFavoritePatterns(List<HistoricalMatch> matches, String league) {
        List<BettingPattern> patterns = new ArrayList<>();

        // Away heavy favorites at < 1.6 odds - often fade these
        List<HistoricalMatch> awayHeavyFavorites = matches.stream()
            .filter(m -> m.getAwayOdds() != null && m.getAwayOdds() < 1.6)
            .collect(Collectors.toList());

        if (awayHeavyFavorites.size() >= MIN_SAMPLE_SIZE) {
            // Check if fading them (betting HOME or DRAW) is profitable
            int homeWins = (int) awayHeavyFavorites.stream().filter(m -> "H".equals(m.getResult())).count();
            int draws = (int) awayHeavyFavorites.stream().filter(m -> "D".equals(m.getResult())).count();
            int awayLosses = homeWins + draws; // Times the heavy favorite didn't win

            double awayWinRate = (double) (awayHeavyFavorites.size() - awayLosses) / awayHeavyFavorites.size() * 100;

            // If away favorites win less than expected at these odds, there's value fading
            // At 1.5 odds, implied prob is 66.7%, so if they win < 65%, there's edge
            if (awayWinRate < 65) {
                // Calculate ROI of betting against (Double Chance: Home or Draw)
                double totalProfit = 0;
                for (HistoricalMatch m : awayHeavyFavorites) {
                    // Approximate double chance odds (usually around 1.5-1.8)
                    double dcOdds = 1.6; // Estimated
                    if (!"A".equals(m.getResult())) {
                        totalProfit += (dcOdds - 1);
                    } else {
                        totalProfit -= 1;
                    }
                }

                double roi = (totalProfit / awayHeavyFavorites.size()) * 100;

                if (roi >= MIN_ROI) {
                    BettingPattern pattern = new BettingPattern();
                    pattern.setPatternId(league + "_FADE_AWAY_FAVORITE");
                    pattern.setName("Fade Away Heavy Favorites");
                    pattern.setDescription("Away teams at < 1.6 odds underperform");
                    pattern.setSport("Soccer");
                    pattern.setLeague(league);
                    pattern.setBetType("DOUBLE_CHANCE_HD");
                    pattern.setMaxOdds(1.6);
                    pattern.setSampleSize(awayHeavyFavorites.size());
                    pattern.setWins(awayLosses);
                    pattern.setLosses(awayHeavyFavorites.size() - awayLosses);
                    pattern.setWinRate((double) awayLosses / awayHeavyFavorites.size() * 100);
                    pattern.setRoi(roi);
                    pattern.setProfitUnits(totalProfit);
                    pattern.setConditions(List.of("Away team odds < 1.6", "Bet: Home or Draw"));
                    pattern.calculateStats();
                    pattern.setIsActive(true);
                    pattern.setConfidence(calculateConfidence(pattern));
                    patterns.add(pattern);
                }
            }
        }

        return patterns;
    }

    /**
     * Pattern: Month/Seasonal trends
     */
    private List<BettingPattern> discoverSeasonalPatterns(List<HistoricalMatch> matches, String league) {
        List<BettingPattern> patterns = new ArrayList<>();

        // Group by month
        Map<Integer, List<HistoricalMatch>> byMonth = matches.stream()
            .filter(m -> m.getMatchDate() != null)
            .collect(Collectors.groupingBy(m -> m.getMatchDate().getMonthValue()));

        String[] monthNames = {"", "January", "February", "March", "April", "May", "June",
                               "July", "August", "September", "October", "November", "December"};

        for (Map.Entry<Integer, List<HistoricalMatch>> entry : byMonth.entrySet()) {
            int month = entry.getKey();
            List<HistoricalMatch> monthMatches = entry.getValue();

            if (monthMatches.size() < MIN_SAMPLE_SIZE) continue;

            // Check draw rate by month
            int draws = (int) monthMatches.stream().filter(m -> "D".equals(m.getResult())).count();
            double drawRate = (double) draws / monthMatches.size() * 100;

            // Average draw rate is ~25-27%, so if significantly higher, there's a pattern
            if (drawRate > 30) {
                double totalProfit = 0;
                double totalOdds = 0;
                for (HistoricalMatch m : monthMatches) {
                    if (m.getDrawOdds() == null) continue;
                    if ("D".equals(m.getResult())) {
                        totalProfit += (m.getDrawOdds() - 1);
                    } else {
                        totalProfit -= 1;
                    }
                    totalOdds += m.getDrawOdds();
                }

                double roi = (totalProfit / monthMatches.size()) * 100;

                if (roi >= MIN_ROI) {
                    BettingPattern pattern = new BettingPattern();
                    pattern.setPatternId(league + "_" + monthNames[month].toUpperCase() + "_DRAWS");
                    pattern.setName(monthNames[month] + " Draws");
                    pattern.setDescription("Higher draw rate in " + monthNames[month]);
                    pattern.setSport("Soccer");
                    pattern.setLeague(league);
                    pattern.setBetType("D");
                    pattern.setSampleSize(monthMatches.size());
                    pattern.setWins(draws);
                    pattern.setLosses(monthMatches.size() - draws);
                    pattern.setWinRate(drawRate);
                    pattern.setRoi(roi);
                    pattern.setAvgOdds(totalOdds / monthMatches.size());
                    pattern.setProfitUnits(totalProfit);
                    pattern.setConditions(List.of("Month: " + monthNames[month], "Bet: Draw"));
                    pattern.calculateStats();
                    pattern.setIsActive(true);
                    pattern.setConfidence(calculateConfidence(pattern));
                    patterns.add(pattern);
                }
            }
        }

        return patterns;
    }

    /**
     * Pattern: Over/Under goals
     */
    private List<BettingPattern> discoverGoalPatterns(List<HistoricalMatch> matches, String league) {
        List<BettingPattern> patterns = new ArrayList<>();

        // Matches with both teams as underdogs tend to go Under
        List<HistoricalMatch> bothUnderdogs = matches.stream()
            .filter(m -> m.getHomeOdds() != null && m.getHomeOdds() > 2.5)
            .filter(m -> m.getAwayOdds() != null && m.getAwayOdds() > 2.5)
            .filter(m -> m.getHomeGoals() != null && m.getAwayGoals() != null)
            .collect(Collectors.toList());

        if (bothUnderdogs.size() >= MIN_SAMPLE_SIZE) {
            int unders = (int) bothUnderdogs.stream()
                .filter(m -> (m.getHomeGoals() + m.getAwayGoals()) < 3)
                .count();

            double underRate = (double) unders / bothUnderdogs.size() * 100;

            // Average under rate is ~45-50%, so if higher, there's value
            if (underRate > 52) {
                // Estimate profit (assume under odds around 1.9 average)
                double avgUnderOdds = 1.9;
                double totalProfit = unders * (avgUnderOdds - 1) - (bothUnderdogs.size() - unders);
                double roi = (totalProfit / bothUnderdogs.size()) * 100;

                if (roi >= MIN_ROI) {
                    BettingPattern pattern = new BettingPattern();
                    pattern.setPatternId(league + "_BOTH_UNDERDOGS_UNDER");
                    pattern.setName("Under 2.5 in Even Matches");
                    pattern.setDescription("When both teams are underdogs (>2.5 odds), games tend to go Under");
                    pattern.setSport("Soccer");
                    pattern.setLeague(league);
                    pattern.setBetType("UNDER_2.5");
                    pattern.setSampleSize(bothUnderdogs.size());
                    pattern.setWins(unders);
                    pattern.setLosses(bothUnderdogs.size() - unders);
                    pattern.setWinRate(underRate);
                    pattern.setRoi(roi);
                    pattern.setAvgOdds(avgUnderOdds);
                    pattern.setProfitUnits(totalProfit);
                    pattern.setConditions(List.of("Both teams > 2.5 odds", "Bet: Under 2.5 goals"));
                    pattern.calculateStats();
                    pattern.setIsActive(true);
                    pattern.setConfidence(calculateConfidence(pattern));
                    patterns.add(pattern);
                }
            }
        }

        return patterns;
    }

    // Helper methods
    private Double getOddsForBetType(HistoricalMatch m, String betType) {
        return switch (betType) {
            case "H" -> m.getHomeOdds();
            case "D" -> m.getDrawOdds();
            case "A" -> m.getAwayOdds();
            default -> null;
        };
    }

    private Double calculateConfidence(BettingPattern pattern) {
        double confidence = 5.0; // Base

        // More sample size = more confidence
        if (pattern.getSampleSize() > 200) confidence += 1.5;
        else if (pattern.getSampleSize() > 100) confidence += 1.0;
        else if (pattern.getSampleSize() > 50) confidence += 0.5;

        // Higher ROI = more confidence
        if (pattern.getRoi() > 15) confidence += 2.0;
        else if (pattern.getRoi() > 10) confidence += 1.5;
        else if (pattern.getRoi() > 5) confidence += 1.0;

        // Statistical significance
        if (pattern.getIsStatisticallySignificant()) confidence += 1.0;

        return Math.min(10.0, confidence);
    }
}
