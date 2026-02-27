package com.coltwarren.sports_betting_analytics.service.weather;

import com.coltwarren.sports_betting_analytics.model.StadiumLocation;
import com.coltwarren.sports_betting_analytics.model.weather.WeatherData;
import com.coltwarren.sports_betting_analytics.model.weather.WeatherImpact;
import com.coltwarren.sports_betting_analytics.repository.StadiumLocationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Weather Analysis Service
 *
 * Analyzes weather conditions and determines their impact on sporting events.
 * Used to adjust betting recommendations based on weather factors.
 *
 * Key Weather Factors by Sport:
 *
 * NFL:
 * - Wind > 15 mph: Reduces passing, favors run game, UNDER
 * - Wind > 20 mph: Severely impacts passing & kicking, strong UNDER
 * - Temp < 20°F: Cold affects ball grip, reduces scoring, UNDER
 * - Rain/Snow: Favors run game, increases turnovers, UNDER
 *
 * MLB:
 * - Wind blowing out: Increases home runs, OVER
 * - Wind blowing in: Suppresses offense, UNDER
 * - Cold: Ball doesn't carry as far, UNDER
 * - Rain: Game may be delayed/shortened
 *
 * MLS:
 * - Extreme heat: Affects player stamina
 * - Heavy rain: Affects ball movement, passing accuracy
 * - Cold: Generally less impact than other sports
 */
@Service
@Slf4j
public class WeatherAnalysisService {

    private final StadiumLocationRepository stadiumRepository;

    private final OpenWeatherMapService weatherService;

    public WeatherAnalysisService(StadiumLocationRepository stadiumRepository,
                                  OpenWeatherMapService weatherService) {
        this.stadiumRepository = stadiumRepository;
        this.weatherService = weatherService;
    }

    /**
     * Analyze weather impact for a game
     *
     * @param teamName Home team name (used to find stadium)
     * @param sport Sport code (NFL, MLB, MLS)
     * @param gameTime Game start time
     * @return WeatherImpact analysis
     */
    public WeatherImpact analyzeGameWeather(String teamName, String sport, LocalDateTime gameTime) {
        // Find stadium by team name - use list to avoid NonUniqueResultException
        List<StadiumLocation> matches = stadiumRepository.findAll().stream()
            .filter(s -> s.getHomeTeams() != null &&
                        s.getHomeTeams().toLowerCase().contains(teamName.toLowerCase()))
            .toList();

        if (matches.isEmpty()) {
            log.debug("Stadium not found for team: {}", teamName);
            return WeatherImpact.noImpact();
        }

        return analyzeGameWeatherByStadium(matches.get(0), sport, gameTime);
    }

    /**
     * Analyze weather impact for a specific stadium
     */
    public WeatherImpact analyzeGameWeatherByStadium(StadiumLocation stadium, String sport, LocalDateTime gameTime) {
        // Skip if indoor stadium
        if (stadium.getIsDome()) {
            log.debug("Game at {} is in a dome - no weather impact", stadium.getStadiumName());
            WeatherImpact impact = WeatherImpact.noImpact();
            impact.setStadium(stadium.getStadiumName());
            impact.setReason("Indoor stadium - weather has no impact");
            return impact;
        }

        // Get weather forecast
        WeatherData weather = weatherService.getWeatherForecast(
            stadium.getLatitude(),
            stadium.getLongitude(),
            gameTime,
            stadium.getStadiumName()
        );

        if (weather == null) {
            log.warn("Could not fetch weather for {}", stadium.getStadiumName());
            return WeatherImpact.noImpact();
        }

        // Analyze based on sport
        WeatherImpact impact;
        switch (sport.toUpperCase()) {
            case "NFL":
                impact = analyzeNFLWeather(weather, stadium);
                break;
            case "MLB":
                impact = analyzeMLBWeather(weather, stadium);
                break;
            case "MLS":
            case "SOCCER":
                impact = analyzeSoccerWeather(weather, stadium);
                break;
            default:
                impact = analyzeGenericWeather(weather, stadium);
        }

        impact.setWeatherData(weather);
        impact.setStadium(stadium.getStadiumName());

        if (impact.getSignificantImpact()) {
            log.info("⚠️ Weather Alert for {} at {}: {}",
                     sport, stadium.getStadiumName(), impact.getReason());
        }

        return impact;
    }

    /**
     * Analyze NFL-specific weather impact
     */
    private WeatherImpact analyzeNFLWeather(WeatherData weather, StadiumLocation stadium) {
        WeatherImpact impact = new WeatherImpact();
        impact.setSignificantImpact(false);
        impact.setAlertLevel("NONE");

        Double windSpeed = weather.getWindSpeed();
        Double temp = weather.getTemperature();
        Double precip = weather.getPrecipitation();
        String conditions = weather.getConditions();

        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 1: EXTREME WIND (Most impactful for NFL)
        // ═══════════════════════════════════════════════════════════════
        if (windSpeed != null && windSpeed >= 20) {
            impact.setRecommendation("UNDER");
            impact.setConfidence(9.0);
            impact.setScoringImpact(-10.5);
            impact.setSpreadImpact(0.0); // Wind affects both teams equally
            impact.setReason(String.format("Extreme wind (%.0f mph) severely impacts passing game and field goals", windSpeed));
            impact.setSportNotes("Historical data shows 10+ fewer points in 20+ mph wind games. Expect run-heavy offenses.");
            impact.setSignificantImpact(true);
            impact.setAlertLevel("CRITICAL");
            return impact;
        }

        if (windSpeed != null && windSpeed >= 15) {
            impact.setRecommendation("UNDER");
            impact.setConfidence(7.5);
            impact.setScoringImpact(-7.0);
            impact.setReason(String.format("High wind (%.0f mph) reduces passing efficiency and field goal accuracy", windSpeed));
            impact.setSportNotes("Passing yards typically down 15-20% in 15-20 mph winds.");
            impact.setSignificantImpact(true);
            impact.setAlertLevel("HIGH");
            return impact;
        }

        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 2: SNOW (Rare but high impact)
        // ═══════════════════════════════════════════════════════════════
        if (conditions != null && conditions.toLowerCase().contains("snow")) {
            impact.setRecommendation("UNDER");
            impact.setConfidence(8.5);
            impact.setScoringImpact(-9.0);
            impact.setReason("Snow expected - major impact on footing, ball handling, and visibility");
            impact.setSportNotes("Snow games average 8-10 fewer points. Turnovers increase significantly.");
            impact.setSignificantImpact(true);
            impact.setAlertLevel("CRITICAL");
            return impact;
        }

        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 3: EXTREME COLD
        // ═══════════════════════════════════════════════════════════════
        if (temp != null && temp < 10) {
            impact.setRecommendation("UNDER");
            impact.setConfidence(8.0);
            impact.setScoringImpact(-9.0);
            impact.setReason(String.format("Extreme cold (%.0f°F) significantly reduces offensive efficiency", temp));
            impact.setSportNotes("Sub-10°F games average 9 fewer points. Ball becomes harder, grip suffers.");
            impact.setSignificantImpact(true);
            impact.setAlertLevel("CRITICAL");
            return impact;
        }

        if (temp != null && temp < 20) {
            impact.setRecommendation("UNDER");
            impact.setConfidence(6.5);
            impact.setScoringImpact(-5.5);
            impact.setReason(String.format("Cold weather (%.0f°F) favors defensive play", temp));
            impact.setSportNotes("Cold affects passing accuracy and kicking distance.");
            impact.setSignificantImpact(true);
            impact.setAlertLevel("HIGH");
            return impact;
        }

        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 4: RAIN
        // ═══════════════════════════════════════════════════════════════
        if (precip != null && precip > 0.3) {
            impact.setRecommendation("UNDER");
            impact.setConfidence(7.0);
            impact.setScoringImpact(-6.5);
            impact.setReason(String.format("Heavy rain expected (%.2f in) - wet conditions favor run game", precip));
            impact.setSportNotes("Rain games see increased rushing attempts and fumbles.");
            impact.setSignificantImpact(true);
            impact.setAlertLevel("HIGH");
            return impact;
        }

        if (precip != null && precip > 0.1) {
            impact.setRecommendation("UNDER");
            impact.setConfidence(5.5);
            impact.setScoringImpact(-3.0);
            impact.setReason("Light rain expected - minor impact on passing and footing");
            impact.setSignificantImpact(true);
            impact.setAlertLevel("MEDIUM");
            return impact;
        }

        // ═══════════════════════════════════════════════════════════════
        // MODERATE FACTORS
        // ═══════════════════════════════════════════════════════════════
        if (temp != null && temp < 35) {
            impact.setRecommendation("LEAN_UNDER");
            impact.setConfidence(5.5);
            impact.setScoringImpact(-2.5);
            impact.setReason(String.format("Cool weather (%.0f°F) may slightly favor defense", temp));
            impact.setSignificantImpact(false);
            impact.setAlertLevel("LOW");
            return impact;
        }

        if (windSpeed != null && windSpeed >= 10) {
            impact.setRecommendation("SLIGHT_UNDER");
            impact.setConfidence(5.0);
            impact.setScoringImpact(-1.5);
            impact.setReason(String.format("Moderate wind (%.0f mph) - minor impact on long passes", windSpeed));
            impact.setSignificantImpact(false);
            impact.setAlertLevel("LOW");
            return impact;
        }

        // No significant weather impact
        impact.setRecommendation("NO_IMPACT");
        impact.setConfidence(5.0);
        impact.setScoringImpact(0.0);
        impact.setReason("Weather conditions within normal parameters");
        return impact;
    }

    /**
     * Analyze MLB-specific weather impact
     */
    private WeatherImpact analyzeMLBWeather(WeatherData weather, StadiumLocation stadium) {
        WeatherImpact impact = new WeatherImpact();
        impact.setSignificantImpact(false);
        impact.setAlertLevel("NONE");

        Double windSpeed = weather.getWindSpeed();
        String windDir = weather.getWindDirection();
        Double temp = weather.getTemperature();
        String stadiumName = stadium.getStadiumName();

        // Determine wind direction relative to typical outfield orientation
        boolean windBlowingOut = isWindBlowingOut(windDir);

        // ═══════════════════════════════════════════════════════════════
        // WRIGLEY FIELD SPECIAL CASE (Wind famous here)
        // ═══════════════════════════════════════════════════════════════
        boolean isWrigley = stadiumName.contains("Wrigley");

        if (isWrigley && windSpeed != null && windSpeed >= 15) {
            if (windBlowingOut) {
                impact.setRecommendation("OVER");
                impact.setConfidence(9.0);
                impact.setScoringImpact(4.5);
                impact.setReason(String.format("Wind blowing out at Wrigley (%.0f mph) - expect fireworks", windSpeed));
                impact.setSportNotes("Wrigley with wind out is one of the best OVER spots in baseball.");
                impact.setSignificantImpact(true);
                impact.setAlertLevel("CRITICAL");
                return impact;
            } else {
                impact.setRecommendation("UNDER");
                impact.setConfidence(8.0);
                impact.setScoringImpact(-3.5);
                impact.setReason(String.format("Wind blowing in at Wrigley (%.0f mph) - suppresses home runs", windSpeed));
                impact.setSportNotes("Wrigley with wind in becomes a pitcher's park.");
                impact.setSignificantImpact(true);
                impact.setAlertLevel("HIGH");
                return impact;
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // COORS FIELD SPECIAL CASE (Altitude + Heat)
        // ═══════════════════════════════════════════════════════════════
        boolean isCoors = stadiumName.contains("Coors");

        if (isCoors && temp != null && temp >= 85) {
            impact.setRecommendation("OVER");
            impact.setConfidence(8.5);
            impact.setScoringImpact(5.0);
            impact.setReason(String.format("Hot weather (%.0f°F) at Coors Field - ball carries even more", temp));
            impact.setSportNotes("Thin air + heat = offensive explosion. OVER hits at high rate.");
            impact.setSignificantImpact(true);
            impact.setAlertLevel("HIGH");
            return impact;
        }

        // ═══════════════════════════════════════════════════════════════
        // GENERAL WIND IMPACT
        // ═══════════════════════════════════════════════════════════════
        if (windSpeed != null && windSpeed >= 15) {
            if (windBlowingOut) {
                impact.setRecommendation("OVER");
                impact.setConfidence(7.0);
                impact.setScoringImpact(3.0);
                impact.setReason(String.format("Wind blowing out (%.0f mph) increases home run probability", windSpeed));
                impact.setSignificantImpact(true);
                impact.setAlertLevel("MEDIUM");
            } else {
                impact.setRecommendation("UNDER");
                impact.setConfidence(6.5);
                impact.setScoringImpact(-2.5);
                impact.setReason(String.format("Wind blowing in (%.0f mph) suppresses offense", windSpeed));
                impact.setSignificantImpact(true);
                impact.setAlertLevel("MEDIUM");
            }
            return impact;
        }

        // ═══════════════════════════════════════════════════════════════
        // TEMPERATURE IMPACT
        // ═══════════════════════════════════════════════════════════════
        if (temp != null && temp < 50) {
            impact.setRecommendation("UNDER");
            impact.setConfidence(6.0);
            impact.setScoringImpact(-1.5);
            impact.setReason(String.format("Cold weather (%.0f°F) - ball doesn't carry as far", temp));
            impact.setSignificantImpact(true);
            impact.setAlertLevel("LOW");
            return impact;
        }

        if (temp != null && temp >= 90) {
            impact.setRecommendation("OVER");
            impact.setConfidence(6.0);
            impact.setScoringImpact(1.5);
            impact.setReason(String.format("Hot weather (%.0f°F) - ball carries better, pitchers tire faster", temp));
            impact.setSignificantImpact(true);
            impact.setAlertLevel("LOW");
            return impact;
        }

        // No significant impact
        impact.setRecommendation("NO_IMPACT");
        impact.setConfidence(5.0);
        impact.setScoringImpact(0.0);
        impact.setReason("Weather conditions within normal parameters");
        return impact;
    }

    /**
     * Analyze Soccer-specific weather impact
     */
    private WeatherImpact analyzeSoccerWeather(WeatherData weather, StadiumLocation stadium) {
        WeatherImpact impact = new WeatherImpact();
        impact.setSignificantImpact(false);
        impact.setAlertLevel("NONE");

        Double temp = weather.getTemperature();
        Double precip = weather.getPrecipitation();

        // Extreme heat (important for outdoor MLS games)
        if (temp != null && temp >= 95) {
            impact.setRecommendation("UNDER");
            impact.setConfidence(7.0);
            impact.setScoringImpact(-0.5);
            impact.setReason(String.format("Extreme heat (%.0f°F) affects player stamina", temp));
            impact.setSportNotes("Expect more substitutions and slower pace of play.");
            impact.setSignificantImpact(true);
            impact.setAlertLevel("MEDIUM");
            return impact;
        }

        // Heavy rain
        if (precip != null && precip > 0.3) {
            impact.setRecommendation("UNDER");
            impact.setConfidence(6.5);
            impact.setScoringImpact(-0.5);
            impact.setReason(String.format("Heavy rain (%.2f in) affects ball control and passing", precip));
            impact.setSportNotes("Wet pitch leads to more long balls, fewer intricate passes.");
            impact.setSignificantImpact(true);
            impact.setAlertLevel("MEDIUM");
            return impact;
        }

        // No significant impact
        impact.setRecommendation("NO_IMPACT");
        impact.setConfidence(5.0);
        impact.setScoringImpact(0.0);
        impact.setReason("Weather conditions within normal parameters for soccer");
        return impact;
    }

    /**
     * Generic weather analysis for other sports
     */
    private WeatherImpact analyzeGenericWeather(WeatherData weather, StadiumLocation stadium) {
        WeatherImpact impact = new WeatherImpact();

        if (weather.isExtreme()) {
            impact.setRecommendation("UNDER");
            impact.setConfidence(6.0);
            impact.setScoringImpact(-3.0);
            impact.setReason("Extreme weather conditions may affect gameplay");
            impact.setSignificantImpact(true);
            impact.setAlertLevel("MEDIUM");
        } else {
            impact.setRecommendation("NO_IMPACT");
            impact.setConfidence(5.0);
            impact.setScoringImpact(0.0);
            impact.setReason("Weather conditions within normal parameters");
            impact.setSignificantImpact(false);
            impact.setAlertLevel("NONE");
        }

        return impact;
    }

    /**
     * Determine if wind is blowing out (toward outfield) based on direction
     * Most MLB parks are oriented with home plate facing east/northeast
     */
    private boolean isWindBlowingOut(String windDir) {
        if (windDir == null) return false;

        // Wind from south/southwest typically blows out to centerfield
        return windDir.contains("S") && !windDir.contains("E");
    }
}
