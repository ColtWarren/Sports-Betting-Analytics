package com.coltwarren.sports_betting_analytics.service.odds;

import com.coltwarren.sports_betting_analytics.config.OddsApiProperties;
import com.coltwarren.sports_betting_analytics.model.odds.CachedOdds;
import com.coltwarren.sports_betting_analytics.model.odds.OddsResponse;
import com.coltwarren.sports_betting_analytics.repository.CachedOddsRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class OddsService {

    private final CachedOddsRepository cachedOddsRepository;
    private final OddsApiProperties properties;
    private final ObjectMapper objectMapper;

    public OddsService(CachedOddsRepository cachedOddsRepository,
                       OddsApiProperties properties,
                       ObjectMapper objectMapper) {
        this.cachedOddsRepository = cachedOddsRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Get live odds from cache (no direct API call).
     * Returns cached data, falling back to stale data if configured.
     */
    public List<OddsResponse> getLiveOdds(String sportKey) {
        LocalDateTime now = LocalDateTime.now();
        String sport = extractSport(sportKey);

        // Try fresh cache first
        List<CachedOdds> cached = cachedOddsRepository.findBySportAndLeagueAndExpiresAtAfter(sport, sportKey, now);

        // Fall back to stale if configured and no fresh data
        if (cached.isEmpty() && properties.isServeStaleIfError()) {
            LocalDateTime staleLimit = now.minusMinutes(properties.getStaleMaxAgeMinutes());
            cached = cachedOddsRepository.findBySportAndLeagueAndExpiresAtAfter(sport, sportKey, staleLimit);
            if (!cached.isEmpty()) {
                log.debug("Serving stale odds for {}", sportKey);
            }
        }

        if (cached.isEmpty()) {
            return List.of();
        }

        // Convert cached entries back to OddsResponse objects
        List<OddsResponse> responses = new ArrayList<>();
        for (CachedOdds entry : cached) {
            OddsResponse response = toOddsResponse(entry);
            if (response != null) {
                responses.add(response);
            }
        }

        return responses;
    }

    /**
     * Check if cache is stale for a given sport key.
     */
    public boolean isCacheStale(String sportKey) {
        String sport = extractSport(sportKey);
        List<CachedOdds> fresh = cachedOddsRepository.findBySportAndLeagueAndExpiresAtAfter(
            sport, sportKey, LocalDateTime.now());
        return fresh.isEmpty();
    }

    /**
     * Find best odds across sportsbooks for a team in a specific market.
     */
    public Map<String, Object> findBestOdds(String sportKey, String teamName, String marketType) {
        List<OddsResponse> allGames = getLiveOdds(sportKey);

        Map<String, Object> result = new HashMap<>();
        result.put("found", false);

        for (OddsResponse game : allGames) {
            if (game.getHome_team().equalsIgnoreCase(teamName) ||
                game.getAway_team().equalsIgnoreCase(teamName)) {

                result.put("found", true);
                result.put("game", String.format("%s vs %s", game.getAway_team(), game.getHome_team()));
                result.put("commence_time", game.getCommence_time());

                Map<String, Map<String, Object>> bookmakerOdds = new HashMap<>();

                for (OddsResponse.Bookmaker bookmaker : game.getBookmakers()) {
                    for (OddsResponse.Market market : bookmaker.getMarkets()) {
                        if (market.getKey().equals(marketType)) {
                            for (OddsResponse.Outcome outcome : market.getOutcomes()) {
                                if (outcome.getName().equalsIgnoreCase(teamName)) {
                                    Map<String, Object> oddsData = new HashMap<>();
                                    oddsData.put("price", outcome.getPrice());
                                    if (outcome.getPoint() != null) {
                                        oddsData.put("point", outcome.getPoint());
                                    }
                                    bookmakerOdds.put(bookmaker.getTitle(), oddsData);
                                }
                            }
                        }
                    }
                }

                result.put("bookmakers", bookmakerOdds);

                if (!bookmakerOdds.isEmpty()) {
                    String bestBook = null;
                    Double bestOdds = Double.NEGATIVE_INFINITY;

                    for (Map.Entry<String, Map<String, Object>> entry : bookmakerOdds.entrySet()) {
                        Double price = (Double) entry.getValue().get("price");
                        if (price > bestOdds) {
                            bestOdds = price;
                            bestBook = entry.getKey();
                        }
                    }

                    result.put("bestBook", bestBook);
                    result.put("bestOdds", bestOdds);
                }

                break;
            }
        }

        return result;
    }

    public String getSportKey(String sport) {
        return switch (sport.toUpperCase()) {
            case "NFL" -> "americanfootball_nfl";
            case "NBA" -> "basketball_nba";
            case "MLB" -> "baseball_mlb";
            case "NHL" -> "icehockey_nhl";
            case "NCAAF" -> "americanfootball_ncaaf";
            case "NCAAB" -> "basketball_ncaab";
            default -> "americanfootball_nfl";
        };
    }

    public String getMarketKey(String betType) {
        return switch (betType.toUpperCase()) {
            case "MONEYLINE" -> "h2h";
            case "SPREAD" -> "spreads";
            case "TOTAL_OVER", "TOTAL_UNDER" -> "totals";
            default -> "h2h";
        };
    }

    /**
     * Convert a CachedOdds entry back to an OddsResponse.
     */
    private OddsResponse toOddsResponse(CachedOdds cached) {
        try {
            OddsResponse response = new OddsResponse();
            response.setId(cached.getEventId());
            response.setSport_key(cached.getLeague());
            response.setHome_team(cached.getHomeTeam());
            response.setAway_team(cached.getAwayTeam());
            response.setCommence_time(
                cached.getCommenceTime() != null ? cached.getCommenceTime().toString() : null);

            List<OddsResponse.Bookmaker> bookmakers = objectMapper.readValue(
                cached.getOddsData(), new TypeReference<List<OddsResponse.Bookmaker>>() {});
            response.setBookmakers(bookmakers);

            return response;
        } catch (Exception e) {
            log.warn("Failed to deserialize cached odds for event {}: {}", cached.getEventId(), e.getMessage());
            return null;
        }
    }

    private String extractSport(String league) {
        int idx = league.lastIndexOf('_');
        return idx > 0 ? league.substring(0, idx) : league;
    }
}
