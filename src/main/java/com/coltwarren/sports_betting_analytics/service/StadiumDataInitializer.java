package com.coltwarren.sports_betting_analytics.service;

import com.coltwarren.sports_betting_analytics.model.StadiumLocation;
import com.coltwarren.sports_betting_analytics.repository.StadiumLocationRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Stadium Data Initializer
 *
 * Populates the stadium_locations table with NFL, MLB, and MLS stadiums
 * for weather-based betting analysis.
 *
 * Runs on application startup if the table is empty.
 */
@Service
@Slf4j
public class StadiumDataInitializer {

    private final StadiumLocationRepository stadiumRepository;

    public StadiumDataInitializer(StadiumLocationRepository stadiumRepository) {
        this.stadiumRepository = stadiumRepository;
    }

    @PostConstruct
    @Transactional
    public void initializeStadiums() {
        if (stadiumRepository.count() > 0) {
            log.info("Stadium database already populated ({} stadiums)", stadiumRepository.count());
            return;
        }

        log.info("Initializing stadium database...");

        List<StadiumLocation> stadiums = Arrays.asList(
            // ═══════════════════════════════════════════════════════════════
            // NFL STADIUMS - OUTDOOR (Weather Affected)
            // ═══════════════════════════════════════════════════════════════
            createStadium("Arrowhead Stadium", 39.0489, -94.4839, false, false,
                "Kansas City", "MO", "Kansas City Chiefs", "America/Chicago", "NFL"),

            createStadium("Lambeau Field", 44.5013, -88.0622, false, false,
                "Green Bay", "WI", "Green Bay Packers", "America/Chicago", "NFL"),

            createStadium("Soldier Field", 41.8623, -87.6167, false, false,
                "Chicago", "IL", "Chicago Bears", "America/Chicago", "NFL"),

            createStadium("Highmark Stadium", 42.7738, -78.7870, false, false,
                "Orchard Park", "NY", "Buffalo Bills", "America/New_York", "NFL"),

            createStadium("Gillette Stadium", 42.0909, -71.2643, false, false,
                "Foxborough", "MA", "New England Patriots", "America/New_York", "NFL"),

            createStadium("MetLife Stadium", 40.8128, -74.0742, false, false,
                "East Rutherford", "NJ", "New York Giants,New York Jets", "America/New_York", "NFL"),

            createStadium("Acrisure Stadium", 40.4468, -80.0158, false, false,
                "Pittsburgh", "PA", "Pittsburgh Steelers", "America/New_York", "NFL"),

            createStadium("M&T Bank Stadium", 39.2780, -76.6227, false, false,
                "Baltimore", "MD", "Baltimore Ravens", "America/New_York", "NFL"),

            createStadium("FirstEnergy Stadium", 41.5061, -81.6995, false, false,
                "Cleveland", "OH", "Cleveland Browns", "America/New_York", "NFL"),

            createStadium("Lincoln Financial Field", 39.9008, -75.1675, false, false,
                "Philadelphia", "PA", "Philadelphia Eagles", "America/New_York", "NFL"),

            createStadium("FedExField", 38.9076, -76.8645, false, false,
                "Landover", "MD", "Washington Commanders", "America/New_York", "NFL"),

            createStadium("Bank of America Stadium", 35.2258, -80.8528, false, false,
                "Charlotte", "NC", "Carolina Panthers", "America/New_York", "NFL"),

            createStadium("Empower Field at Mile High", 39.7439, -105.0201, false, false,
                "Denver", "CO", "Denver Broncos", "America/Denver", "NFL"),

            createStadium("Levi's Stadium", 37.4032, -121.9698, false, false,
                "Santa Clara", "CA", "San Francisco 49ers", "America/Los_Angeles", "NFL"),

            createStadium("Paycor Stadium", 39.0955, -84.5161, false, false,
                "Cincinnati", "OH", "Cincinnati Bengals", "America/New_York", "NFL"),

            createStadium("GEHA Field at Arrowhead Stadium", 39.0490, -94.4839, false, false,
                "Kansas City", "MO", "Kansas City Chiefs", "America/Chicago", "NFL"),

            createStadium("Nissan Stadium", 36.1665, -86.7713, false, false,
                "Nashville", "TN", "Tennessee Titans", "America/Chicago", "NFL"),

            createStadium("EverBank Stadium", 30.3239, -81.6373, false, false,
                "Jacksonville", "FL", "Jacksonville Jaguars", "America/New_York", "NFL"),

            createStadium("Raymond James Stadium", 27.9759, -82.5033, false, false,
                "Tampa", "FL", "Tampa Bay Buccaneers", "America/New_York", "NFL"),

            createStadium("Hard Rock Stadium", 25.9580, -80.2389, false, false,
                "Miami Gardens", "FL", "Miami Dolphins", "America/New_York", "NFL"),

            // ═══════════════════════════════════════════════════════════════
            // NFL STADIUMS - DOMES (Weather NOT Affected)
            // ═══════════════════════════════════════════════════════════════
            createStadium("SoFi Stadium", 33.9535, -118.3392, false, true,
                "Inglewood", "CA", "Los Angeles Rams,Los Angeles Chargers", "America/Los_Angeles", "NFL"),

            createStadium("AT&T Stadium", 32.7473, -97.0945, true, true,
                "Arlington", "TX", "Dallas Cowboys", "America/Chicago", "NFL"),

            createStadium("U.S. Bank Stadium", 44.9738, -93.2575, true, false,
                "Minneapolis", "MN", "Minnesota Vikings", "America/Chicago", "NFL"),

            createStadium("Mercedes-Benz Stadium", 33.7553, -84.4006, true, true,
                "Atlanta", "GA", "Atlanta Falcons", "America/New_York", "NFL"),

            createStadium("Caesars Superdome", 29.9511, -90.0812, true, false,
                "New Orleans", "LA", "New Orleans Saints", "America/Chicago", "NFL"),

            createStadium("Lucas Oil Stadium", 39.7601, -86.1639, true, true,
                "Indianapolis", "IN", "Indianapolis Colts", "America/New_York", "NFL"),

            createStadium("State Farm Stadium", 33.5276, -112.2626, true, true,
                "Glendale", "AZ", "Arizona Cardinals", "America/Phoenix", "NFL"),

            createStadium("Allegiant Stadium", 36.0909, -115.1833, true, false,
                "Las Vegas", "NV", "Las Vegas Raiders", "America/Los_Angeles", "NFL"),

            createStadium("Ford Field", 42.3400, -83.0456, true, false,
                "Detroit", "MI", "Detroit Lions", "America/New_York", "NFL"),

            createStadium("NRG Stadium", 29.6847, -95.4107, true, true,
                "Houston", "TX", "Houston Texans", "America/Chicago", "NFL"),

            // ═══════════════════════════════════════════════════════════════
            // MLB STADIUMS - OUTDOOR (Weather Affected)
            // ═══════════════════════════════════════════════════════════════
            createStadium("Wrigley Field", 41.9484, -87.6553, false, false,
                "Chicago", "IL", "Chicago Cubs", "America/Chicago", "MLB"),

            createStadium("Fenway Park", 42.3467, -71.0972, false, false,
                "Boston", "MA", "Boston Red Sox", "America/New_York", "MLB"),

            createStadium("Dodger Stadium", 34.0739, -118.2400, false, false,
                "Los Angeles", "CA", "Los Angeles Dodgers", "America/Los_Angeles", "MLB"),

            createStadium("Coors Field", 39.7559, -104.9942, false, false,
                "Denver", "CO", "Colorado Rockies", "America/Denver", "MLB"),

            createStadium("Busch Stadium", 38.6226, -90.1928, false, false,
                "St. Louis", "MO", "St. Louis Cardinals", "America/Chicago", "MLB"),

            createStadium("Kauffman Stadium", 39.0517, -94.4803, false, false,
                "Kansas City", "MO", "Kansas City Royals", "America/Chicago", "MLB"),

            createStadium("Progressive Field", 41.4962, -81.6852, false, false,
                "Cleveland", "OH", "Cleveland Guardians", "America/New_York", "MLB"),

            createStadium("Oriole Park at Camden Yards", 39.2839, -76.6218, false, false,
                "Baltimore", "MD", "Baltimore Orioles", "America/New_York", "MLB"),

            createStadium("Yankee Stadium", 40.8296, -73.9262, false, false,
                "Bronx", "NY", "New York Yankees", "America/New_York", "MLB"),

            createStadium("Citizens Bank Park", 39.9061, -75.1665, false, false,
                "Philadelphia", "PA", "Philadelphia Phillies", "America/New_York", "MLB"),

            createStadium("Citi Field", 40.7571, -73.8458, false, false,
                "Queens", "NY", "New York Mets", "America/New_York", "MLB"),

            createStadium("Nationals Park", 38.8730, -77.0074, false, false,
                "Washington", "DC", "Washington Nationals", "America/New_York", "MLB"),

            createStadium("PNC Park", 40.4469, -80.0057, false, false,
                "Pittsburgh", "PA", "Pittsburgh Pirates", "America/New_York", "MLB"),

            createStadium("Great American Ball Park", 39.0979, -84.5082, false, false,
                "Cincinnati", "OH", "Cincinnati Reds", "America/New_York", "MLB"),

            createStadium("Guaranteed Rate Field", 41.8299, -87.6338, false, false,
                "Chicago", "IL", "Chicago White Sox", "America/Chicago", "MLB"),

            createStadium("Target Field", 44.9817, -93.2776, false, false,
                "Minneapolis", "MN", "Minnesota Twins", "America/Chicago", "MLB"),

            createStadium("Comerica Park", 42.3390, -83.0485, false, false,
                "Detroit", "MI", "Detroit Tigers", "America/New_York", "MLB"),

            createStadium("Angel Stadium", 33.8003, -117.8827, false, false,
                "Anaheim", "CA", "Los Angeles Angels", "America/Los_Angeles", "MLB"),

            createStadium("Oracle Park", 37.7786, -122.3893, false, false,
                "San Francisco", "CA", "San Francisco Giants", "America/Los_Angeles", "MLB"),

            createStadium("Petco Park", 32.7076, -117.1570, false, false,
                "San Diego", "CA", "San Diego Padres", "America/Los_Angeles", "MLB"),

            // ═══════════════════════════════════════════════════════════════
            // MLS STADIUMS (Missouri Teams + Major Outdoor)
            // ═══════════════════════════════════════════════════════════════
            createStadium("CITYPARK", 38.6310, -90.2090, false, false,
                "St. Louis", "MO", "St. Louis City SC", "America/Chicago", "MLS"),

            createStadium("Children's Mercy Park", 39.1218, -94.8231, false, false,
                "Kansas City", "KS", "Sporting Kansas City,Sporting KC", "America/Chicago", "MLS"),

            createStadium("Dignity Health Sports Park", 33.8644, -118.2611, false, false,
                "Carson", "CA", "LA Galaxy", "America/Los_Angeles", "MLS"),

            createStadium("Providence Park", 45.5216, -122.6917, false, false,
                "Portland", "OR", "Portland Timbers", "America/Los_Angeles", "MLS"),

            createStadium("Lumen Field", 47.5952, -122.3316, false, false,
                "Seattle", "WA", "Seattle Sounders FC", "America/Los_Angeles", "MLS"),

            createStadium("Red Bull Arena", 40.7368, -74.1503, false, false,
                "Harrison", "NJ", "New York Red Bulls", "America/New_York", "MLS"),

            createStadium("Audi Field", 38.8686, -77.0129, false, false,
                "Washington", "DC", "D.C. United", "America/New_York", "MLS"),

            createStadium("Lower.com Field", 39.9689, -82.9828, false, false,
                "Columbus", "OH", "Columbus Crew", "America/New_York", "MLS")
        );

        stadiumRepository.saveAll(stadiums);
        log.info("Initialized {} stadiums", stadiums.size());

        // Log summary by sport
        long nflCount = stadiums.stream().filter(s -> "NFL".equals(s.getSport())).count();
        long mlbCount = stadiums.stream().filter(s -> "MLB".equals(s.getSport())).count();
        long mlsCount = stadiums.stream().filter(s -> "MLS".equals(s.getSport())).count();
        long outdoorCount = stadiums.stream().filter(s -> !s.getIsDome()).count();

        log.info("Stadium breakdown: {} NFL, {} MLB, {} MLS ({} outdoor)",
                 nflCount, mlbCount, mlsCount, outdoorCount);
    }

    private StadiumLocation createStadium(String name, Double lat, Double lon,
            Boolean isDome, Boolean hasRetractableRoof,
            String city, String state, String homeTeams,
            String timezone, String sport) {

        StadiumLocation stadium = new StadiumLocation();
        stadium.setStadiumName(name);
        stadium.setLatitude(lat);
        stadium.setLongitude(lon);
        stadium.setIsDome(isDome);
        stadium.setHasRetractableRoof(hasRetractableRoof);
        stadium.setCity(city);
        stadium.setState(state);
        stadium.setHomeTeams(homeTeams);
        stadium.setTimezone(timezone);
        stadium.setSport(sport);
        return stadium;
    }
}
