package com.coltwarren.sports_betting_analytics.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Cache Configuration
 *
 * Configures in-memory caching for frequently accessed data.
 * Reduces API calls and improves response times.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ODDS_CACHE = "oddsCache";
    public static final String XG_CACHE = "xgCache";
    public static final String WEATHER_CACHE = "weatherCache";
    public static final String PATTERNS_CACHE = "patternsCache";
    public static final String INJURIES_CACHE = "injuriesCache";
    public static final String PUBLIC_BETTING_CACHE = "publicBettingCache";
    public static final String BEST_BETS_CACHE = "bestBetsCache";
    public static final String HISTORICAL_CACHE = "historicalCache";
    public static final String CFB_DATA_CACHE = "cfbDataCache";      // CollegeFootballData.com
    public static final String CBB_DATA_CACHE = "cbbDataCache";      // CollegeBasketballData.com

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache(ODDS_CACHE),
            new ConcurrentMapCache(XG_CACHE),
            new ConcurrentMapCache(WEATHER_CACHE),
            new ConcurrentMapCache(PATTERNS_CACHE),
            new ConcurrentMapCache(INJURIES_CACHE),
            new ConcurrentMapCache(PUBLIC_BETTING_CACHE),
            new ConcurrentMapCache(BEST_BETS_CACHE),
            new ConcurrentMapCache(HISTORICAL_CACHE),
            new ConcurrentMapCache(CFB_DATA_CACHE),
            new ConcurrentMapCache(CBB_DATA_CACHE)
        ));
        return cacheManager;
    }
}
