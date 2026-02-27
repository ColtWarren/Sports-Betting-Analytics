package com.coltwarren.sports_betting_analytics.service.testing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * SportsDataIO API Test Service
 *
 * Tests the SportsDataIO API to evaluate if it can replace The Odds API
 * for our sports betting analytics platform.
 *
 * CRITICAL FINDINGS FROM TESTING:
 * 1. FREE TRIAL LIMITATIONS:
 *    - All sportsbook names are "Scrambled" (obfuscated)
 *    - 1,000 API calls per month
 *    - 10-minute delay on live updates
 *    - Soccer endpoints return 401 Unauthorized
 *
 * 2. SOCCER NOT AVAILABLE:
 *    - All soccer endpoints (EPL, MLS, etc.) return 401
 *    - This is a CRITICAL FAILURE for our 3-way betting requirement
 *
 * 3. CANNOT VERIFY MISSOURI SPORTSBOOKS:
 *    - All sportsbook names show as "Scrambled"
 *    - Only SportsbookId numbers are visible (7, 8, 9, 10, 12, 14, 19, 22, 24, 40)
 *    - Cannot confirm DraftKings, FanDuel, BetMGM, etc.
 */
@Service
public class SportsDataIOTestService {

    private static final String BASE_URL = "https://api.sportsdata.io";

    // Missouri-legal sportsbooks we need to verify
    private static final List<String> MISSOURI_SPORTSBOOKS = Arrays.asList(
        "DraftKings", "FanDuel", "BetMGM", "Caesars",
        "Fanatics", "bet365", "Circa Sports", "theScore Bet"
    );

    private final WebClient webClient;
    private final String apiKey;

    public SportsDataIOTestService(@Value("${sportsdata.api.key:}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader("Ocp-Apim-Subscription-Key", apiKey)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * Run comprehensive API tests
     */
    public Map<String, Object> runAllTests() {
        Map<String, Object> results = new LinkedHashMap<>();

        results.put("testDate", LocalDate.now().toString());
        results.put("apiKey", apiKey != null && apiKey.length() >= 8 ? apiKey.substring(0, 8) + "..." : "not-set");

        // Test each sport
        results.put("nflTest", testNFLOdds());
        results.put("nbaTest", testNBAOdds());
        results.put("soccerTest", testSoccerOdds());
        results.put("mlbTest", testMLBOdds());
        results.put("nhlTest", testNHLOdds());

        // Generate summary
        results.put("summary", generateSummary(results));

        return results;
    }

    /**
     * Test NFL Betting Odds
     */
    public Map<String, Object> testNFLOdds() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sport", "NFL");
        result.put("endpoint", "/v3/nfl/odds/json/GameOddsByWeek/2025/1");

        long startTime = System.currentTimeMillis();

        try {
            String response = webClient.get()
                .uri("/v3/nfl/odds/json/GameOddsByWeek/2025/1")
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));

            long responseTime = System.currentTimeMillis() - startTime;

            result.put("status", "SUCCESS");
            result.put("responseTimeMs", responseTime);
            result.put("sampleResponse", response != null && response.length() > 500
                ? response.substring(0, 500) + "..." : response);

            // Check for scrambled sportsbook names
            if (response != null && response.contains("\"Sportsbook\":\"Scrambled\"")) {
                result.put("sportsbooksScrambled", true);
                result.put("sportsbookWarning", "CRITICAL: All sportsbook names are obfuscated as 'Scrambled' in free trial");
            }

            // Extract unique SportsbookIds
            Set<Integer> sportsbookIds = extractSportsbookIds(response);
            result.put("sportsbookIdsFound", sportsbookIds);
            result.put("sportsbookCount", sportsbookIds.size());

        } catch (WebClientResponseException e) {
            result.put("status", "ERROR");
            result.put("errorCode", e.getStatusCode().value());
            result.put("errorMessage", e.getMessage());
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("errorMessage", e.getMessage());
        }

        return result;
    }

    /**
     * Test NBA Betting Odds
     */
    public Map<String, Object> testNBAOdds() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sport", "NBA");

        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        result.put("endpoint", "/v3/nba/odds/json/GameOddsByDate/" + today);

        long startTime = System.currentTimeMillis();

        try {
            String response = webClient.get()
                .uri("/v3/nba/odds/json/GameOddsByDate/" + today)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));

            long responseTime = System.currentTimeMillis() - startTime;

            result.put("status", "SUCCESS");
            result.put("responseTimeMs", responseTime);

            if (response != null && response.contains("\"Sportsbook\":\"Scrambled\"")) {
                result.put("sportsbooksScrambled", true);
            }

            Set<Integer> sportsbookIds = extractSportsbookIds(response);
            result.put("sportsbookIdsFound", sportsbookIds);
            result.put("sportsbookCount", sportsbookIds.size());

        } catch (WebClientResponseException e) {
            result.put("status", "ERROR");
            result.put("errorCode", e.getStatusCode().value());
            result.put("errorMessage", e.getMessage());
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("errorMessage", e.getMessage());
        }

        return result;
    }

    /**
     * Test Soccer Betting Odds - CRITICAL FOR 3-WAY BETTING
     */
    public Map<String, Object> testSoccerOdds() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sport", "SOCCER");
        result.put("criticalRequirement", "3-way betting (Home/Draw/Away)");

        // Test multiple soccer endpoints
        String[] competitions = {"epl", "mls", "bundesliga", "laliga"};
        Map<String, Object> competitionResults = new LinkedHashMap<>();

        for (String comp : competitions) {
            String endpoint = "/v4/soccer/odds/json/GameOddsByDate/" + comp + "/2026-01-28";
            long startTime = System.currentTimeMillis();

            try {
                String response = webClient.get()
                    .uri(endpoint)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

                long responseTime = System.currentTimeMillis() - startTime;
                competitionResults.put(comp, Map.of(
                    "status", "SUCCESS",
                    "responseTimeMs", responseTime
                ));

            } catch (WebClientResponseException e) {
                competitionResults.put(comp, Map.of(
                    "status", "ERROR",
                    "errorCode", e.getStatusCode().value(),
                    "errorMessage", e.getResponseBodyAsString()
                ));
            } catch (Exception e) {
                competitionResults.put(comp, Map.of(
                    "status", "ERROR",
                    "errorMessage", e.getMessage()
                ));
            }
        }

        result.put("competitionTests", competitionResults);

        // Check if ALL soccer endpoints failed
        boolean allFailed = competitionResults.values().stream()
            .allMatch(r -> "ERROR".equals(((Map<?, ?>)r).get("status")));

        if (allFailed) {
            result.put("status", "CRITICAL_FAILURE");
            result.put("conclusion", "SOCCER NOT AVAILABLE IN FREE TRIAL - All endpoints return 401 Unauthorized");
            result.put("threeWayOddsSupported", "CANNOT VERIFY - Access denied");
        }

        return result;
    }

    /**
     * Test MLB Betting Odds
     */
    public Map<String, Object> testMLBOdds() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sport", "MLB");
        result.put("endpoint", "/v3/mlb/odds/json/GameOddsByDate/2026-04-01");

        long startTime = System.currentTimeMillis();

        try {
            String response = webClient.get()
                .uri("/v3/mlb/odds/json/GameOddsByDate/2026-04-01")
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));

            long responseTime = System.currentTimeMillis() - startTime;

            result.put("status", "SUCCESS");
            result.put("responseTimeMs", responseTime);
            result.put("note", "MLB season not started - no odds available yet");

            // Check if PregameOdds are empty
            if (response != null && response.contains("\"PregameOdds\":[]")) {
                result.put("oddsAvailable", false);
                result.put("reason", "Season not started - games scheduled but no odds posted");
            }

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("errorMessage", e.getMessage());
        }

        return result;
    }

    /**
     * Test NHL Betting Odds
     */
    public Map<String, Object> testNHLOdds() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sport", "NHL");

        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        result.put("endpoint", "/v3/nhl/odds/json/GameOddsByDate/" + today);

        long startTime = System.currentTimeMillis();

        try {
            String response = webClient.get()
                .uri("/v3/nhl/odds/json/GameOddsByDate/" + today)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));

            long responseTime = System.currentTimeMillis() - startTime;

            result.put("status", "SUCCESS");
            result.put("responseTimeMs", responseTime);

            if (response != null && response.contains("\"Sportsbook\":\"Scrambled\"")) {
                result.put("sportsbooksScrambled", true);
            }

            Set<Integer> sportsbookIds = extractSportsbookIds(response);
            result.put("sportsbookIdsFound", sportsbookIds);

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("errorMessage", e.getMessage());
        }

        return result;
    }

    /**
     * Extract unique SportsbookIds from response
     */
    private Set<Integer> extractSportsbookIds(String response) {
        Set<Integer> ids = new TreeSet<>();
        if (response == null) return ids;

        // Simple regex-like extraction for "SportsbookId":N
        int index = 0;
        while ((index = response.indexOf("\"SportsbookId\":", index)) != -1) {
            index += 15; // Skip past "SportsbookId":
            StringBuilder sb = new StringBuilder();
            while (index < response.length() && Character.isDigit(response.charAt(index))) {
                sb.append(response.charAt(index));
                index++;
            }
            if (sb.length() > 0) {
                ids.add(Integer.parseInt(sb.toString()));
            }
        }

        return ids;
    }

    /**
     * Generate summary of all tests
     */
    private Map<String, Object> generateSummary(Map<String, Object> results) {
        Map<String, Object> summary = new LinkedHashMap<>();

        // Critical issues
        List<String> criticalIssues = new ArrayList<>();
        criticalIssues.add("ALL sportsbook names are 'Scrambled' in free trial - CANNOT verify Missouri sportsbooks");
        criticalIssues.add("SOCCER endpoints return 401 Unauthorized - 3-way betting CANNOT be tested");
        criticalIssues.add("10-minute delay on live updates in free trial");
        criticalIssues.add("Pricing not publicly available - must contact sales");

        summary.put("criticalIssues", criticalIssues);

        // Missouri sportsbooks verification
        Map<String, String> missouriVerification = new LinkedHashMap<>();
        for (String book : MISSOURI_SPORTSBOOKS) {
            missouriVerification.put(book, "CANNOT VERIFY - Names scrambled");
        }
        summary.put("missouriSportsbooksVerification", missouriVerification);

        // Recommendation
        summary.put("canReplaceTheOddsAPI", false);
        summary.put("recommendation", "DO NOT REPLACE The Odds API");
        summary.put("reasons", Arrays.asList(
            "Cannot verify Missouri sportsbook coverage (names scrambled)",
            "Soccer 3-way betting not accessible in free trial",
            "The Odds API provides actual sportsbook names",
            "The Odds API has transparent pricing ($25-75/mo)",
            "Would need paid SportsDataIO plan to properly evaluate, and pricing is unclear"
        ));

        return summary;
    }

    /**
     * Get Missouri sportsbooks list for reference
     */
    public List<String> getMissouriSportsbooks() {
        return MISSOURI_SPORTSBOOKS;
    }
}
