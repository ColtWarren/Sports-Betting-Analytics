package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.weather.WeatherData;
import com.coltwarren.sports_betting_analytics.service.weather.OpenWeatherMapService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple weather lookup by game string.
 *
 * Delegates data fetching to {@link OpenWeatherMapService} (single source of truth
 * for weather API calls). For sport-specific quantified impact analysis, use
 * {@link com.coltwarren.sports_betting_analytics.service.weather.WeatherAnalysisService}.
 */
@Service
public class WeatherService {

    private final OpenWeatherMapService openWeatherMapService;

    // City to coordinates mapping for NFL stadiums
    private static final Map<String, double[]> NFL_STADIUMS = new ConcurrentHashMap<>();
    static {
        NFL_STADIUMS.put("Kansas City", new double[]{39.0489, -94.4839});
        NFL_STADIUMS.put("Buffalo", new double[]{42.7738, -78.7870});
        NFL_STADIUMS.put("Philadelphia", new double[]{39.9008, -75.1675});
        NFL_STADIUMS.put("San Francisco", new double[]{37.4032, -121.9696});
        NFL_STADIUMS.put("Jacksonville", new double[]{30.3240, -81.6373});
        NFL_STADIUMS.put("Green Bay", new double[]{44.5013, -88.0622});
        NFL_STADIUMS.put("Chicago", new double[]{41.8623, -87.6167});
        NFL_STADIUMS.put("Dallas", new double[]{32.7473, -97.0945});
        NFL_STADIUMS.put("Miami", new double[]{25.9580, -80.2389});
        NFL_STADIUMS.put("New England", new double[]{42.0909, -71.2643});
        NFL_STADIUMS.put("New York", new double[]{40.8128, -74.0742});
        NFL_STADIUMS.put("Las Vegas", new double[]{36.0909, -115.1833});
        NFL_STADIUMS.put("Los Angeles", new double[]{33.9534, -118.3392});
        NFL_STADIUMS.put("Denver", new double[]{39.7439, -105.0201});
        NFL_STADIUMS.put("Seattle", new double[]{47.5952, -122.3316});
        NFL_STADIUMS.put("Tampa Bay", new double[]{27.9759, -82.5033});
        NFL_STADIUMS.put("Atlanta", new double[]{33.7553, -84.4008});
        NFL_STADIUMS.put("New Orleans", new double[]{29.9511, -90.0812});
        NFL_STADIUMS.put("Detroit", new double[]{42.3400, -83.0456});
        NFL_STADIUMS.put("Minnesota", new double[]{44.9738, -93.2577});
        NFL_STADIUMS.put("Baltimore", new double[]{39.2780, -76.6227});
        NFL_STADIUMS.put("Pittsburgh", new double[]{40.4468, -80.0158});
        NFL_STADIUMS.put("Cleveland", new double[]{41.5061, -81.6995});
        NFL_STADIUMS.put("Cincinnati", new double[]{39.0954, -84.5160});
        NFL_STADIUMS.put("Houston", new double[]{29.6847, -95.4107});
        NFL_STADIUMS.put("Indianapolis", new double[]{39.7601, -86.1639});
        NFL_STADIUMS.put("Tennessee", new double[]{36.1665, -86.7713});
        NFL_STADIUMS.put("Carolina", new double[]{35.2258, -80.8529});
        NFL_STADIUMS.put("Washington", new double[]{38.9076, -76.8645});
        NFL_STADIUMS.put("Arizona", new double[]{33.5276, -112.2626});
    }

    public WeatherService(OpenWeatherMapService openWeatherMapService) {
        this.openWeatherMapService = openWeatherMapService;
    }

    public Map<String, Object> getWeatherForGame(String game) {
        try {
            String city = extractCity(game);

            if (city == null || !NFL_STADIUMS.containsKey(city)) {
                return createUnavailableWeather("City not found");
            }

            double[] coords = NFL_STADIUMS.get(city);

            WeatherData data = openWeatherMapService.getWeatherForecast(
                    coords[0], coords[1], LocalDateTime.now(), city);

            if (data == null) {
                return createUnavailableWeather("Weather data unavailable");
            }

            return formatWeatherData(data, city);

        } catch (Exception e) {
            return createUnavailableWeather("Error: " + e.getMessage());
        }
    }

    private String extractCity(String game) {
        for (String city : NFL_STADIUMS.keySet()) {
            if (game.contains(city)) {
                return city;
            }
            if (game.contains("Chiefs") && city.equals("Kansas City")) return city;
            if (game.contains("Bills") && city.equals("Buffalo")) return city;
            if (game.contains("Eagles") && city.equals("Philadelphia")) return city;
            if (game.contains("49ers") && city.equals("San Francisco")) return city;
            if (game.contains("Jaguars") && city.equals("Jacksonville")) return city;
            if (game.contains("Packers") && city.equals("Green Bay")) return city;
            if (game.contains("Bears") && city.equals("Chicago")) return city;
            if (game.contains("Cowboys") && city.equals("Dallas")) return city;
            if (game.contains("Dolphins") && city.equals("Miami")) return city;
            if (game.contains("Patriots") && city.equals("New England")) return city;
            if (game.contains("Giants") || game.contains("Jets")) return "New York";
            if (game.contains("Raiders") && city.equals("Las Vegas")) return city;
            if (game.contains("Rams") || game.contains("Chargers")) return "Los Angeles";
            if (game.contains("Broncos") && city.equals("Denver")) return city;
            if (game.contains("Seahawks") && city.equals("Seattle")) return city;
            if (game.contains("Buccaneers") && city.equals("Tampa Bay")) return city;
            if (game.contains("Falcons") && city.equals("Atlanta")) return city;
            if (game.contains("Saints") && city.equals("New Orleans")) return city;
            if (game.contains("Lions") && city.equals("Detroit")) return city;
            if (game.contains("Vikings") && city.equals("Minnesota")) return city;
            if (game.contains("Ravens") && city.equals("Baltimore")) return city;
            if (game.contains("Steelers") && city.equals("Pittsburgh")) return city;
            if (game.contains("Browns") && city.equals("Cleveland")) return city;
            if (game.contains("Bengals") && city.equals("Cincinnati")) return city;
            if (game.contains("Texans") && city.equals("Houston")) return city;
            if (game.contains("Colts") && city.equals("Indianapolis")) return city;
            if (game.contains("Titans") && city.equals("Tennessee")) return city;
            if (game.contains("Panthers") && city.equals("Carolina")) return city;
            if (game.contains("Commanders") && city.equals("Washington")) return city;
            if (game.contains("Cardinals") && city.equals("Arizona")) return city;
        }
        return null;
    }

    private Map<String, Object> formatWeatherData(WeatherData data, String city) {
        Map<String, Object> weather = new HashMap<>();

        weather.put("available", true);
        weather.put("city", city);
        weather.put("temperature", Math.round(data.getTemperature()));
        weather.put("feelsLike", Math.round(data.getFeelsLike()));
        weather.put("humidity", data.getHumidity());
        weather.put("windSpeed", Math.round(data.getWindSpeed()));
        weather.put("windDirection", data.getWindDirection() != null ? data.getWindDirection() : "N/A");
        weather.put("condition", data.getConditions() != null ? data.getConditions() : "Unknown");
        weather.put("icon", getWeatherIcon(data.getConditions()));
        weather.put("bettingImpact", assessBettingImpact(
                data.getTemperature(), data.getWindSpeed(), data.getConditions()));

        return weather;
    }

    private Map<String, Object> createUnavailableWeather(String reason) {
        Map<String, Object> weather = new HashMap<>();
        weather.put("available", false);
        weather.put("reason", reason);
        weather.put("icon", "?");
        weather.put("message", "Weather data unavailable");
        return weather;
    }

    private String getWeatherIcon(String condition) {
        if (condition == null) return "?";
        return switch (condition.toLowerCase()) {
            case "clear" -> "sunny";
            case "clouds" -> "cloudy";
            case "rain" -> "rainy";
            case "drizzle" -> "drizzle";
            case "snow" -> "snowy";
            case "thunderstorm" -> "stormy";
            case "mist", "fog" -> "foggy";
            default -> "partly_cloudy";
        };
    }

    private String assessBettingImpact(double temp, double windSpeed, String condition) {
        StringBuilder impact = new StringBuilder();

        if (temp < 32) {
            impact.append("Very Cold - Favors running game, lowers totals. ");
        } else if (temp < 45) {
            impact.append("Cold - May affect passing game slightly. ");
        } else if (temp > 85) {
            impact.append("Hot - May tire defenses in 4th quarter. ");
        } else {
            impact.append("Ideal - Temperature neutral. ");
        }

        if (windSpeed > 20) {
            impact.append("HIGH WIND - Significantly impacts passing/kicking. Bet UNDER. ");
        } else if (windSpeed > 15) {
            impact.append("Windy - May affect deep passes and field goals. ");
        } else {
            impact.append("Light Wind - Minimal impact. ");
        }

        if (condition != null && (condition.toLowerCase().contains("rain") || condition.toLowerCase().contains("snow"))) {
            impact.append("Precipitation - Favors rushing, bet UNDER. ");
        }

        return impact.toString().trim();
    }
}
