package com.coltwarren.sports_betting_analytics.service.weather;

import com.coltwarren.sports_betting_analytics.model.weather.WeatherData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenWeatherMap API Service
 *
 * Fetches weather forecasts for stadium locations.
 * Uses the free tier (1000 calls/day, 5-day forecast).
 *
 * API Documentation: https://openweathermap.org/api/one-call-3
 *
 * Free tier limitations:
 * - 1000 API calls per day
 * - 5-day forecast (3-hour intervals)
 * - Current weather
 * - No historical data
 */
@Service
@Slf4j
public class OpenWeatherMapService {

    @Value("${openweathermap.api.key:}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache weather data for 30 minutes to save API calls
    private final Map<String, CachedWeather> weatherCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MINUTES = 30;

    public OpenWeatherMapService() {
        this.webClient = WebClient.builder()
            .baseUrl("https://api.openweathermap.org/data/2.5")
            .build();
    }

    /**
     * Get weather forecast for a location at a specific game time
     */
    public WeatherData getWeatherForecast(Double lat, Double lon, LocalDateTime gameTime, String stadiumName) {
        // Check cache first
        String cacheKey = String.format("%.4f,%.4f", lat, lon);
        CachedWeather cached = weatherCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Using cached weather for {}", cacheKey);
            return findClosestForecast(cached.data, gameTime, stadiumName);
        }

        // If no API key, return mock data
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("No OpenWeatherMap API key configured - using mock data");
            return getMockWeatherData(lat, lon, gameTime, stadiumName);
        }

        try {
            // Fetch 5-day forecast
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/forecast")
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "imperial")
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));

            if (response == null) {
                log.error("No response from OpenWeatherMap");
                return getMockWeatherData(lat, lon, gameTime, stadiumName);
            }

            // Parse response
            JsonNode root = objectMapper.readTree(response);
            JsonNode forecastList = root.get("list");

            if (forecastList == null || !forecastList.isArray()) {
                log.error("Invalid forecast response format");
                return getMockWeatherData(lat, lon, gameTime, stadiumName);
            }

            // Cache the response
            weatherCache.put(cacheKey, new CachedWeather(root, System.currentTimeMillis()));

            // Find forecast closest to game time
            return findClosestForecast(root, gameTime, stadiumName);

        } catch (Exception e) {
            log.error("Error fetching weather from OpenWeatherMap: {}", e.getMessage());
            return getMockWeatherData(lat, lon, gameTime, stadiumName);
        }
    }

    /**
     * Find the forecast entry closest to the game time
     */
    private WeatherData findClosestForecast(JsonNode root, LocalDateTime gameTime, String stadiumName) {
        JsonNode forecastList = root.get("list");
        if (forecastList == null) {
            return null;
        }

        long gameTimestamp = gameTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        JsonNode closestForecast = null;
        long closestDiff = Long.MAX_VALUE;

        for (JsonNode forecast : forecastList) {
            long forecastTime = forecast.get("dt").asLong();
            long diff = Math.abs(forecastTime - gameTimestamp);
            if (diff < closestDiff) {
                closestDiff = diff;
                closestForecast = forecast;
            }
        }

        if (closestForecast == null) {
            return null;
        }

        return parseWeatherData(closestForecast, stadiumName);
    }

    /**
     * Parse OpenWeatherMap JSON into WeatherData
     */
    private WeatherData parseWeatherData(JsonNode forecast, String stadiumName) {
        WeatherData weather = new WeatherData();

        try {
            // Main weather data
            JsonNode main = forecast.get("main");
            if (main != null) {
                weather.setTemperature(main.get("temp").asDouble());
                weather.setFeelsLike(main.get("feels_like").asDouble());
                weather.setHumidity(main.get("humidity").asInt());
                weather.setPressure(main.get("pressure").asDouble());
            }

            // Wind data
            JsonNode wind = forecast.get("wind");
            if (wind != null) {
                weather.setWindSpeed(wind.get("speed").asDouble());
                if (wind.has("gust")) {
                    weather.setWindGust(wind.get("gust").asDouble());
                }
                if (wind.has("deg")) {
                    int degrees = wind.get("deg").asInt();
                    weather.setWindDegrees(degrees);
                    weather.setWindDirection(degreesToCardinal(degrees));
                }
            }

            // Weather conditions
            JsonNode weatherArray = forecast.get("weather");
            if (weatherArray != null && weatherArray.isArray() && weatherArray.size() > 0) {
                JsonNode weatherInfo = weatherArray.get(0);
                weather.setConditions(weatherInfo.get("main").asText());
                weather.setConditionsDescription(weatherInfo.get("description").asText());
            }

            // Precipitation (rain or snow)
            double precip = 0.0;
            if (forecast.has("rain") && forecast.get("rain").has("3h")) {
                precip += forecast.get("rain").get("3h").asDouble() / 25.4; // mm to inches
            }
            if (forecast.has("snow") && forecast.get("snow").has("3h")) {
                precip += forecast.get("snow").get("3h").asDouble() / 25.4; // mm to inches
            }
            weather.setPrecipitation(precip);

            // Probability of precipitation
            if (forecast.has("pop")) {
                weather.setPrecipitationProbability(forecast.get("pop").asDouble() * 100);
            }

            // Visibility (meters to miles)
            if (forecast.has("visibility")) {
                weather.setVisibility((int) (forecast.get("visibility").asInt() / 1609.34));
            }

            // Timestamp
            if (forecast.has("dt")) {
                long timestamp = forecast.get("dt").asLong();
                weather.setTimestamp(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()));
            }

            weather.setStadium(stadiumName);
            weather.setSource("OpenWeatherMap");

            log.info("🌤️ Weather for {}: {}°F, Wind: {} mph {}, {}",
                     stadiumName, weather.getTemperature(), weather.getWindSpeed(),
                     weather.getWindDirection(), weather.getConditions());

        } catch (Exception e) {
            log.error("Error parsing weather data: {}", e.getMessage());
        }

        return weather;
    }

    /**
     * Convert wind degrees to cardinal direction
     */
    private String degreesToCardinal(int degrees) {
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                              "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        int index = (int) Math.round((degrees % 360) / 22.5) % 16;
        return directions[index];
    }

    /**
     * Get mock weather data when API is unavailable
     * Simulates various weather scenarios for testing
     */
    private WeatherData getMockWeatherData(Double lat, Double lon, LocalDateTime gameTime, String stadiumName) {
        WeatherData weather = new WeatherData();

        // Vary mock data based on latitude (northern = colder) and time of year
        int month = gameTime.getMonthValue();
        boolean isWinter = month <= 2 || month >= 11;
        boolean isNorthern = lat > 40;

        // Temperature based on season and location
        double baseTemp = isWinter ? 35.0 : 72.0;
        if (isNorthern && isWinter) {
            baseTemp -= 15; // Colder in the north during winter
        }
        // Add some randomness based on coordinates
        baseTemp += (lon % 10) - 5;

        weather.setTemperature(baseTemp);
        weather.setFeelsLike(baseTemp - 3);

        // Wind (more common in winter and at certain stadiums)
        double windSpeed = 8.0 + (lat % 10);
        if (isWinter) windSpeed += 5;
        weather.setWindSpeed(windSpeed);
        weather.setWindGust(windSpeed + 5);

        // Simulate wind direction
        int windDeg = (int) ((lon * 10) % 360);
        weather.setWindDegrees(windDeg);
        weather.setWindDirection(degreesToCardinal(windDeg));

        // Precipitation based on season
        weather.setPrecipitation(isWinter ? 0.1 : 0.0);
        weather.setPrecipitationProbability(isWinter ? 30.0 : 10.0);

        // Humidity
        weather.setHumidity(isWinter ? 75 : 55);

        // Conditions
        if (isWinter && baseTemp < 32) {
            weather.setConditions("Snow");
            weather.setConditionsDescription("light snow");
            weather.setPrecipitation(0.3);
        } else if (isWinter) {
            weather.setConditions("Clouds");
            weather.setConditionsDescription("overcast clouds");
        } else {
            weather.setConditions("Clear");
            weather.setConditionsDescription("clear sky");
        }

        weather.setVisibility(10);
        weather.setPressure(1015.0);
        weather.setTimestamp(gameTime);
        weather.setStadium(stadiumName);
        weather.setSource("Mock Data");

        log.info("🌤️ [MOCK] Weather for {}: {}°F, Wind: {} mph {}, {}",
                 stadiumName, weather.getTemperature(), weather.getWindSpeed(),
                 weather.getWindDirection(), weather.getConditions());

        return weather;
    }

    /**
     * Get current weather (simpler endpoint, uses less quota)
     */
    public WeatherData getCurrentWeather(Double lat, Double lon, String stadiumName) {
        return getWeatherForecast(lat, lon, LocalDateTime.now(), stadiumName);
    }

    /**
     * Cache entry for weather data
     */
    private static class CachedWeather {
        final JsonNode data;
        final long timestamp;

        CachedWeather(JsonNode data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            long age = System.currentTimeMillis() - timestamp;
            return age > CACHE_DURATION_MINUTES * 60 * 1000;
        }
    }
}
