package com.coltwarren.sports_betting_analytics.service.odds;

import com.coltwarren.sports_betting_analytics.config.OddsApiProperties;
import com.coltwarren.sports_betting_analytics.model.odds.OddsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class OddsBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final CachedOddsService cachedOddsService;
    private final OddsApiProperties oddsApiProperties;

    public OddsBroadcastService(SimpMessagingTemplate messagingTemplate,
                                CachedOddsService cachedOddsService,
                                OddsApiProperties oddsApiProperties) {
        this.messagingTemplate = messagingTemplate;
        this.cachedOddsService = cachedOddsService;
        this.oddsApiProperties = oddsApiProperties;
    }

    /**
     * Broadcast odds update summaries across all enabled leagues.
     * Called once after all leagues have been fetched for a consistent snapshot.
     */
    public void broadcastAll() {
        try {
            broadcastOddsUpdate();
        } catch (Exception e) {
            log.error("Failed to broadcast odds update: {}", e.getMessage(), e);
        }
    }

    /**
     * Build lightweight event summaries from cached odds and send to /topic/odds.
     * Does NOT include full bookmaker data — clients pull detail via REST.
     */
    private void broadcastOddsUpdate() {
        List<String> leagues = oddsApiProperties.getEnabledLeagues();
        List<Map<String, Object>> leagueSummaries = new ArrayList<>();
        int totalEvents = 0;

        for (String league : leagues) {
            List<OddsResponse> odds = cachedOddsService.getLiveOdds(league);
            if (odds.isEmpty()) {
                continue;
            }

            List<Map<String, Object>> eventSummaries = new ArrayList<>();
            for (OddsResponse event : odds) {
                Map<String, Object> eventSummary = new LinkedHashMap<>();
                eventSummary.put("eventId", event.getId());
                eventSummary.put("sportKey", event.getSport_key());
                eventSummary.put("homeTeam", event.getHome_team());
                eventSummary.put("awayTeam", event.getAway_team());
                eventSummary.put("commenceTime", event.getCommence_time());
                eventSummary.put("bookmakersCount",
                        event.getBookmakers() != null ? event.getBookmakers().size() : 0);
                eventSummaries.add(eventSummary);
            }

            Map<String, Object> leagueSummary = new LinkedHashMap<>();
            leagueSummary.put("league", league);
            leagueSummary.put("eventCount", eventSummaries.size());
            leagueSummary.put("events", eventSummaries);
            leagueSummaries.add(leagueSummary);

            totalEvents += eventSummaries.size();
        }

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "ODDS_UPDATE");
        message.put("timestamp", LocalDateTime.now().toString());
        message.put("leagueCount", leagueSummaries.size());
        message.put("totalEvents", totalEvents);
        message.put("leagues", leagueSummaries);

        messagingTemplate.convertAndSend("/topic/odds", (Object) message);
        log.info("Broadcast odds update: {} leagues, {} events", leagueSummaries.size(), totalEvents);
    }
}
