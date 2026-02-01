package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.CacheEvictionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Health Controller
 *
 * Provides health check endpoints for monitoring and orchestration.
 * Includes liveness, readiness, and detailed system information.
 */
@RestController
@RequestMapping("/api/health")
@Slf4j
public class HealthController {

    @Autowired
    private CacheEvictionService cacheEvictionService;

    @Autowired(required = false)
    private BuildProperties buildProperties;

    private final LocalDateTime startTime = LocalDateTime.now();

    /**
     * Basic health check - Kubernetes liveness probe
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Readiness check - Kubernetes readiness probe
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        // Check if all required services are available
        boolean isReady = true;
        List<String> issues = new ArrayList<>();

        // Add readiness checks here if needed
        // e.g., database connection, external API availability

        if (isReady) {
            return ResponseEntity.ok(Map.of(
                "status", "READY",
                "timestamp", LocalDateTime.now().toString()
            ));
        } else {
            return ResponseEntity.status(503).body(Map.of(
                "status", "NOT_READY",
                "issues", issues,
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }

    /**
     * Comprehensive health check
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();

        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("application", getApplicationInfo());
        health.put("apis", getApiStatus());
        health.put("system", getSystemInfo());
        health.put("memory", getMemoryInfo());
        health.put("cache", cacheEvictionService.getCacheStats());
        health.put("uptime", getUptime());

        return ResponseEntity.ok(health);
    }

    /**
     * Get application version info
     */
    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> version() {
        Map<String, Object> version = new LinkedHashMap<>();

        version.put("name", "Sports Betting Analytics");
        version.put("version", buildProperties != null ? buildProperties.getVersion() : "2.7.0");
        version.put("features", List.of(
            "Multi-Factor Recommendation Engine",
            "xG Soccer Analytics",
            "Pattern Recognition",
            "Historical Backtesting",
            "Injury Tracking",
            "Public Betting Analysis",
            "Weather Intelligence"
        ));

        return ResponseEntity.ok(version);
    }

    /**
     * Clear all caches
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        cacheEvictionService.evictAllCaches();
        log.info("All caches cleared via API");
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "All caches cleared",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Get cache statistics
     */
    @GetMapping("/cache")
    public ResponseEntity<Map<String, Object>> cacheStats() {
        return ResponseEntity.ok(cacheEvictionService.getCacheStats());
    }

    /**
     * Get detailed application info
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> appInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", "Sports Betting Analytics Platform");
        info.put("version", buildProperties != null ? buildProperties.getVersion() : "2.7.0");
        info.put("features", List.of(
            "Weather Intelligence",
            "Public Betting / Contrarian Analysis",
            "Injury Tracking (ESPN)",
            "Historical Backtesting",
            "Pattern Recognition",
            "Multi-Factor Recommendations",
            "xG Soccer Analytics"
        ));
        info.put("sports", List.of("NFL", "CFB", "NBA", "CBB", "NHL", "MLB", "Soccer"));
        info.put("sportsbooks", List.of(
            "DraftKings", "FanDuel", "BetMGM", "Caesars",
            "Fanatics", "ESPN BET", "Hard Rock", "bet365"
        ));
        info.put("apis", getApiStatus());
        info.put("futureIntegrations", List.of(
            "CollegeFootballData.com - CFB advanced stats",
            "CollegeBasketballData.com - CBB advanced stats"
        ));
        return ResponseEntity.ok(info);
    }

    private Map<String, Object> getApplicationInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", "Sports Betting Analytics");
        info.put("version", buildProperties != null ? buildProperties.getVersion() : "2.7.0");
        info.put("startTime", startTime.toString());
        return info;
    }

    private Map<String, String> getApiStatus() {
        Map<String, String> apis = new LinkedHashMap<>();
        apis.put("theOddsApi", checkEnv("ODDS_API_KEY") ? "CONFIGURED" : "MISSING");
        apis.put("openWeatherMap", checkEnv("OPENWEATHERMAP_API_KEY") ? "CONFIGURED" : "MISSING");
        apis.put("claudeAi", checkEnv("CLAUDE_API_KEY") ? "CONFIGURED" : "MISSING");
        apis.put("espn", "FREE (no key needed)");
        apis.put("collegeFootballData", checkEnv("CFB_API_KEY") ? "CONFIGURED" : "NOT_CONFIGURED");
        apis.put("collegeBasketballData", checkEnv("CBB_API_KEY") ? "CONFIGURED" : "NOT_CONFIGURED");
        return apis;
    }

    private boolean checkEnv(String key) {
        String value = System.getenv(key);
        return value != null && !value.isEmpty();
    }

    private Map<String, Object> getSystemInfo() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));
        info.put("osName", System.getProperty("os.name"));
        info.put("osArch", System.getProperty("os.arch"));
        info.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        return info;
    }

    private Map<String, Object> getMemoryInfo() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        Runtime runtime = Runtime.getRuntime();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("heapUsed", formatBytes(runtime.totalMemory() - runtime.freeMemory()));
        info.put("heapMax", formatBytes(runtime.maxMemory()));
        info.put("heapFree", formatBytes(runtime.freeMemory()));
        info.put("heapUsagePercent", String.format("%.1f%%",
            (double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100));
        return info;
    }

    private String getUptime() {
        Duration uptime = Duration.between(startTime, LocalDateTime.now());
        long days = uptime.toDays();
        long hours = uptime.toHours() % 24;
        long minutes = uptime.toMinutes() % 60;
        long seconds = uptime.getSeconds() % 60;

        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
