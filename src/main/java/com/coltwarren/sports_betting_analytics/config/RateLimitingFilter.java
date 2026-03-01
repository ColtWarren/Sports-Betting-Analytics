package com.coltwarren.sports_betting_analytics.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;

    private final int apiLimit;
    private final int aiLimit;

    private final ConcurrentHashMap<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            @Value("${rate-limit.api.requests-per-minute:60}") int apiLimit,
            @Value("${rate-limit.ai.requests-per-minute:10}") int aiLimit) {
        this.apiLimit = apiLimit;
        this.aiLimit = aiLimit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/")
                || path.startsWith("/api/health")
                || path.equals("/login")
                || path.startsWith("/login/")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit = isAiEndpoint(path) ? aiLimit : apiLimit;
        String key = resolveKey(request, path);
        long now = System.currentTimeMillis();

        Deque<Long> timestamps = requestLog.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        evictOld(timestamps, now);

        if (timestamps.size() >= limit) {
            long oldestInWindow = timestamps.peekFirst();
            long retryAfterSeconds = Math.max(1, (oldestInWindow + WINDOW_MS - now + 999) / 1000);

            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.getWriter().write(
                    "{\"error\":\"Too many requests\",\"retryAfterSeconds\":" + retryAfterSeconds + "}");
            return;
        }

        timestamps.addLast(now);
        filterChain.doFilter(request, response);
    }

    private boolean isAiEndpoint(String path) {
        return path.startsWith("/api/matchup-analysis")
                || path.startsWith("/api/recommendations")
                || path.startsWith("/api/ai");
    }

    private String resolveKey(HttpServletRequest request, String path) {
        String tier = isAiEndpoint(path) ? "ai" : "api";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identity;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            identity = auth.getName();
        } else {
            identity = request.getRemoteAddr();
        }
        return tier + ":" + identity;
    }

    private void evictOld(Deque<Long> timestamps, long now) {
        long cutoff = now - WINDOW_MS;
        Iterator<Long> it = timestamps.iterator();
        while (it.hasNext()) {
            if (it.next() < cutoff) {
                it.remove();
            } else {
                break;
            }
        }
    }

    @Scheduled(fixedRate = 300_000)
    void cleanupStaleEntries() {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        requestLog.entrySet().removeIf(entry -> {
            Deque<Long> timestamps = entry.getValue();
            evictOld(timestamps, System.currentTimeMillis());
            return timestamps.isEmpty();
        });
    }
}
