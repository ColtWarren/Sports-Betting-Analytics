package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.UserContextService;
import com.coltwarren.sports_betting_analytics.service.odds.OddsSchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/odds")
@Slf4j
public class OddsAdminController {

    private final OddsSchedulerService oddsSchedulerService;
    private final UserContextService userContextService;

    public OddsAdminController(OddsSchedulerService oddsSchedulerService,
                               UserContextService userContextService) {
        this.oddsSchedulerService = oddsSchedulerService;
        this.userContextService = userContextService;
    }

    /**
     * GET /api/admin/odds/status - Cache status and API credit info.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        if (!userContextService.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        LocalDateTime lastFetch = oddsSchedulerService.getLastFetchTime();
        LocalDateTime nextFetch = oddsSchedulerService.getNextFetchTime();
        int creditsRemaining = oddsSchedulerService.getCreditsRemaining();
        int creditsUsedToday = oddsSchedulerService.getCreditsUsedToday();

        String cacheHealth;
        if (lastFetch == null) {
            cacheHealth = "NO_DATA";
        } else if (lastFetch.isAfter(LocalDateTime.now().minusMinutes(30))) {
            cacheHealth = "HEALTHY";
        } else {
            cacheHealth = "STALE";
        }

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("leagues", oddsSchedulerService.getEnabledLeagues());
        status.put("lastFetch", lastFetch);
        status.put("nextFetch", nextFetch);
        status.put("cacheHealth", cacheHealth);
        status.put("creditsUsedToday", creditsUsedToday);
        status.put("creditsRemaining", creditsRemaining);

        return ResponseEntity.ok(status);
    }

    /**
     * POST /api/admin/odds/refresh - Manual refresh of all leagues.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshAll() {
        if (!userContextService.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        log.info("Admin triggered manual odds refresh for all leagues");
        oddsSchedulerService.fetchAllOdds();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "completed");
        result.put("leagues", oddsSchedulerService.getEnabledLeagues().size());
        result.put("creditsRemaining", oddsSchedulerService.getCreditsRemaining());

        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/admin/odds/refresh/{league} - Force refresh a single league.
     */
    @PostMapping("/refresh/{league}")
    public ResponseEntity<Map<String, Object>> refreshLeague(@PathVariable String league) {
        if (!userContextService.isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        if (!oddsSchedulerService.getEnabledLeagues().contains(league)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Unknown league",
                "league", league,
                "availableLeagues", oddsSchedulerService.getEnabledLeagues()
            ));
        }

        log.info("Admin triggered manual odds refresh for league: {}", league);
        int events = oddsSchedulerService.fetchLeagueOddsSync(league);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "completed");
        result.put("league", league);
        result.put("eventsFetched", events);
        result.put("creditsRemaining", oddsSchedulerService.getCreditsRemaining());

        return ResponseEntity.ok(result);
    }
}
