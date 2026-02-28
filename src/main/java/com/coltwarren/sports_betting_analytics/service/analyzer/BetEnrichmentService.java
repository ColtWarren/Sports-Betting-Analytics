package com.coltwarren.sports_betting_analytics.service.analyzer;

import com.coltwarren.sports_betting_analytics.model.betting.ContrarianValue;
import com.coltwarren.sports_betting_analytics.model.betting.PublicBettingData;
import com.coltwarren.sports_betting_analytics.model.injury.InjuryImpact;
import com.coltwarren.sports_betting_analytics.model.weather.WeatherImpact;
import com.coltwarren.sports_betting_analytics.service.betting.PublicBettingService;
import com.coltwarren.sports_betting_analytics.service.injury.InjuryAnalysisService;
import com.coltwarren.sports_betting_analytics.service.weather.WeatherAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Enriches bet recommendations with weather, public betting, and injury data.
 */
@Service
@Slf4j
public class BetEnrichmentService {

    private final WeatherAnalysisService weatherAnalysisService;
    private final PublicBettingService publicBettingService;
    private final InjuryAnalysisService injuryAnalysisService;

    public BetEnrichmentService(WeatherAnalysisService weatherAnalysisService,
                                 PublicBettingService publicBettingService,
                                 InjuryAnalysisService injuryAnalysisService) {
        this.weatherAnalysisService = weatherAnalysisService;
        this.publicBettingService = publicBettingService;
        this.injuryAnalysisService = injuryAnalysisService;
    }

    /**
     * Gather injury context string for AI prompt building.
     */
    public InjuryContext gatherInjuryContext(String sport, String homeTeam, String awayTeam) {
        try {
            Map<String, InjuryImpact> matchupInjuries = injuryAnalysisService.analyzeMatchupInjuries(
                sport, homeTeam, null, awayTeam, null);
            InjuryImpact homeInj = matchupInjuries.get("home");
            InjuryImpact awayInj = matchupInjuries.get("away");

            StringBuilder injCtx = new StringBuilder();
            if (Boolean.TRUE.equals(homeInj.getHasSignificantInjuries())) {
                injCtx.append(String.format("%s INJURIES: %s (%.1f pts spread impact)\n",
                    homeTeam, homeInj.getSummary(), homeInj.getSpreadImpact()));
            }
            if (Boolean.TRUE.equals(awayInj.getHasSignificantInjuries())) {
                injCtx.append(String.format("%s INJURIES: %s (%.1f pts spread impact)\n",
                    awayTeam, awayInj.getSummary(), awayInj.getSpreadImpact()));
            }
            return new InjuryContext(injCtx.toString(), matchupInjuries);

        } catch (Exception e) {
            log.error("Pre-analysis injury fetch error: {}", e.getMessage());
            return new InjuryContext("", null);
        }
    }

    /**
     * Add weather impact data to a bet (outdoor sports only).
     */
    public void enrichWithWeather(Map<String, Object> bet, Map<String, Object> game,
                                   String homeTeam, String sport) {
        try {
            LocalDateTime gameTime = BetAnalysisUtils.parseGameTime(game.get("gameTime"));
            WeatherImpact weatherImpact = weatherAnalysisService.analyzeGameWeather(
                homeTeam, sport, gameTime != null ? gameTime : LocalDateTime.now().plusHours(3));

            if (weatherImpact != null) {
                bet.put("weatherImpact", weatherImpact);
                bet.put("hasWeatherImpact", weatherImpact.getSignificantImpact());

                if (Boolean.TRUE.equals(weatherImpact.getSignificantImpact())) {
                    String weatherNote = "\n\n☁️ WEATHER ALERT: " + weatherImpact.getReason();
                    if (weatherImpact.getScoringImpact() != null) {
                        weatherNote += String.format(" | Expected: %+.1f pts", weatherImpact.getScoringImpact());
                    }
                    weatherNote += " | Suggests: " + weatherImpact.getRecommendation();
                    bet.put("analysis", bet.get("analysis") + weatherNote);

                    if (weatherImpact.getConfidence() != null && weatherImpact.getConfidence() >= 7.0) {
                        double confidence = Math.min(10.0, (Double) bet.get("confidence") + 0.5);
                        bet.put("confidence", confidence);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Weather analysis error: {}", e.getMessage());
        }
    }

    /**
     * Add public betting / contrarian data to a bet.
     */
    @SuppressWarnings("unchecked")
    public void enrichWithPublicBetting(Map<String, Object> bet, Map<String, Object> gameOdds,
                                         String sport, String homeTeam, String awayTeam) {
        try {
            boolean isHomeFavorite = determineHomeFavorite(gameOdds);
            String gameId = sport + "_" + homeTeam + "_" + awayTeam;

            PublicBettingData publicData = publicBettingService.getPublicBettingData(
                gameId, sport, homeTeam, awayTeam, isHomeFavorite);
            ContrarianValue spreadContrarian = publicBettingService.analyzeContrarianValue(
                publicData, homeTeam, awayTeam);
            ContrarianValue totalsContrarian = publicBettingService.analyzeTotalsContrarian(publicData);

            bet.put("publicBettingData", publicData);
            bet.put("spreadContrarian", spreadContrarian);
            bet.put("totalsContrarian", totalsContrarian);
            bet.put("hasContrarianValue",
                Boolean.TRUE.equals(spreadContrarian.getHasContrarianValue()) ||
                Boolean.TRUE.equals(totalsContrarian.getHasContrarianValue()));

            if (Boolean.TRUE.equals(spreadContrarian.getHasContrarianValue())) {
                String currentAnalysis = (String) bet.get("analysis");
                bet.put("analysis", currentAnalysis + "\n\n📊 PUBLIC FADE: " + spreadContrarian.getReason());

                double confidence = (Double) bet.get("confidence");
                if ("EXTREME".equals(spreadContrarian.getAlertLevel())) {
                    bet.put("confidence", Math.min(10.0, confidence + 1.0));
                } else if ("STRONG".equals(spreadContrarian.getAlertLevel())) {
                    bet.put("confidence", Math.min(10.0, confidence + 0.5));
                }
            }

        } catch (Exception e) {
            log.error("Public betting analysis error: {}", e.getMessage());
        }
    }

    /**
     * Add injury metadata to a bet.
     */
    public void enrichWithInjuries(Map<String, Object> bet, Map<String, InjuryImpact> matchupInjuries) {
        if (matchupInjuries == null) return;

        try {
            InjuryImpact homeInjuryImpact = matchupInjuries.get("home");
            InjuryImpact awayInjuryImpact = matchupInjuries.get("away");

            bet.put("homeInjuryImpact", homeInjuryImpact);
            bet.put("awayInjuryImpact", awayInjuryImpact);
            bet.put("netInjuryAdvantage", injuryAnalysisService.getNetInjuryAdvantage(matchupInjuries));
            bet.put("injurySeverity", injuryAnalysisService.getWorstSeverity(matchupInjuries));
            bet.put("hasInjuryImpact",
                Boolean.TRUE.equals(homeInjuryImpact.getHasSignificantInjuries()) ||
                Boolean.TRUE.equals(awayInjuryImpact.getHasSignificantInjuries()));
        } catch (Exception e) {
            log.error("Injury metadata error: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private boolean determineHomeFavorite(Map<String, Object> gameOdds) {
        try {
            Map<String, Object> bestOdds = (Map<String, Object>) gameOdds.get("bestOdds");
            if (bestOdds == null) return true;

            if (bestOdds.containsKey("homeSpread")) {
                Object spreadObj = bestOdds.get("homeSpread");
                if (spreadObj instanceof Number) {
                    return ((Number) spreadObj).doubleValue() < 0;
                }
            }

            if (bestOdds.containsKey("homeML") && bestOdds.containsKey("awayML")) {
                Object homeML = bestOdds.get("homeML");
                Object awayML = bestOdds.get("awayML");
                if (homeML instanceof Number && awayML instanceof Number) {
                    return ((Number) homeML).intValue() < ((Number) awayML).intValue();
                }
            }

            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Holds pre-fetched injury context for AI prompt and raw data for metadata.
     */
    public record InjuryContext(String promptText, Map<String, InjuryImpact> matchupInjuries) {}
}
