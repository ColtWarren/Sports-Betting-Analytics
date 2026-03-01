package com.coltwarren.sports_betting_analytics.service.soccer;

import com.coltwarren.sports_betting_analytics.model.soccer.SoccerFixture;
import com.coltwarren.sports_betting_analytics.model.soccer.TeamStanding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API-Football Integration Service
 *
 * FREE TIER: 100 requests/day - must be used wisely
 *
 * Priority Leagues:
 * 1. MLS (USA) - League ID: 253 (Missouri teams!)
 * 2. Premier League (England) - League ID: 39
 * 3. Champions League - League ID: 2
 * 4. La Liga (Spain) - League ID: 140
 * 5. Bundesliga (Germany) - League ID: 78
 * 6. Serie A (Italy) - League ID: 135
 * 7. Ligue 1 (France) - League ID: 61
 *
 * Missouri Local Teams:
 * - Sporting Kansas City (MLS)
 * - St. Louis City SC (MLS)
 */
@Slf4j
@Service
public class ApiFootballService {

    @Value("${apifootball.api.key:}")
    private String apiKey;

    private final WebClient webClient;

    // Cache for API responses (5 minute TTL to preserve rate limit)
    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    // API rate limit tracking
    private int dailyRequestCount = 0;
    private LocalDate lastResetDate = LocalDate.now();
    private static final int DAILY_LIMIT = 100;

    // League IDs mapping
    private static final Map<String, Integer> LEAGUE_IDS = Map.of(
        "MLS", 253,
        "PREMIER_LEAGUE", 39,
        "CHAMPIONS_LEAGUE", 2,
        "LA_LIGA", 140,
        "BUNDESLIGA", 78,
        "SERIE_A", 135,
        "LIGUE_1", 61
    );

    // Missouri local team IDs
    private static final Set<String> MISSOURI_TEAMS = Set.of(
        "Sporting Kansas City",
        "Sporting KC",
        "St. Louis City SC",
        "St. Louis City",
        "STL City SC"
    );

    public ApiFootballService() {
        this.webClient = WebClient.builder()
            .baseUrl("https://v3.football.api-sports.io")
            .build();
    }

    /**
     * Reset daily request count at midnight
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyCount() {
        dailyRequestCount = 0;
        lastResetDate = LocalDate.now();
        log.info("API-Football daily request count reset");
    }

    /**
     * Check if we have requests remaining
     */
    private boolean canMakeRequest() {
        // Reset if it's a new day
        if (!LocalDate.now().equals(lastResetDate)) {
            dailyRequestCount = 0;
            lastResetDate = LocalDate.now();
        }

        if (dailyRequestCount >= DAILY_LIMIT - 10) {
            log.warn("Approaching API-Football daily limit! {}/100", dailyRequestCount);
        }

        return dailyRequestCount < DAILY_LIMIT;
    }

    /**
     * Track API request
     */
    private void trackRequest() {
        dailyRequestCount++;
        log.info("API-Football requests today: {}/{}", dailyRequestCount, DAILY_LIMIT);
    }

    /**
     * Get today's fixtures for a specific league
     */
    public List<SoccerFixture> getTodayFixtures(String league) {
        String cacheKey = "fixtures_" + league + "_" + LocalDate.now();

        // Check cache first
        CachedResponse cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("Cache HIT for {} fixtures", league);
            return (List<SoccerFixture>) cached.data;
        }

        if (!canMakeRequest()) {
            log.error("API-Football daily limit reached, returning empty list");
            return Collections.emptyList();
        }

        Integer leagueId = LEAGUE_IDS.get(league);
        if (leagueId == null) {
            log.error("Unknown league: {}", league);
            return Collections.emptyList();
        }

        try {
            String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/fixtures")
                    .queryParam("league", leagueId)
                    .queryParam("date", today)
                    .queryParam("season", getCurrentSeason())
                    .build())
                .header("x-apisports-key", apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(10));

            trackRequest();

            List<SoccerFixture> fixtures = parseFixtures(response, league);

            // Cache the result
            cache.put(cacheKey, new CachedResponse(fixtures));

            return fixtures;

        } catch (Exception e) {
            log.error("Error fetching {} fixtures: {}", league, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get upcoming fixtures for a league (next 7 days)
     */
    public List<SoccerFixture> getUpcomingFixtures(String league) {
        String cacheKey = "upcoming_" + league;

        CachedResponse cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return (List<SoccerFixture>) cached.data;
        }

        if (!canMakeRequest()) {
            return Collections.emptyList();
        }

        Integer leagueId = LEAGUE_IDS.get(league);
        if (leagueId == null) {
            return Collections.emptyList();
        }

        try {
            String from = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            String to = LocalDate.now().plusDays(7).format(DateTimeFormatter.ISO_DATE);

            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/fixtures")
                    .queryParam("league", leagueId)
                    .queryParam("from", from)
                    .queryParam("to", to)
                    .queryParam("season", getCurrentSeason())
                    .build())
                .header("x-apisports-key", apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(10));

            trackRequest();

            List<SoccerFixture> fixtures = parseFixtures(response, league);
            cache.put(cacheKey, new CachedResponse(fixtures));

            return fixtures;

        } catch (Exception e) {
            log.error("Error fetching upcoming {} fixtures: {}", league, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get league standings
     */
    public List<TeamStanding> getStandings(String league) {
        String cacheKey = "standings_" + league;

        CachedResponse cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return (List<TeamStanding>) cached.data;
        }

        if (!canMakeRequest()) {
            return Collections.emptyList();
        }

        Integer leagueId = LEAGUE_IDS.get(league);
        if (leagueId == null) {
            return Collections.emptyList();
        }

        try {
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/standings")
                    .queryParam("league", leagueId)
                    .queryParam("season", getCurrentSeason())
                    .build())
                .header("x-apisports-key", apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(10));

            trackRequest();

            List<TeamStanding> standings = parseStandings(response, league);
            cache.put(cacheKey, new CachedResponse(standings));

            return standings;

        } catch (Exception e) {
            log.error("Error fetching {} standings: {}", league, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get all Missouri local team games (Sporting KC & St. Louis City SC)
     */
    public List<SoccerFixture> getLocalTeamGames() {
        List<SoccerFixture> allMLSFixtures = getUpcomingFixtures("MLS");

        List<SoccerFixture> localGames = new ArrayList<>();
        for (SoccerFixture fixture : allMLSFixtures) {
            if (isMissouriTeam(fixture.getHomeTeam()) || isMissouriTeam(fixture.getAwayTeam())) {
                fixture.setLocalTeam(true);
                if (isMissouriTeam(fixture.getHomeTeam())) {
                    fixture.setLocalTeamName(fixture.getHomeTeam());
                } else {
                    fixture.setLocalTeamName(fixture.getAwayTeam());
                }
                localGames.add(fixture);
            }
        }

        return localGames;
    }

    /**
     * Check if team is a Missouri team
     */
    private boolean isMissouriTeam(String teamName) {
        if (teamName == null) return false;
        String lower = teamName.toLowerCase();
        return lower.contains("sporting k") ||
               lower.contains("kansas city") ||
               lower.contains("st. louis") ||
               lower.contains("stl city");
    }

    /**
     * Parse fixtures from API response
     */
    private List<SoccerFixture> parseFixtures(Map<String, Object> response, String league) {
        List<SoccerFixture> fixtures = new ArrayList<>();

        if (response == null) return fixtures;

        List<Map<String, Object>> responseList = (List<Map<String, Object>>) response.get("response");
        if (responseList == null) return fixtures;

        for (Map<String, Object> fixtureData : responseList) {
            try {
                SoccerFixture fixture = new SoccerFixture();
                fixture.setLeague(league);

                // Parse fixture info
                Map<String, Object> fixtureInfo = (Map<String, Object>) fixtureData.get("fixture");
                if (fixtureInfo != null) {
                    fixture.setFixtureId(((Number) fixtureInfo.get("id")).longValue());

                    String dateStr = (String) fixtureInfo.get("date");
                    if (dateStr != null) {
                        ZonedDateTime zdt = ZonedDateTime.parse(dateStr);
                        fixture.setKickoffTime(zdt.toLocalDateTime());
                    }

                    Map<String, Object> venueInfo = (Map<String, Object>) fixtureInfo.get("venue");
                    if (venueInfo != null) {
                        fixture.setVenue((String) venueInfo.get("name"));
                    }

                    Map<String, Object> statusInfo = (Map<String, Object>) fixtureInfo.get("status");
                    if (statusInfo != null) {
                        fixture.setStatus((String) statusInfo.get("short"));
                    }
                }

                // Parse teams
                Map<String, Object> teams = (Map<String, Object>) fixtureData.get("teams");
                if (teams != null) {
                    Map<String, Object> home = (Map<String, Object>) teams.get("home");
                    Map<String, Object> away = (Map<String, Object>) teams.get("away");

                    if (home != null) {
                        fixture.setHomeTeam((String) home.get("name"));
                        fixture.setHomeTeamLogo((String) home.get("logo"));
                    }
                    if (away != null) {
                        fixture.setAwayTeam((String) away.get("name"));
                        fixture.setAwayTeamLogo((String) away.get("logo"));
                    }
                }

                // Parse goals (if game started)
                Map<String, Object> goals = (Map<String, Object>) fixtureData.get("goals");
                if (goals != null) {
                    Object homeGoals = goals.get("home");
                    Object awayGoals = goals.get("away");
                    if (homeGoals != null) fixture.setHomeScore(((Number) homeGoals).intValue());
                    if (awayGoals != null) fixture.setAwayScore(((Number) awayGoals).intValue());
                }

                // Check if it's a Missouri team game
                fixture.checkLocalTeam();

                fixtures.add(fixture);

            } catch (Exception e) {
                log.error("Error parsing fixture: {}", e.getMessage());
            }
        }

        return fixtures;
    }

    /**
     * Parse standings from API response
     */
    private List<TeamStanding> parseStandings(Map<String, Object> response, String league) {
        List<TeamStanding> standings = new ArrayList<>();

        if (response == null) return standings;

        List<Map<String, Object>> responseList = (List<Map<String, Object>>) response.get("response");
        if (responseList == null || responseList.isEmpty()) return standings;

        Map<String, Object> leagueData = (Map<String, Object>) responseList.get(0).get("league");
        if (leagueData == null) return standings;

        List<List<Map<String, Object>>> standingsData = (List<List<Map<String, Object>>>) leagueData.get("standings");
        if (standingsData == null || standingsData.isEmpty()) return standings;

        for (Map<String, Object> teamData : standingsData.get(0)) {
            try {
                TeamStanding standing = new TeamStanding();
                standing.setLeague(league);
                standing.setPosition(((Number) teamData.get("rank")).intValue());
                standing.setPoints(((Number) teamData.get("points")).intValue());

                Map<String, Object> team = (Map<String, Object>) teamData.get("team");
                if (team != null) {
                    standing.setTeamName((String) team.get("name"));
                    standing.setTeamLogo((String) team.get("logo"));
                }

                Map<String, Object> all = (Map<String, Object>) teamData.get("all");
                if (all != null) {
                    standing.setPlayed(((Number) all.get("played")).intValue());
                    standing.setWon(((Number) all.get("win")).intValue());
                    standing.setDrawn(((Number) all.get("draw")).intValue());
                    standing.setLost(((Number) all.get("lose")).intValue());

                    Map<String, Object> goalsAll = (Map<String, Object>) all.get("goals");
                    if (goalsAll != null) {
                        standing.setGoalsFor(((Number) goalsAll.get("for")).intValue());
                        standing.setGoalsAgainst(((Number) goalsAll.get("against")).intValue());
                    }
                }

                standing.setForm((String) teamData.get("form"));
                standing.setGoalDifference(((Number) teamData.get("goalsDiff")).intValue());

                standings.add(standing);

            } catch (Exception e) {
                log.error("Error parsing standing: {}", e.getMessage());
            }
        }

        return standings;
    }

    /**
     * Get current season year based on league type
     */
    private int getCurrentSeason() {
        LocalDate now = LocalDate.now();
        // MLS and most leagues use the calendar year
        // European leagues span 2 years (e.g., 2025-2026 season = "2025")
        // For simplicity, use current year for MLS, current year for others if before July, otherwise current year
        if (now.getMonthValue() >= 7) {
            return now.getYear();
        }
        return now.getYear() - 1;
    }

    /**
     * Get API usage stats
     */
    public Map<String, Object> getApiUsageStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("dailyRequestCount", dailyRequestCount);
        stats.put("dailyLimit", DAILY_LIMIT);
        stats.put("remainingRequests", DAILY_LIMIT - dailyRequestCount);
        stats.put("cacheSize", cache.size());
        stats.put("lastResetDate", lastResetDate.toString());
        return stats;
    }

    /**
     * Cache entry with TTL
     */
    private static class CachedResponse {
        final Object data;
        final long timestamp;

        CachedResponse(Object data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
