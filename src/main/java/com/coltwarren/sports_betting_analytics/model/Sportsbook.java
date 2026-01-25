package com.coltwarren.sports_betting_analytics.model;

/**
 * Missouri Legal Sportsbook Model
 * Represents one of the 8 licensed Missouri sportsbooks
 */
public class Sportsbook {
    private String name;
    private double rating;
    private String bonus;
    private String url;
    private String description;
    private String emoji;
    private String badge;  // e.g., "Best Overall", "Best App"

    // Constructor
    public Sportsbook(String name, double rating, String bonus, String url,
                     String description, String emoji, String badge) {
        this.name = name;
        this.rating = rating;
        this.bonus = bonus;
        this.url = url;
        this.description = description;
        this.emoji = emoji;
        this.badge = badge;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getBonus() { return bonus; }
    public void setBonus(String bonus) { this.bonus = bonus; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    // Helper method to get star rating as integer (for template loop)
    public int getFullStars() {
        return (int) rating;
    }

    // Helper method to check if there's a half star
    public boolean hasHalfStar() {
        return rating % 1 >= 0.5;
    }
}
