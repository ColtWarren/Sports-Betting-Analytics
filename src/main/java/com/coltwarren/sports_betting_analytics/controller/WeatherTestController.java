package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
public class WeatherTestController {
    
    private final WeatherService weatherService;
    
    @Autowired
    public WeatherTestController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }
    
    @GetMapping("/test")
    public Map<String, Object> testWeather(@RequestParam(defaultValue = "Buffalo Bills @ Jacksonville Jaguars") String game) {
        return weatherService.getWeatherForGame(game);
    }
}
