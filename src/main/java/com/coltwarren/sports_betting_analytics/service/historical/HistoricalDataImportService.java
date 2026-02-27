package com.coltwarren.sports_betting_analytics.service.historical;

import com.coltwarren.sports_betting_analytics.model.historical.HistoricalMatch;
import com.coltwarren.sports_betting_analytics.repository.HistoricalMatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Historical Data Import Service
 *
 * Downloads and imports historical soccer betting data from Football-Data.co.uk.
 * Supports multiple leagues and seasons dating back to 2010.
 */
@Service
@Slf4j
public class HistoricalDataImportService {

    private final HistoricalMatchRepository matchRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public HistoricalDataImportService(HistoricalMatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    // Football-Data.co.uk URLs
    private static final String BASE_URL = "https://www.football-data.co.uk/mmz4281";

    // League codes
    private static final Map<String, String> LEAGUE_CODES = Map.of(
        "EPL", "E0",        // English Premier League
        "EFL_CHAMP", "E1",  // English Championship
        "LA_LIGA", "SP1",   // Spanish La Liga
        "BUNDESLIGA", "D1", // German Bundesliga
        "SERIE_A", "I1",    // Italian Serie A
        "LIGUE_1", "F1"     // French Ligue 1
    );

    // Season format: "2324" for 2023-24
    private static final List<String> SEASONS = List.of(
        "2425", "2324", "2223", "2122", "2021", "1920", "1819", "1718",
        "1617", "1516", "1415", "1314", "1213", "1112", "1011"
    );

    /**
     * Import data for a specific league and season
     */
    public int importLeagueSeason(String league, String seasonCode) {
        String leagueCode = LEAGUE_CODES.get(league);
        if (leagueCode == null) {
            log.error("Unknown league: {}", league);
            return 0;
        }

        // Check if already imported
        String formattedSeason = formatSeason(seasonCode);
        if (matchRepository.existsByLeagueAndSeason(league, formattedSeason)) {
            log.info("Data already exists for {} {}", league, formattedSeason);
            return 0;
        }

        String url = String.format("%s/%s/%s.csv", BASE_URL, seasonCode, leagueCode);
        log.info("Importing from: {}", url);

        try {
            String csvData = restTemplate.getForObject(url, String.class);
            if (csvData == null || csvData.isEmpty()) {
                log.warn("No data returned for {} {}", league, seasonCode);
                return 0;
            }

            List<HistoricalMatch> matches = parseCsvData(csvData, league, seasonCode);
            matchRepository.saveAll(matches);

            log.info("Imported {} matches for {} season {}", matches.size(), league, formattedSeason);
            return matches.size();

        } catch (Exception e) {
            log.error("Error importing {} {}: {}", league, seasonCode, e.getMessage());
            return 0;
        }
    }

    /**
     * Import all available data for a league
     */
    public Map<String, Integer> importAllSeasonsForLeague(String league) {
        Map<String, Integer> results = new LinkedHashMap<>();

        for (String season : SEASONS) {
            int count = importLeagueSeason(league, season);
            if (count > 0) {
                results.put(formatSeason(season), count);
            }
        }

        return results;
    }

    /**
     * Import all leagues, all seasons
     */
    public Map<String, Map<String, Integer>> importAllData() {
        Map<String, Map<String, Integer>> results = new LinkedHashMap<>();

        for (String league : LEAGUE_CODES.keySet()) {
            Map<String, Integer> leagueResults = importAllSeasonsForLeague(league);
            if (!leagueResults.isEmpty()) {
                results.put(league, leagueResults);
            }
        }

        return results;
    }

    private List<HistoricalMatch> parseCsvData(String csvData, String league, String seasonCode) {
        List<HistoricalMatch> matches = new ArrayList<>();
        String[] lines = csvData.split("\n");

        if (lines.length < 2) return matches;

        // Parse header to find column indices
        String[] headers = lines[0].split(",");
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i].trim(), i);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter altFormatter = DateTimeFormatter.ofPattern("dd/MM/yy");

        for (int i = 1; i < lines.length; i++) {
            try {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                String[] cols = parseCsvLine(line);
                if (cols.length < 10) continue;

                HistoricalMatch match = new HistoricalMatch();
                match.setLeague(league);
                match.setSeason(formatSeason(seasonCode));

                // Parse date
                String dateStr = getCol(cols, colIndex, "Date");
                if (dateStr != null && !dateStr.isEmpty()) {
                    try {
                        match.setMatchDate(LocalDate.parse(dateStr, formatter));
                    } catch (Exception e) {
                        try {
                            match.setMatchDate(LocalDate.parse(dateStr, altFormatter));
                        } catch (Exception e2) {
                            continue; // Skip if date can't be parsed
                        }
                    }
                } else {
                    continue;
                }

                match.setHomeTeam(getCol(cols, colIndex, "HomeTeam"));
                match.setAwayTeam(getCol(cols, colIndex, "AwayTeam"));

                if (match.getHomeTeam() == null || match.getAwayTeam() == null) continue;

                // Goals
                match.setHomeGoals(parseIntSafe(getCol(cols, colIndex, "FTHG")));
                match.setAwayGoals(parseIntSafe(getCol(cols, colIndex, "FTAG")));
                match.setResult(getCol(cols, colIndex, "FTR"));

                // Bet365 odds (most common)
                match.setB365Home(parseDoubleSafe(getCol(cols, colIndex, "B365H")));
                match.setB365Draw(parseDoubleSafe(getCol(cols, colIndex, "B365D")));
                match.setB365Away(parseDoubleSafe(getCol(cols, colIndex, "B365A")));

                // Use B365 as primary odds
                match.setHomeOdds(match.getB365Home());
                match.setDrawOdds(match.getB365Draw());
                match.setAwayOdds(match.getB365Away());

                // Over/Under 2.5
                match.setOver25Odds(parseDoubleSafe(getCol(cols, colIndex, "B365>2.5")));
                match.setUnder25Odds(parseDoubleSafe(getCol(cols, colIndex, "B365<2.5")));

                // Bwin as backup
                match.setBwinHome(parseDoubleSafe(getCol(cols, colIndex, "BWH")));
                match.setBwinDraw(parseDoubleSafe(getCol(cols, colIndex, "BWD")));
                match.setBwinAway(parseDoubleSafe(getCol(cols, colIndex, "BWA")));

                // Only add if we have essential data
                if (match.getHomeOdds() != null && match.getResult() != null) {
                    matches.add(match);
                }

            } catch (Exception e) {
                log.debug("Error parsing line {}: {}", i, e.getMessage());
            }
        }

        return matches;
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());

        return result.toArray(new String[0]);
    }

    private String getCol(String[] cols, Map<String, Integer> colIndex, String colName) {
        Integer idx = colIndex.get(colName);
        if (idx != null && idx < cols.length) {
            return cols[idx].trim();
        }
        return null;
    }

    private Integer parseIntSafe(String val) {
        if (val == null || val.isEmpty()) return null;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDoubleSafe(String val) {
        if (val == null || val.isEmpty()) return null;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatSeason(String code) {
        // "2324" -> "2023-24"
        if (code.length() == 4) {
            return "20" + code.substring(0, 2) + "-" + code.substring(2, 4);
        }
        return code;
    }

    /**
     * Get import statistics
     */
    public Map<String, Object> getImportStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long total = matchRepository.count();
        stats.put("totalMatches", total);

        List<String> leagues = matchRepository.findAllLeagues();
        stats.put("leagues", leagues);
        stats.put("leagueCount", leagues.size());

        Map<String, Long> byLeague = new LinkedHashMap<>();
        for (String league : leagues) {
            byLeague.put(league, matchRepository.countByLeague(league));
        }
        stats.put("matchesByLeague", byLeague);

        // Seasons available
        Map<String, List<String>> seasonsByLeague = new LinkedHashMap<>();
        for (String league : leagues) {
            seasonsByLeague.put(league, matchRepository.findSeasonsByLeague(league));
        }
        stats.put("seasonsByLeague", seasonsByLeague);

        return stats;
    }

    /**
     * Get available leagues
     */
    public Set<String> getAvailableLeagues() {
        return LEAGUE_CODES.keySet();
    }

    /**
     * Clear all data for a league
     */
    public int clearLeagueData(String league) {
        List<HistoricalMatch> matches = matchRepository.findByLeague(league);
        int count = matches.size();
        matchRepository.deleteAll(matches);
        log.info("Deleted {} matches for league {}", count, league);
        return count;
    }
}
