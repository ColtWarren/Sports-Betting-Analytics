package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Cache Eviction Service
 *
 * Manages cache lifecycle with scheduled evictions
 * to ensure data freshness.
 */
@Service
@Slf4j
public class CacheEvictionService {

    private final CacheManager cacheManager;

    private LocalDateTime lastEviction = LocalDateTime.now();

    public CacheEvictionService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Clear odds cache every 5 minutes (odds change frequently)
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void evictOddsCache() {
        evictCache(CacheConfig.ODDS_CACHE);
        log.trace("Odds cache evicted");
    }

    /**
     * Clear weather cache every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void evictWeatherCache() {
        evictCache(CacheConfig.WEATHER_CACHE);
        log.trace("Weather cache evicted");
    }

    /**
     * Clear xG cache every hour (xG data is more stable)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void evictXGCache() {
        evictCache(CacheConfig.XG_CACHE);
        log.trace("xG cache evicted");
    }

    /**
     * Clear injuries cache every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void evictInjuriesCache() {
        evictCache(CacheConfig.INJURIES_CACHE);
        log.trace("Injuries cache evicted");
    }

    /**
     * Clear public betting cache every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void evictPublicBettingCache() {
        evictCache(CacheConfig.PUBLIC_BETTING_CACHE);
        log.trace("Public betting cache evicted");
    }

    /**
     * Clear patterns cache daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void evictPatternsCache() {
        evictCache(CacheConfig.PATTERNS_CACHE);
        evictCache(CacheConfig.HISTORICAL_CACHE);
        log.info("Patterns and historical cache evicted (daily)");
    }

    /**
     * Clear best bets cache every 5 minutes (same as odds)
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void evictBestBetsCache() {
        evictCache(CacheConfig.BEST_BETS_CACHE);
        log.trace("Best bets cache evicted");
    }

    /**
     * Clear college data caches every 15 minutes
     * CollegeFootballData.com and CollegeBasketballData.com
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void evictCollegeDataCache() {
        evictCache(CacheConfig.CFB_DATA_CACHE);
        evictCache(CacheConfig.CBB_DATA_CACHE);
        log.trace("College sports data cache evicted (CFB, CBB)");
    }

    /**
     * Evict a specific cache
     */
    public void evictCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Clear all caches
     */
    public void evictAllCaches() {
        cacheManager.getCacheNames()
            .forEach(cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
        lastEviction = LocalDateTime.now();
        log.info("All caches evicted");
    }

    /**
     * Get last eviction time
     */
    public LocalDateTime getLastEviction() {
        return lastEviction;
    }

    /**
     * Get cache statistics
     */
    public java.util.Map<String, Object> getCacheStats() {
        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("lastEviction", lastEviction);
        stats.put("caches", cacheManager.getCacheNames());
        return stats;
    }
}
