package com.coltwarren.sports_betting_analytics.service.odds;

import com.coltwarren.sports_betting_analytics.config.OddsApiProperties;
import com.coltwarren.sports_betting_analytics.model.odds.CachedOdds;
import com.coltwarren.sports_betting_analytics.model.odds.OddsFetchLog;
import com.coltwarren.sports_betting_analytics.model.odds.OddsResponse;
import com.coltwarren.sports_betting_analytics.repository.CachedOddsRepository;
import com.coltwarren.sports_betting_analytics.repository.OddsFetchLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class OddsSchedulerService {

    private final OddsApiProperties properties;
    private final CachedOddsRepository cachedOddsRepository;
    private final OddsFetchLogRepository oddsFetchLogRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final OddsBroadcastService oddsBroadcastService;

    // Track API credits from response headers
    private volatile int creditsRemaining = -1;
    private volatile int creditsUsed = -1;
    private volatile LocalDateTime lastFetchTime;
    private final AtomicBoolean fetchInProgress = new AtomicBoolean(false);

    public OddsSchedulerService(OddsApiProperties properties,
                                CachedOddsRepository cachedOddsRepository,
                                OddsFetchLogRepository oddsFetchLogRepository,
                                ObjectMapper objectMapper,
                                @Qualifier("oddsApiWebClient") WebClient webClient,
                                OddsBroadcastService oddsBroadcastService) {
        this.properties = properties;
        this.cachedOddsRepository = cachedOddsRepository;
        this.oddsFetchLogRepository = oddsFetchLogRepository;
        this.objectMapper = objectMapper;
        this.webClient = webClient;
        this.oddsBroadcastService = oddsBroadcastService;
    }

    /**
     * Fetch odds on startup if configured.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (properties.isEnabled() && properties.isStartupFetch()) {
            log.info("Startup odds fetch triggered for {} leagues", properties.getEnabledLeagues().size());
            fetchAllOdds();
        }
    }

    /**
     * Scheduled fetch of all enabled leagues.
     */
    @Scheduled(fixedRateString = "#{${odds-api.refresh-interval-minutes:15} * 60 * 1000}")
    public void scheduledFetch() {
        if (!properties.isEnabled()) return;
        fetchAllOdds();
    }

    /**
     * Daily summary logged at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void logDailySummary() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<OddsFetchLog> recentLogs = oddsFetchLogRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since);

        long successCount = recentLogs.stream().filter(l -> "SUCCESS".equals(l.getStatus())).count();
        long failCount = recentLogs.stream().filter(l -> "FAILED".equals(l.getStatus())).count();
        long rateLimitCount = recentLogs.stream().filter(l -> "RATE_LIMITED".equals(l.getStatus())).count();
        int totalEvents = recentLogs.stream()
            .filter(l -> "SUCCESS".equals(l.getStatus()))
            .mapToInt(l -> l.getEventsFetched() != null ? l.getEventsFetched() : 0)
            .sum();
        int totalCredits = recentLogs.stream()
            .mapToInt(l -> l.getApiCreditsUsed() != null ? l.getApiCreditsUsed() : 0)
            .sum();

        log.info("Daily odds fetch summary: {} successful, {} failed, {} rate-limited, {} events, {} API credits used",
            successCount, failCount, rateLimitCount, totalEvents, totalCredits);
    }

    /**
     * Iterate through all enabled leagues, fetching odds for each
     * with a delay between calls to respect rate limits.
     */
    public void fetchAllOdds() {
        if (!fetchInProgress.compareAndSet(false, true)) {
            log.info("Fetch already in progress, skipping");
            return;
        }

        try {
            List<String> leagues = properties.getEnabledLeagues();
            log.info("Starting odds fetch for {} leagues", leagues.size());

            int successCount = 0;
            int failCount = 0;
            int totalEvents = 0;

            for (int i = 0; i < leagues.size(); i++) {
                String league = leagues.get(i);

                try {
                    int events = fetchLeagueOddsSync(league);
                    successCount++;
                    totalEvents += events;
                } catch (Exception e) {
                    failCount++;
                    log.error("Failed to fetch odds for {}: {}", league, e.getMessage());
                }

                // Delay between leagues (skip after the last one)
                if (i < leagues.size() - 1) {
                    try {
                        Thread.sleep(properties.getDelayBetweenLeaguesMs());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Odds fetch interrupted after {}/{} leagues", i + 1, leagues.size());
                        return;
                    }
                }
            }

            log.info("Odds fetch complete: {}/{} leagues succeeded, {} total events cached, API credits remaining: {}",
                successCount, leagues.size(), totalEvents, creditsRemaining);

            if (failCount > 0) {
                log.warn("Odds fetch had {} failures out of {} leagues", failCount, leagues.size());
            }

            // Record fetch time, broadcast update, and clean up expired cache entries
            this.lastFetchTime = LocalDateTime.now();
            oddsBroadcastService.broadcastAll();
            cleanExpiredCache();
        } finally {
            fetchInProgress.set(false);
        }
    }

    /**
     * Fetch a single league asynchronously (for on-demand refreshes).
     */
    @Async("externalApiExecutor")
    public void fetchLeagueOdds(String league) {
        fetchLeagueOddsSync(league);
    }

    /**
     * Fetch odds for a single league with retry logic.
     * Stores results in cached_odds and logs to odds_fetch_log.
     * Returns number of events fetched.
     */
    @Transactional
    public int fetchLeagueOddsSync(String league) {
        long startTime = System.currentTimeMillis();
        String sport = extractSport(league);

        for (int attempt = 1; attempt <= properties.getMaxRetries(); attempt++) {
            try {
                AtomicInteger apiCredits = new AtomicInteger(0);
                List<OddsResponse> odds = callOddsApi(league, apiCredits);
                long duration = System.currentTimeMillis() - startTime;

                // Store each event in cached_odds
                int eventsStored = 0;
                for (OddsResponse event : odds) {
                    storeOddsEvent(sport, league, event);
                    eventsStored++;
                }

                // Log success
                logFetch(sport, league, "SUCCESS", eventsStored, apiCredits.get(), duration, null);
                log.info("Fetched odds for {} - {} events found, {} credits used, {}ms",
                    league, eventsStored, apiCredits.get(), duration);
                return eventsStored;

            } catch (WebClientResponseException.TooManyRequests e) {
                long duration = System.currentTimeMillis() - startTime;
                logFetch(sport, league, "RATE_LIMITED", 0, 0, duration, e.getMessage());
                log.warn("Rate limited on {} (attempt {}/{})", league, attempt, properties.getMaxRetries());

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;

                if (attempt == properties.getMaxRetries()) {
                    logFetch(sport, league, "FAILED", 0, 0, duration, e.getMessage());
                    log.error("Failed to fetch {} after {} retries: {}", league, attempt, e.getMessage());
                    return 0;
                }

                log.warn("Retry {}/{} for {}: {}", attempt, properties.getMaxRetries(), league, e.getMessage());
            }

            // Wait before retry
            try {
                Thread.sleep(properties.getRetryDelayMs());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return 0;
            }
        }
        return 0;
    }

    /**
     * Get cached odds for a league. Returns stale data if configured and fresh data unavailable.
     */
    public List<CachedOdds> getCachedOdds(String sport, String league) {
        LocalDateTime now = LocalDateTime.now();

        // Try fresh cache first
        List<CachedOdds> fresh = cachedOddsRepository.findBySportAndLeagueAndExpiresAtAfter(sport, league, now);
        if (!fresh.isEmpty()) {
            return fresh;
        }

        // Serve stale if configured
        if (properties.isServeStaleIfError()) {
            LocalDateTime staleLimit = now.minusMinutes(properties.getStaleMaxAgeMinutes());
            return cachedOddsRepository.findBySportAndLeagueAndExpiresAtAfter(sport, league, staleLimit);
        }

        return List.of();
    }

    /**
     * Get current API credit status.
     */
    public int getCreditsRemaining() {
        return creditsRemaining;
    }

    public LocalDateTime getLastFetchTime() {
        return lastFetchTime;
    }

    public LocalDateTime getNextFetchTime() {
        if (lastFetchTime == null) return null;
        return lastFetchTime.plusMinutes(properties.getRefreshIntervalMinutes());
    }

    public List<String> getEnabledLeagues() {
        return properties.getEnabledLeagues();
    }

    /**
     * Get total API credits used today from fetch logs.
     */
    public int getCreditsUsedToday() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<OddsFetchLog> todayLogs = oddsFetchLogRepository.findByCreatedAtAfterOrderByCreatedAtDesc(startOfDay);
        return todayLogs.stream()
            .mapToInt(l -> l.getApiCreditsUsed() != null ? l.getApiCreditsUsed() : 0)
            .sum();
    }

    /**
     * Call The Odds API for a specific league.
     * Reads x-requests-remaining and x-requests-used from response headers.
     */
    private List<OddsResponse> callOddsApi(String league, AtomicInteger creditsOut) {
        String url = String.format("/sports/%s/odds?apiKey=%s&regions=us&markets=h2h,spreads,totals&oddsFormat=american",
            league, properties.getApiKey());

        ResponseEntity<List<OddsResponse>> response = webClient.get()
            .uri(url)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<List<OddsResponse>>() {})
            .block(Duration.ofSeconds(15));

        if (response != null) {
            // Track API credits from response headers
            String remaining = response.getHeaders().getFirst("x-requests-remaining");
            String used = response.getHeaders().getFirst("x-requests-used");

            if (remaining != null) {
                this.creditsRemaining = Integer.parseInt(remaining);
            }
            if (used != null) {
                this.creditsUsed = Integer.parseInt(used);
                creditsOut.set(1); // Each call uses 1 credit
            }

            return response.getBody() != null ? response.getBody() : List.of();
        }

        return List.of();
    }

    /**
     * Store or update a single odds event in the cache.
     */
    private void storeOddsEvent(String sport, String league, OddsResponse event) {
        Optional<CachedOdds> existing = cachedOddsRepository
            .findBySportAndLeagueAndEventId(sport, league, event.getId());

        CachedOdds cached = existing.orElseGet(CachedOdds::new);
        cached.setSport(sport);
        cached.setLeague(league);
        cached.setEventId(event.getId());
        cached.setHomeTeam(event.getHome_team());
        cached.setAwayTeam(event.getAway_team());
        cached.setCommenceTime(parseCommenceTime(event.getCommence_time()));
        cached.setBookmakersCount(event.getBookmakers() != null ? event.getBookmakers().size() : 0);
        cached.setFetchedAt(LocalDateTime.now());
        cached.setExpiresAt(LocalDateTime.now().plusMinutes(properties.getCacheTtlMinutes()));

        try {
            cached.setOddsData(objectMapper.writeValueAsString(event.getBookmakers()));
        } catch (JsonProcessingException e) {
            cached.setOddsData("[]");
            log.warn("Failed to serialize odds data for event {}", event.getId());
        }

        cachedOddsRepository.save(cached);
    }

    /**
     * Log a fetch attempt to odds_fetch_log.
     */
    private void logFetch(String sport, String league, String status, int eventsFetched,
                          int apiCreditsUsed, long durationMs, String error) {
        OddsFetchLog fetchLog = new OddsFetchLog();
        fetchLog.setSport(sport);
        fetchLog.setLeague(league);
        fetchLog.setStatus(status);
        fetchLog.setEventsFetched(eventsFetched);
        fetchLog.setApiCreditsUsed(apiCreditsUsed);
        fetchLog.setDurationMs((int) durationMs);
        fetchLog.setErrorMessage(error);
        oddsFetchLogRepository.save(fetchLog);
    }

    /**
     * Remove expired cache entries.
     */
    @Transactional
    public void cleanExpiredCache() {
        LocalDateTime staleLimit = LocalDateTime.now().minusMinutes(properties.getStaleMaxAgeMinutes());
        cachedOddsRepository.deleteByExpiresAtBefore(staleLimit);
    }

    /**
     * Extract sport category from league key (e.g., "americanfootball_nfl" -> "americanfootball").
     */
    private String extractSport(String league) {
        int idx = league.lastIndexOf('_');
        return idx > 0 ? league.substring(0, idx) : league;
    }

    /**
     * Parse ISO commence time string to LocalDateTime.
     */
    private LocalDateTime parseCommenceTime(String commenceTime) {
        if (commenceTime == null) return null;
        try {
            return LocalDateTime.parse(commenceTime.replace("Z", ""));
        } catch (Exception e) {
            return null;
        }
    }
}
