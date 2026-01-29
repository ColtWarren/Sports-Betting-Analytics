package com.coltwarren.sports_betting_analytics.model.weather;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Weather Impact Model
 *
 * Represents the analyzed impact of weather conditions on a sporting event.
 * Used to adjust betting recommendations and Kelly Criterion calculations.
 *
 * Key fields:
 * - recommendation: What to bet based on weather (OVER, UNDER, etc.)
 * - confidence: How confident we are in the weather impact (0-10)
 * - scoringImpact: Expected change in total points/runs
 * - spreadImpact: Expected change in spread
 */
@Data
@NoArgsConstructor
public class WeatherImpact {

    /**
     * Betting recommendation based on weather
     * Values: OVER, UNDER, NO_IMPACT, FADE_FAVORITE, TAKE_UNDERDOG
     */
    private String recommendation;

    /**
     * Confidence in the weather impact (0-10 scale)
     * Higher = more confident weather will affect the game
     */
    private Double confidence;

    /**
     * Human-readable explanation of the weather impact
     */
    private String reason;

    /**
     * Expected change in total scoring
     * Negative = fewer points/runs expected
     * Positive = more points/runs expected
     */
    private Double scoringImpact;

    /**
     * Expected change to the spread
     * Positive = favorite should cover by more
     * Negative = underdog gets help
     */
    private Double spreadImpact;

    /**
     * True if weather is expected to significantly impact the game
     */
    private Boolean significantImpact;

    /**
     * The weather data that led to this analysis
     */
    private WeatherData weatherData;

    /**
     * Sport-specific notes
     */
    private String sportNotes;

    /**
     * Stadium name
     */
    private String stadium;

    /**
     * Alert level: NONE, LOW, MEDIUM, HIGH, CRITICAL
     */
    private String alertLevel;

    /**
     * Default constructor with sensible defaults
     */
    public static WeatherImpact noImpact() {
        WeatherImpact impact = new WeatherImpact();
        impact.setRecommendation("NO_IMPACT");
        impact.setConfidence(5.0);
        impact.setReason("Weather conditions within normal parameters");
        impact.setScoringImpact(0.0);
        impact.setSpreadImpact(0.0);
        impact.setSignificantImpact(false);
        impact.setAlertLevel("NONE");
        return impact;
    }

    /**
     * Create a high-wind impact
     */
    public static WeatherImpact highWind(double windSpeed, String sport) {
        WeatherImpact impact = new WeatherImpact();

        if ("NFL".equals(sport)) {
            impact.setRecommendation("UNDER");
            impact.setConfidence(windSpeed > 20 ? 9.0 : 7.5);
            impact.setScoringImpact(windSpeed > 20 ? -10.5 : -7.0);
            impact.setReason(String.format("High wind (%.0f mph) reduces passing efficiency", windSpeed));
            impact.setSportNotes("Passing game severely impacted. Expect run-heavy game plans.");
        } else if ("MLB".equals(sport)) {
            impact.setRecommendation("UNDER"); // Depends on direction, simplified here
            impact.setConfidence(7.0);
            impact.setScoringImpact(-2.5);
            impact.setReason(String.format("Strong wind (%.0f mph) affects ball flight", windSpeed));
        }

        impact.setSignificantImpact(true);
        impact.setAlertLevel(windSpeed > 20 ? "HIGH" : "MEDIUM");
        return impact;
    }

    /**
     * Create an extreme cold impact
     */
    public static WeatherImpact extremeCold(double temperature, String sport) {
        WeatherImpact impact = new WeatherImpact();

        impact.setRecommendation("UNDER");
        impact.setSignificantImpact(true);

        if (temperature < 10) {
            impact.setConfidence(8.5);
            impact.setScoringImpact(-9.0);
            impact.setReason(String.format("Extreme cold (%.0f°F) significantly reduces offensive production", temperature));
            impact.setAlertLevel("CRITICAL");
        } else if (temperature < 20) {
            impact.setConfidence(7.0);
            impact.setScoringImpact(-5.5);
            impact.setReason(String.format("Cold weather (%.0f°F) favors defensive play", temperature));
            impact.setAlertLevel("HIGH");
        } else {
            impact.setConfidence(6.0);
            impact.setScoringImpact(-3.5);
            impact.setReason(String.format("Cool weather (%.0f°F) may slightly reduce scoring", temperature));
            impact.setAlertLevel("MEDIUM");
        }

        impact.setSportNotes("Cold affects ball grip, player mobility, and kicking accuracy.");
        return impact;
    }

    /**
     * Create a precipitation impact
     */
    public static WeatherImpact precipitation(double amount, String type, String sport) {
        WeatherImpact impact = new WeatherImpact();

        impact.setRecommendation("UNDER");
        impact.setSignificantImpact(true);

        if ("snow".equalsIgnoreCase(type)) {
            impact.setConfidence(8.0);
            impact.setScoringImpact(-8.5);
            impact.setReason(String.format("Snow expected (%.1f in) - major impact on footing and ball handling", amount));
            impact.setAlertLevel("CRITICAL");
        } else if (amount > 0.5) {
            impact.setConfidence(7.0);
            impact.setScoringImpact(-6.5);
            impact.setReason(String.format("Heavy rain expected (%.2f in) - wet conditions favor run game", amount));
            impact.setAlertLevel("HIGH");
        } else {
            impact.setConfidence(5.5);
            impact.setScoringImpact(-3.0);
            impact.setReason(String.format("Light precipitation expected (%.2f in)", amount));
            impact.setAlertLevel("MEDIUM");
        }

        return impact;
    }

    /**
     * Get a formatted summary for display
     */
    public String getFormattedSummary() {
        if (!Boolean.TRUE.equals(significantImpact)) {
            return "No significant weather impact expected";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ WEATHER ALERT: ").append(reason);

        if (scoringImpact != null && scoringImpact != 0) {
            sb.append(String.format(" | Expected scoring impact: %+.1f points", scoringImpact));
        }

        if (recommendation != null && !"NO_IMPACT".equals(recommendation)) {
            sb.append(" | Suggests: ").append(recommendation);
        }

        return sb.toString();
    }

    /**
     * Get confidence as a percentage string
     */
    public String getConfidenceDisplay() {
        if (confidence == null) return "N/A";
        return String.format("%.0f%%", confidence * 10);
    }

    /**
     * Check if this impact should trigger an alert on the UI
     */
    public boolean shouldShowAlert() {
        return Boolean.TRUE.equals(significantImpact) &&
               confidence != null && confidence >= 6.0;
    }
}
