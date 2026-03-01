package com.coltwarren.sports_betting_analytics.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Stadium Location Entity
 *
 * Stores stadium coordinates for weather lookups.
 * Used to determine if weather impacts game outcomes.
 *
 * Key fields:
 * - isDome: If true, weather has no impact
 * - hasRetractableRoof: May or may not be closed depending on conditions
 * - homeTeams: Comma-separated list (some stadiums host multiple teams)
 */
@Entity
@Table(name = "stadium_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StadiumLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String stadiumName;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Boolean isDome; // true = indoor, ignore weather

    @Column(nullable = false)
    private Boolean hasRetractableRoof; // May be open or closed

    private String city;

    private String state;

    @Column(name = "home_teams")
    private String homeTeams; // Comma-separated: "New York Giants,New York Jets"

    private String timezone; // e.g., "America/Chicago"

    @Column(nullable = false)
    private String sport; // NFL, MLB, MLS, etc.

    /**
     * Check if this stadium is affected by weather
     * Domes are never affected, retractable roofs depend on whether it's closed
     */
    public boolean isWeatherAffected() {
        return !isDome;
    }

    /**
     * Check if a specific team plays at this stadium
     */
    public boolean hasTeam(String teamName) {
        if (homeTeams == null) return false;
        return homeTeams.toLowerCase().contains(teamName.toLowerCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StadiumLocation that = (StadiumLocation) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
