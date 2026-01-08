package com.coltwarren.sports_betting_analytics.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {
    
    // Store active notification subscribers (in production, use Redis or database)
    private final Map<String, NotificationSubscriber> subscribers = new ConcurrentHashMap<>();
    
    public static class NotificationSubscriber {
        private String sessionId;
        private boolean enableBestBets;
        private boolean enableHighValue;
        private double minValue;
        
        public NotificationSubscriber(String sessionId) {
            this.sessionId = sessionId;
            this.enableBestBets = true;
            this.enableHighValue = true;
            this.minValue = 100.0; // Minimum 100 pts value
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public boolean isEnableBestBets() { return enableBestBets; }
        public boolean isEnableHighValue() { return enableHighValue; }
        public double getMinValue() { return minValue; }
        
        public void setEnableBestBets(boolean enable) { this.enableBestBets = enable; }
        public void setEnableHighValue(boolean enable) { this.enableHighValue = enable; }
        public void setMinValue(double value) { this.minValue = value; }
    }
    
    public static class Notification {
        private String title;
        private String message;
        private String type; // "best-bet", "high-value", "bankroll", "clv"
        private Map<String, Object> data;
        private long timestamp;
        
        public Notification(String title, String message, String type) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.data = new HashMap<>();
            this.timestamp = System.currentTimeMillis();
        }
        
        public void addData(String key, Object value) {
            this.data.put(key, value);
        }
        
        // Getters
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getType() { return type; }
        public Map<String, Object> getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Subscribe to notifications
     */
    public void subscribe(String sessionId) {
        subscribers.putIfAbsent(sessionId, new NotificationSubscriber(sessionId));
    }
    
    /**
     * Unsubscribe from notifications
     */
    public void unsubscribe(String sessionId) {
        subscribers.remove(sessionId);
    }
    
    /**
     * Update notification preferences
     */
    public void updatePreferences(String sessionId, boolean enableBestBets, 
                                  boolean enableHighValue, double minValue) {
        NotificationSubscriber subscriber = subscribers.get(sessionId);
        if (subscriber != null) {
            subscriber.setEnableBestBets(enableBestBets);
            subscriber.setEnableHighValue(enableHighValue);
            subscriber.setMinValue(minValue);
        }
    }
    
    /**
     * Create notification for best bet opportunity
     */
    public Notification createBestBetNotification(String game, String selection, 
                                                  int odds, double value, String book) {
        Notification notification = new Notification(
            "🔥 Best Bet Alert!",
            game + " - " + selection + " @ " + (odds > 0 ? "+" : "") + odds,
            "best-bet"
        );
        
        notification.addData("game", game);
        notification.addData("selection", selection);
        notification.addData("odds", odds);
        notification.addData("value", value);
        notification.addData("book", book);
        
        return notification;
    }
    
    /**
     * Create notification for high value opportunity
     */
    public Notification createHighValueNotification(double value, String game) {
        Notification notification = new Notification(
            "💎 High Value Alert!",
            Math.round(value) + " pts value on " + game,
            "high-value"
        );
        
        notification.addData("value", value);
        notification.addData("game", game);
        
        return notification;
    }
    
    /**
     * Create notification for bankroll milestone
     */
    public Notification createBankrollNotification(String milestone, double amount) {
        Notification notification = new Notification(
            "💰 Bankroll Milestone!",
            milestone + ": $" + String.format("%.2f", amount),
            "bankroll"
        );
        
        notification.addData("milestone", milestone);
        notification.addData("amount", amount);
        
        return notification;
    }
    
    /**
     * Create notification for positive CLV
     */
    public Notification createCLVNotification(String betDescription, double clv) {
        Notification notification = new Notification(
            "📈 Great Line!",
            "You beat closing line by " + String.format("%.2f", clv) + "% on " + betDescription,
            "clv"
        );
        
        notification.addData("clv", clv);
        notification.addData("bet", betDescription);
        
        return notification;
    }
    
    /**
     * Get active subscribers count
     */
    public int getSubscriberCount() {
        return subscribers.size();
    }
    
    /**
     * Check if user should receive notification
     */
    public boolean shouldNotify(String sessionId, String type, double value) {
        NotificationSubscriber subscriber = subscribers.get(sessionId);
        if (subscriber == null) {
            return false;
        }
        
        if ("best-bet".equals(type) && !subscriber.isEnableBestBets()) {
            return false;
        }
        
        if ("high-value".equals(type) && !subscriber.isEnableHighValue()) {
            return false;
        }
        
        return value >= subscriber.getMinValue();
    }
}
