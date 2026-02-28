package com.coltwarren.sports_betting_analytics.service.soccer;

import com.coltwarren.sports_betting_analytics.model.soccer.SoccerFixture;
import com.coltwarren.sports_betting_analytics.model.soccer.TeamStanding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Soccer Data Aggregator Service
 *
 * Combines data from:
 * - API-Football (fixtures, standings, team stats)
 * - The Odds API (3-way odds, totals)
 *
 * This service provides a unified view of soccer data with complete
 * fixture information including odds from Missouri-licensed sportsbooks.
 */
@Slf4j
@Service
public class SoccerDataAggregatorService {

    private final ApiFootballService apiFootballService;

    private final SoccerOddsService soccerOddsService;

    public SoccerDataAggregatorService(ApiFootballService apiFootballService,
                                       SoccerOddsService soccerOddsService) {
        this.apiFootballService = apiFootballService;
        this.soccerOddsService = soccerOddsService;
    }

    // League to sport key mapping
    private static final Map<String, String> LEAGUE_TO_SPORT_KEY = Map.of(
        "MLS", "soccer_usa_mls",
        "PREMIER_LEAGUE", "soccer_epl",
        "CHAMPIONS_LEAGUE", "soccer_uefa_champs_league",
        "LA_LIGA", "soccer_spain_la_liga",
        "BUNDESLIGA", "soccer_germany_bundesliga",
        "SERIE_A", "soccer_italy_serie_a",
        "LIGUE_1", "soccer_france_ligue_one"
    );

    /**
     * Get complete match data: fixtures + odds
     *
     * @param league The league name (MLS, PREMIER_LEAGUE, etc.)
     * @return List of fixtures with merged odds data
     */
    public List<SoccerFixture> getCompleteFixtures(String league) {
        // Step 1: Get fixtures from API-Football
        List<SoccerFixture> fixtures = apiFootballService.getUpcomingFixtures(league);

        if (fixtures.isEmpty()) {
            log.info("No fixtures found for {}", league);
            return fixtures;
        }

        // Step 2: Get odds from The Odds API
        String sportKey = LEAGUE_TO_SPORT_KEY.get(league);
        if (sportKey == null) {
            log.error("Unknown league for odds: {}", league);
            return fixtures;
        }

        Map<String, SoccerOddsService.SoccerOdds> oddsMap = soccerOddsService.getSoccerOdds(sportKey);

        // Step 3: Merge data
        for (SoccerFixture fixture : fixtures) {
            mergeOddsIntoFixture(fixture, oddsMap);
        }

        log.info("Aggregated {} complete fixtures for {}", fixtures.size(), league);
        return fixtures;
    }

    /**
     * Get all leagues with complete data
     *
     * @return Map of league -> list of fixtures with odds
     */
    public Map<String, List<SoccerFixture>> getAllCompleteFixtures() {
        Map<String, List<SoccerFixture>> allData = new LinkedHashMap<>();

        // Priority order: MLS first (Missouri teams), then major European leagues
        List<String> leagues = Arrays.asList(
            "MLS",
            "PREMIER_LEAGUE",
            "CHAMPIONS_LEAGUE",
            "LA_LIGA",
            "BUNDESLIGA",
            "SERIE_A",
            "LIGUE_1"
        );

        for (String league : leagues) {
            try {
                List<SoccerFixture> fixtures = getCompleteFixtures(league);
                if (!fixtures.isEmpty()) {
                    allData.put(league, fixtures);
                }
            } catch (Exception e) {
                log.error("Error loading {}: {}", league, e.getMessage());
            }
        }

        return allData;
    }

    /**
     * Get Missouri local team games with complete data
     *
     * @return List of fixtures featuring Sporting KC or St. Louis City SC
     */
    public List<SoccerFixture> getLocalTeamGamesWithOdds() {
        List<SoccerFixture> localGames = apiFootballService.getLocalTeamGames();

        if (localGames.isEmpty()) {
            return localGames;
        }

        // Get MLS odds
        Map<String, SoccerOddsService.SoccerOdds> mlsOdds = soccerOddsService.getSoccerOdds("soccer_usa_mls");

        // Merge odds into local games
        for (SoccerFixture fixture : localGames) {
            mergeOddsIntoFixture(fixture, mlsOdds);
        }

        return localGames;
    }

    /**
     * Get fixtures with odds only (skip API-Football to save rate limits)
     *
     * This is useful when we only need odds data and don't need
     * detailed fixture info from API-Football.
     */
    public List<SoccerFixture> getFixturesFromOddsOnly(String league) {
        String sportKey = LEAGUE_TO_SPORT_KEY.get(league);
        if (sportKey == null) {
            return Collections.emptyList();
        }

        Map<String, SoccerOddsService.SoccerOdds> oddsMap = soccerOddsService.getSoccerOdds(sportKey);
        List<SoccerFixture> fixtures = new ArrayList<>();

        for (Map.Entry<String, SoccerOddsService.SoccerOdds> entry : oddsMap.entrySet()) {
            SoccerOddsService.SoccerOdds odds = entry.getValue();

            SoccerFixture fixture = new SoccerFixture();
            fixture.setLeague(league);
            fixture.setHomeTeam(odds.getHomeTeam());
            fixture.setAwayTeam(odds.getAwayTeam());

            // Parse and set kickoff time from commence_time (ISO 8601 format)
            String commenceTime = odds.getCommenceTime();
            if (commenceTime != null && !commenceTime.isEmpty()) {
                try {
                    ZonedDateTime zdt = ZonedDateTime.parse(commenceTime);
                    fixture.setKickoffTime(zdt.toLocalDateTime());
                    log.info("{} vs {} - Kickoff: {}", odds.getHomeTeam(), odds.getAwayTeam(), fixture.getKickoffTime());
                } catch (Exception e) {
                    log.error("Error parsing commence_time: {} - {}", commenceTime, e.getMessage());
                }
            }

            // Set odds
            fixture.setHomeWinOdds(odds.getHomeWinOdds());
            fixture.setDrawOdds(odds.getDrawOdds());
            fixture.setAwayWinOdds(odds.getAwayWinOdds());
            fixture.setBestHomeBook(odds.getBestHomeBook());
            fixture.setBestDrawBook(odds.getBestDrawBook());
            fixture.setBestAwayBook(odds.getBestAwayBook());
            fixture.setOverOdds(odds.getOverOdds());
            fixture.setUnderOdds(odds.getUnderOdds());
            fixture.setGoalLine(odds.getGoalLine());
            fixture.setBestOverBook(odds.getBestOverBook());
            fixture.setBestUnderBook(odds.getBestUnderBook());

            // Check for local team
            fixture.checkLocalTeam();

            fixtures.add(fixture);
        }

        // Sort by kickoff time (soonest first)
        fixtures.sort((a, b) -> {
            if (a.getKickoffTime() == null && b.getKickoffTime() == null) return 0;
            if (a.getKickoffTime() == null) return 1;
            if (b.getKickoffTime() == null) return -1;
            return a.getKickoffTime().compareTo(b.getKickoffTime());
        });

        return fixtures;
    }

    /**
     * Get all fixtures from odds only (conserves API-Football rate limits)
     */
    public Map<String, List<SoccerFixture>> getAllFixturesFromOddsOnly() {
        Map<String, List<SoccerFixture>> allData = new LinkedHashMap<>();

        for (String league : LEAGUE_TO_SPORT_KEY.keySet()) {
            try {
                List<SoccerFixture> fixtures = getFixturesFromOddsOnly(league);
                if (!fixtures.isEmpty()) {
                    allData.put(league, fixtures);
                }
            } catch (Exception e) {
                log.error("Error loading odds for {}: {}", league, e.getMessage());
            }
        }

        return allData;
    }

    /**
     * Merge odds data into a fixture
     */
    private void mergeOddsIntoFixture(SoccerFixture fixture, Map<String, SoccerOddsService.SoccerOdds> oddsMap) {
        // Try to find matching odds
        SoccerOddsService.SoccerOdds odds = findOddsForFixture(fixture, oddsMap);

        if (odds != null) {
            // 3-Way odds
            fixture.setHomeWinOdds(odds.getHomeWinOdds());
            fixture.setDrawOdds(odds.getDrawOdds());
            fixture.setAwayWinOdds(odds.getAwayWinOdds());
            fixture.setBestHomeBook(odds.getBestHomeBook());
            fixture.setBestDrawBook(odds.getBestDrawBook());
            fixture.setBestAwayBook(odds.getBestAwayBook());

            // Totals
            fixture.setOverOdds(odds.getOverOdds());
            fixture.setUnderOdds(odds.getUnderOdds());
            fixture.setGoalLine(odds.getGoalLine());
            fixture.setBestOverBook(odds.getBestOverBook());
            fixture.setBestUnderBook(odds.getBestUnderBook());
        }
    }

    /**
     * Find odds for a fixture using fuzzy team name matching
     */
    private SoccerOddsService.SoccerOdds findOddsForFixture(
            SoccerFixture fixture,
            Map<String, SoccerOddsService.SoccerOdds> oddsMap) {

        // Try exact match first
        String exactKey = fixture.getHomeTeam() + " vs " + fixture.getAwayTeam();
        if (oddsMap.containsKey(exactKey)) {
            return oddsMap.get(exactKey);
        }

        // Try fuzzy matching
        for (Map.Entry<String, SoccerOddsService.SoccerOdds> entry : oddsMap.entrySet()) {
            SoccerOddsService.SoccerOdds odds = entry.getValue();

            if (teamsMatch(fixture.getHomeTeam(), odds.getHomeTeam()) &&
                teamsMatch(fixture.getAwayTeam(), odds.getAwayTeam())) {
                return odds;
            }
        }

        return null;
    }

    /**
     * Fuzzy team name matching
     *
     * Handles variations like:
     * - "Sporting Kansas City" vs "Sporting KC"
     * - "St. Louis City SC" vs "St. Louis City"
     * - "Manchester United" vs "Man United" vs "Man Utd"
     */
    private boolean teamsMatch(String team1, String team2) {
        if (team1 == null || team2 == null) return false;

        String t1 = normalizeTeamName(team1);
        String t2 = normalizeTeamName(team2);

        return t1.contains(t2) || t2.contains(t1);
    }

    /**
     * Normalize team name for matching
     */
    private String normalizeTeamName(String name) {
        return name.toLowerCase()
            .replaceAll("fc$|sc$|cf$", "")  // Remove common suffixes
            .replaceAll("[^a-z0-9]", "")     // Remove non-alphanumeric
            .trim();
    }

    /**
     * Get standings for a specific league
     */
    public List<TeamStanding> getStandings(String league) {
        return apiFootballService.getStandings(league);
    }

    /**
     * Get API usage statistics
     */
    public Map<String, Object> getApiStats() {
        return apiFootballService.getApiUsageStats();
    }
}
