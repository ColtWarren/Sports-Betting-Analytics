package com.coltwarren.sports_betting_analytics.repository;

import com.coltwarren.sports_betting_analytics.model.StadiumLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Stadium Locations
 *
 * Used to lookup stadium coordinates for weather forecasts.
 */
@Repository
public interface StadiumLocationRepository extends JpaRepository<StadiumLocation, Long> {

    /**
     * Find stadium by exact name
     */
    Optional<StadiumLocation> findByStadiumName(String stadiumName);

    /**
     * Find stadium by partial name match (case insensitive)
     */
    @Query("SELECT s FROM StadiumLocation s WHERE LOWER(s.stadiumName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<StadiumLocation> findByStadiumNameContaining(@Param("name") String name);

    /**
     * Find first stadium by partial name match (case insensitive)
     */
    @Query("SELECT s FROM StadiumLocation s WHERE LOWER(s.stadiumName) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY s.id ASC")
    Optional<StadiumLocation> findFirstByStadiumNameContaining(@Param("name") String name);

    /**
     * Find stadium by team name
     */
    @Query("SELECT s FROM StadiumLocation s WHERE LOWER(s.homeTeams) LIKE LOWER(CONCAT('%', :teamName, '%'))")
    Optional<StadiumLocation> findByHomeTeamsContaining(@Param("teamName") String teamName);

    /**
     * Get all outdoor stadiums (weather-affected)
     */
    @Query("SELECT s FROM StadiumLocation s WHERE s.isDome = false")
    List<StadiumLocation> findAllOutdoorStadiums();

    /**
     * Get all stadiums for a specific sport
     */
    List<StadiumLocation> findBySport(String sport);

    /**
     * Get all outdoor stadiums for a specific sport
     */
    @Query("SELECT s FROM StadiumLocation s WHERE s.sport = :sport AND s.isDome = false")
    List<StadiumLocation> findOutdoorStadiumsBySport(@Param("sport") String sport);

    /**
     * Find stadium by city and state
     */
    Optional<StadiumLocation> findByCityAndState(String city, String state);
}
