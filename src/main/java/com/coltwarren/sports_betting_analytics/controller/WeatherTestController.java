package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.model.StadiumLocation;
import com.coltwarren.sports_betting_analytics.model.weather.WeatherData;
import com.coltwarren.sports_betting_analytics.model.weather.WeatherImpact;
import com.coltwarren.sports_betting_analytics.repository.StadiumLocationRepository;
import com.coltwarren.sports_betting_analytics.service.weather.OpenWeatherMapService;
import com.coltwarren.sports_betting_analytics.service.weather.WeatherAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Weather Test Controller
 */
@RestController
@RequestMapping("/api/test")
public class WeatherTestController {

    @Autowired
    private WeatherAnalysisService weatherAnalysisService;

    @Autowired
    private OpenWeatherMapService openWeatherMapService;

    @Autowired
    private StadiumLocationRepository stadiumRepository;

    @GetMapping("/weather/{stadiumName}")
    public ResponseEntity<?> testWeather(@PathVariable String stadiumName) {
        // Find stadium by partial name match - use List and take first
        List<StadiumLocation> matches = stadiumRepository.findByStadiumNameContaining(stadiumName);
        
        if (matches.isEmpty()) {
            // Try by team name
            matches = stadiumRepository.findAll().stream()
                .filter(s -> s.getHomeTeams() != null && 
                            s.getHomeTeams().toLowerCase().contains(stadiumName.toLowerCase()))
                .toList();
        }

        if (matches.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Stadium not found: " + stadiumName);
            error.put("suggestion", "Try: Arrowhead, Lambeau, Wrigley, AT&T, CityPark, Children");
            return ResponseEntity.badRequest().body(error);
        }

        StadiumLocation stadium = matches.get(0);

        WeatherData weather = openWeatherMapService.getWeatherForecast(
            stadium.getLatitude(),
            stadium.getLongitude(),
            LocalDateTime.now(),
            stadium.getStadiumName()
        );

        WeatherImpact impact = weatherAnalysisService.analyzeGameWeather(
            stadium.getHomeTeams(),
            stadium.getSport(),
            LocalDateTime.now()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("stadium", stadium.getStadiumName());
        result.put("city", stadium.getCity());
        result.put("state", stadium.getState());
        result.put("sport", stadium.getSport());
        result.put("isDome", stadium.getIsDome());
        result.put("hasRetractableRoof", stadium.getHasRetractableRoof());
        result.put("coordinates", String.format("%.4f, %.4f", stadium.getLatitude(), stadium.getLongitude()));
        result.put("homeTeams", stadium.getHomeTeams());
        result.put("currentWeather", formatWeatherData(weather));
        result.put("bettingImpact", formatWeatherImpact(impact));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/weather/all-stadiums")
    public ResponseEntity<?> listAllStadiums() {
        List<StadiumLocation> stadiums = stadiumRepository.findAll();

        Map<String, Object> result = new HashMap<>();
        result.put("totalStadiums", stadiums.size());
        result.put("outdoorStadiums", stadiums.stream().filter(s -> !s.getIsDome()).count());
        result.put("domedStadiums", stadiums.stream().filter(s -> s.getIsDome()).count());

        Map<String, List<Map<String, Object>>> bySport = new HashMap<>();
        bySport.put("NFL", stadiums.stream()
            .filter(s -> "NFL".equals(s.getSport()))
            .map(this::formatStadiumSummary)
            .toList());
        bySport.put("MLB", stadiums.stream()
            .filter(s -> "MLB".equals(s.getSport()))
            .map(this::formatStadiumSummary)
            .toList());
        bySport.put("MLS", stadiums.stream()
            .filter(s -> "MLS".equals(s.getSport()))
            .map(this::formatStadiumSummary)
            .toList());

        result.put("stadiumsBySport", bySport);
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> formatWeatherData(WeatherData weather) {
        if (weather == null) return Map.of("error", "No weather data available");

        Map<String, Object> map = new HashMap<>();
        map.put("temperature", weather.getTemperature());
        map.put("feelsLike", weather.getFeelsLike());
        map.put("windSpeed", weather.getWindSpeed());
        map.put("windGust", weather.getWindGust());
        map.put("windDirection", weather.getWindDirection());
        map.put("humidity", weather.getHumidity());
        map.put("conditions", weather.getConditions());
        map.put("conditionsDescription", weather.getConditionsDescription());
        map.put("precipitation", weather.getPrecipitation());
        map.put("precipitationProbability", weather.getPrecipitationProbability());
        map.put("visibility", weather.getVisibility());
        map.put("source", weather.getSource());
        map.put("isExtreme", weather.isExtreme());
        map.put("severityLevel", weather.getSeverityLevel());
        return map;
    }

    private Map<String, Object> formatWeatherImpact(WeatherImpact impact) {
        if (impact == null) return Map.of("error", "No impact analysis available");

        Map<String, Object> map = new HashMap<>();
        map.put("recommendation", impact.getRecommendation());
        map.put("confidence", impact.getConfidence());
        map.put("reason", impact.getReason());
        map.put("scoringImpact", impact.getScoringImpact());
        map.put("spreadImpact", impact.getSpreadImpact());
        map.put("significantImpact", impact.getSignificantImpact());
        map.put("alertLevel", impact.getAlertLevel());
        map.put("sportNotes", impact.getSportNotes());
        return map;
    }

    private Map<String, Object> formatStadiumSummary(StadiumLocation stadium) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", stadium.getStadiumName());
        map.put("city", stadium.getCity());
        map.put("state", stadium.getState());
        map.put("isDome", stadium.getIsDome());
        map.put("teams", stadium.getHomeTeams());
        return map;
    }
}
