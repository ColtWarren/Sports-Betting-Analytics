package com.coltwarren.sports_betting_analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "odds-api")
public class OddsApiProperties {

    private boolean enabled = true;
    private String apiKey;
    private String baseUrl = "https://api.the-odds-api.com/v4";

    // Scheduling
    private int refreshIntervalMinutes = 15;
    private boolean startupFetch = true;

    // Rate limiting
    private int delayBetweenLeaguesMs = 300;
    private int maxRetries = 3;
    private int retryDelayMs = 1000;

    // Cache settings
    private int cacheTtlMinutes = 20;
    private boolean serveStaleIfError = true;
    private int staleMaxAgeMinutes = 60;

    // Leagues to fetch
    private List<String> enabledLeagues = List.of(
        "americanfootball_nfl",
        "basketball_nba",
        "basketball_ncaab",
        "basketball_wnba",
        "basketball_wncaab",
        "icehockey_nhl",
        "baseball_mlb",
        "americanfootball_ncaaf",
        "soccer_epl",
        "soccer_usa_mls",
        "soccer_uefa_champs_league",
        "soccer_spain_la_liga",
        "soccer_italy_serie_a",
        "soccer_france_ligue_one",
        "soccer_germany_bundesliga"
    );

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public int getRefreshIntervalMinutes() { return refreshIntervalMinutes; }
    public void setRefreshIntervalMinutes(int refreshIntervalMinutes) { this.refreshIntervalMinutes = refreshIntervalMinutes; }

    public boolean isStartupFetch() { return startupFetch; }
    public void setStartupFetch(boolean startupFetch) { this.startupFetch = startupFetch; }

    public int getDelayBetweenLeaguesMs() { return delayBetweenLeaguesMs; }
    public void setDelayBetweenLeaguesMs(int delayBetweenLeaguesMs) { this.delayBetweenLeaguesMs = delayBetweenLeaguesMs; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public int getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(int retryDelayMs) { this.retryDelayMs = retryDelayMs; }

    public int getCacheTtlMinutes() { return cacheTtlMinutes; }
    public void setCacheTtlMinutes(int cacheTtlMinutes) { this.cacheTtlMinutes = cacheTtlMinutes; }

    public boolean isServeStaleIfError() { return serveStaleIfError; }
    public void setServeStaleIfError(boolean serveStaleIfError) { this.serveStaleIfError = serveStaleIfError; }

    public int getStaleMaxAgeMinutes() { return staleMaxAgeMinutes; }
    public void setStaleMaxAgeMinutes(int staleMaxAgeMinutes) { this.staleMaxAgeMinutes = staleMaxAgeMinutes; }

    public List<String> getEnabledLeagues() { return enabledLeagues; }
    public void setEnabledLeagues(List<String> enabledLeagues) { this.enabledLeagues = enabledLeagues; }
}
