package com.coltwarren.sports_betting_analytics.model.weather;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Weather Data Model
 *
 * Contains weather conditions for a specific location and time.
 * Used to analyze potential impact on sporting events.
 *
 * Key metrics for sports betting:
 * - Wind Speed: Critical for NFL passing, MLB home runs
 * - Temperature: Affects player performance, ball flight
 * - Precipitation: Impacts footing, ball handling
 * - Wind Direction: Critical for MLB (wind blowing in/out)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherData {

    private Double temperature; // Fahrenheit
    private Double feelsLike; // Wind chill / Heat index
    private Double windSpeed; // mph
    private Double windGust; // mph (peak gusts)
    private String windDirection; // N, NE, E, SE, S, SW, W, NW
    private Integer windDegrees; // 0-360 degrees
    private Double precipitation; // inches expected
    private Double precipitationProbability; // 0-100%
    private Integer humidity; // percentage
    private String conditions; // Clear, Cloudy, Rain, Snow, etc.
    private String conditionsDescription; // More detailed description
    private Integer visibility; // miles
    private Double pressure; // millibars (affects ball flight at altitude)
    private LocalDateTime timestamp;
    private String stadium;
    private String source; // API source (OpenWeatherMap, etc.)

    /**
     * Check if conditions are "extreme" for sports
     */
    public boolean isExtreme() {
        // Extreme wind
        if (windSpeed != null && windSpeed > 25) return true;
        // Extreme cold
        if (temperature != null && temperature < 10) return true;
        // Extreme heat
        if (temperature != null && temperature > 100) return true;
        // Heavy precipitation
        if (precipitation != null && precipitation > 0.5) return true;
        return false;
    }

    /**
     * Get wind chill if applicable (cold + wind)
     */
    public Double getEffectiveTemperature() {
        if (feelsLike != null) return feelsLike;
        if (temperature == null || windSpeed == null) return temperature;

        // Wind chill formula (valid for temps <= 50°F and wind >= 3 mph)
        if (temperature <= 50 && windSpeed >= 3) {
            return 35.74 + (0.6215 * temperature) -
                   (35.75 * Math.pow(windSpeed, 0.16)) +
                   (0.4275 * temperature * Math.pow(windSpeed, 0.16));
        }
        return temperature;
    }

    /**
     * Check if it's raining or snowing
     */
    public boolean hasPrecipitation() {
        if (precipitation != null && precipitation > 0) return true;
        if (conditions == null) return false;
        String lower = conditions.toLowerCase();
        return lower.contains("rain") || lower.contains("snow") ||
               lower.contains("drizzle") || lower.contains("shower");
    }

    /**
     * Get severity level: 0 = ideal, 1 = mild, 2 = moderate, 3 = severe
     */
    public int getSeverityLevel() {
        int severity = 0;

        // Wind severity
        if (windSpeed != null) {
            if (windSpeed > 25) severity = Math.max(severity, 3);
            else if (windSpeed > 18) severity = Math.max(severity, 2);
            else if (windSpeed > 12) severity = Math.max(severity, 1);
        }

        // Temperature severity
        if (temperature != null) {
            if (temperature < 15 || temperature > 100) severity = Math.max(severity, 3);
            else if (temperature < 25 || temperature > 95) severity = Math.max(severity, 2);
            else if (temperature < 35 || temperature > 90) severity = Math.max(severity, 1);
        }

        // Precipitation severity
        if (precipitation != null) {
            if (precipitation > 0.5) severity = Math.max(severity, 3);
            else if (precipitation > 0.2) severity = Math.max(severity, 2);
            else if (precipitation > 0) severity = Math.max(severity, 1);
        }

        return severity;
    }

    /**
     * Get a human-readable summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        if (temperature != null) {
            sb.append(String.format("%.0f°F", temperature));
        }

        if (conditions != null && !conditions.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(conditions);
        }

        if (windSpeed != null && windSpeed > 5) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(String.format("Wind: %.0f mph %s", windSpeed,
                windDirection != null ? windDirection : ""));
        }

        return sb.toString();
    }
}
