package com.coltwarren.sports_betting_analytics.controller;

import com.coltwarren.sports_betting_analytics.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @PostMapping("/subscribe")
    public Map<String, Object> subscribe() {
        String sessionId = UUID.randomUUID().toString();
        notificationService.subscribe(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("subscribed", true);
        response.put("message", "Successfully subscribed to notifications");
        
        return response;
    }
    
    @PostMapping("/unsubscribe")
    public Map<String, Object> unsubscribe(@RequestParam String sessionId) {
        notificationService.unsubscribe(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("subscribed", false);
        response.put("message", "Successfully unsubscribed from notifications");
        
        return response;
    }
    
    @PostMapping("/preferences")
    public Map<String, Object> updatePreferences(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "true") boolean enableBestBets,
            @RequestParam(defaultValue = "true") boolean enableHighValue,
            @RequestParam(defaultValue = "100") double minValue) {
        
        notificationService.updatePreferences(sessionId, enableBestBets, enableHighValue, minValue);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("preferences", Map.of(
            "enableBestBets", enableBestBets,
            "enableHighValue", enableHighValue,
            "minValue", minValue
        ));
        
        return response;
    }
    
    @GetMapping("/test")
    public Map<String, Object> testNotification(@RequestParam String sessionId) {
        NotificationService.Notification notification = notificationService.createBestBetNotification(
            "Chiefs vs Bills",
            "Chiefs -3",
            -110,
            235.0,
            "DraftKings"
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("notification", notification);
        response.put("message", "Test notification created");
        
        return response;
    }
    
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeSubscribers", notificationService.getSubscriberCount());
        stats.put("status", "active");
        
        return stats;
    }
}
