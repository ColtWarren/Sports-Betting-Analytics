# CFBD/CBBD API Integration - Implementation Plan

**Date:** January 20, 2026
**API Key:** HiNBYl/WBC0BLmmaGfXWz35rYqFBFpn0Nbz6g6vH8TC5nj8imYp2WwreJhewXY8Y
**API Docs:** https://collegefootballdata.com/api/docs

---

## ✅ PART 1: KELLY CRITERION FIX VERIFICATION

### STATUS: **ALREADY FIXED** ✅

**File:** `best-bets.html`
**Line 548:**
```html
💰 Kelly: <span class="kelly-percent">${formatKelly(bet.kellyPercent)}%</span> of bankroll
```

**Line 678-684 (formatKelly function):**
```javascript
function formatKelly(kellyPercent) {
    if (kellyPercent == null || kellyPercent === 0) {
        return '0.0';
    }
    // Service already returns percentage (0-100), so don't multiply by 100
    return kellyPercent.toFixed(1);
}
```

**✅ CONFIRMED:**
- No `* 100` multiplication in display code
- Comment explicitly states service returns percentage already
- Kelly will display correctly as 2-8% instead of 200-800%

**NO ACTION REQUIRED** - Fix is already in place!

---

## 📋 PART 2: CFBD API KEY INSTALLATION

### Step 1: Update application.properties

Add these lines to `/Users/coltwarren/projects/sports-betting-analytics/src/main/resources/application.properties`

**Add after line 42 (after weather.api.key):**

```properties
# ============================================
# COLLEGE FOOTBALL/BASKETBALL DATA API
# ============================================
cfbd.api.key=${CFBD_API_KEY}
cfbd.api.url=https://api.collegefootballdata.com
```

### Step 2: Set Environment Variable

**For current terminal session:**
```bash
export CFBD_API_KEY=HiNBYl/WBC0BLmmaGfXWz35rYqFBFpn0Nbz6g6vH8TC5nj8imYp2WwreJhewXY8Y
```

**For permanent setup (add to ~/.zshrc or ~/.bash_profile):**
```bash
echo 'export CFBD_API_KEY=HiNBYl/WBC0BLmmaGfXWz35rYqFBFpn0Nbz6g6vH8TC5nj8imYp2WwreJhewXY8Y' >> ~/.zshrc
source ~/.zshrc
```

### Step 3: Full Startup Command (with all APIs)

```bash
export CLAUDE_API_KEY=YOUR_CLAUDE_API_KEY_HERE && \
export ODDS_API_KEY=6f9a804009739bb562afa482297057e7 && \
export WEATHER_API_KEY=313f9507472c126b8ce6fcf79f121f8f && \
export CFBD_API_KEY=HiNBYl/WBC0BLmmaGfXWz35rYqFBFpn0Nbz6g6vH8TC5nj8imYp2WwreJhewXY8Y && \
./mvnw spring-boot:run
```

---

## 🏗️ PART 3: CFBD SERVICE ARCHITECTURE

### Current API Configuration Pattern

**Example from application.properties:**
```properties
# Claude AI
claude.api.key=${CLAUDE_API_KEY}
claude.api.url=https://api.anthropic.com/v1/messages
claude.model=claude-sonnet-4-20250514

# Odds API
odds.api.key=${ODDS_API_KEY}
odds.api.url=https://api.the-odds-api.com/v4

# Weather API
weather.api.key=313f9507472c126b8ce6fcf79f121f8f

# CFBD API (NEW)
cfbd.api.key=${CFBD_API_KEY}
cfbd.api.url=https://api.collegefootballdata.com
```

### Service Class Structure

**New Service:** `CFBDataService.java`
**Location:** `/src/main/java/com/coltwarren/sports_betting_analytics/service/college/CFBDataService.java`

---

## 🎯 PART 4: CFBD API ENDPOINTS

The College Football Data API provides extensive data for both CFB and CBB.

### Available Endpoints (CFB)

**Games:**
- `GET /games` - Game information
- `GET /games/teams` - Games by team
- `GET /calendar` - Season calendar
- `GET /scoreboard` - Live scores

**Teams:**
- `GET /teams` - All teams
- `GET /teams/fbs` - FBS teams only
- `GET /roster` - Team rosters
- `GET /talent` - Team talent composites

**Stats:**
- `GET /stats/season` - Season stats
- `GET /stats/player/season` - Player stats
- `GET /stats/season/advanced` - Advanced stats
- `GET /stats/game/advanced` - Game-level advanced stats

**Rankings:**
- `GET /rankings` - AP Poll and Coaches Poll
- `GET /rankings/weeks` - Rankings by week

**Betting:**
- `GET /lines` - Historical betting lines
- `GET /metrics/sp` - SP+ ratings
- `GET /ratings/sp` - SP+ defensive/offensive ratings

**Weather:**
- `GET /games/weather` - Game weather data

### Query Parameters

**Common parameters:**
- `year` (required for most endpoints)
- `week` (optional, for weekly data)
- `team` (optional, filter by team)
- `seasonType` (regular, postseason, both)
- `division` (fbs, fcs, ii, iii)

### Authentication

**Header:**
```
Authorization: Bearer HiNBYl/WBC0BLmmaGfXWz35rYqFBFpn0Nbz6g6vH8TC5nj8imYp2WwreJhewXY8Y
```

---

## 📐 PART 5: IMPLEMENTATION PLAN

### Phase 1: Create CFBDataService (Foundation)

**File:** `src/main/java/com/coltwarren/sports_betting_analytics/service/college/CFBDataService.java`

**Features:**
- API configuration injection
- WebClient setup with authentication
- Basic HTTP GET methods
- Error handling

**Methods to implement:**
1. `getGames(int year, int week, String team)` - Fetch games
2. `getTeamStats(int year, String team)` - Get team statistics
3. `getRankings(int year, int week)` - Get AP/Coaches polls
4. `getSPRatings(int year)` - Get SP+ ratings for teams
5. `getGameLines(int year, int week, String team)` - Historical betting lines
6. `getGameWeather(int gameId)` - Weather for specific game

### Phase 2: Integration with MultiSportBestBetsService

**Update:** `MultiSportBestBetsService.java`

**Changes needed:**
1. Autowire `CFBDataService`
2. Enhance `analyzeSport("CFB")` method to use CFBD data
3. Enhance `analyzeSport("CBB")` method to use CFBD data
4. Incorporate SP+ ratings into confidence calculations
5. Use team stats for better AI analysis prompts

**Example enhancement:**
```java
private List<Map<String, Object>> analyzeSport(String sport) {
    if (sport.equals("CFB") || sport.equals("CBB")) {
        // Get CFBD data for richer analysis
        List<Map<String, Object>> games = cfbDataService.getGames(2026, getCurrentWeek(), null);
        Map<String, Double> spRatings = cfbDataService.getSPRatings(2026);

        // Use SP+ ratings to enhance win probability estimates
        // More accurate Kelly calculations
    }

    // ... rest of existing logic
}
```

### Phase 3: Create College-Specific Analysis

**New Service:** `CollegeMatchupAnalyzer.java`

**Features:**
- SP+ rating differential analysis
- Roster talent composite comparison
- Historical head-to-head records
- Home field advantage calculations
- Weather impact for outdoor games

### Phase 4: Enhanced Kelly Criterion for College

**Use CFBD data to improve Kelly calculations:**

1. **SP+ Ratings** → More accurate win probability
2. **Team Talent** → Adjust confidence scores
3. **Historical Lines** → Calculate CLV potential
4. **Advanced Stats** → Refine edge detection

---

## 🔧 PART 6: CODE TEMPLATES

### CFBDataService.java Template

```java
package com.coltwarren.sports_betting_analytics.service.college;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class CFBDataService {

    @Value("${cfbd.api.key}")
    private String apiKey;

    @Value("${cfbd.api.url}")
    private String apiUrl;

    private final WebClient webClient;

    public CFBDataService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Get games for a specific year, week, and optionally team
     */
    public List<Map<String, Object>> getGames(int year, Integer week, String team) {
        try {
            String url = apiUrl + "/games?year=" + year;

            if (week != null) {
                url += "&week=" + week;
            }

            if (team != null) {
                url += "&team=" + team;
            }

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            return response != null ? Arrays.asList(response) : Collections.emptyList();

        } catch (Exception e) {
            System.err.println("Error fetching CFBD games: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get SP+ ratings for all teams in a given year
     */
    public Map<String, Double> getSPRatings(int year) {
        try {
            String url = apiUrl + "/ratings/sp?year=" + year;

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            Map<String, Double> ratings = new HashMap<>();

            if (response != null) {
                for (Map<String, Object> team : response) {
                    String teamName = (String) team.get("team");
                    Double rating = ((Number) team.get("rating")).doubleValue();
                    ratings.put(teamName, rating);
                }
            }

            return ratings;

        } catch (Exception e) {
            System.err.println("Error fetching SP+ ratings: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Get current AP/Coaches rankings
     */
    public List<Map<String, Object>> getRankings(int year, int week) {
        try {
            String url = apiUrl + "/rankings?year=" + year + "&week=" + week;

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            return response != null ? Arrays.asList(response) : Collections.emptyList();

        } catch (Exception e) {
            System.err.println("Error fetching rankings: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get team statistics for a season
     */
    public Map<String, Object> getTeamStats(int year, String team) {
        try {
            String url = apiUrl + "/stats/season?year=" + year + "&team=" + team;

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            return response != null && response.length > 0 ? response[0] : Collections.emptyMap();

        } catch (Exception e) {
            System.err.println("Error fetching team stats: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Get historical betting lines for games
     */
    public List<Map<String, Object>> getGameLines(int year, Integer week, String team) {
        try {
            String url = apiUrl + "/lines?year=" + year;

            if (week != null) {
                url += "&week=" + week;
            }

            if (team != null) {
                url += "&team=" + team;
            }

            Map<String, Object>[] response = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

            return response != null ? Arrays.asList(response) : Collections.emptyList();

        } catch (Exception e) {
            System.err.println("Error fetching game lines: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
```

### application.properties Update

```properties
spring.application.name=BettingAnalytics
# ============================================
# DATABASE CONFIGURATION
# ============================================
spring.datasource.url=jdbc:mysql://localhost:3306/betting_analytics?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ============================================
# JPA / HIBERNATE CONFIGURATION
# ============================================
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect

# ============================================
# SERVER CONFIGURATION
# ============================================
server.port=8080

# ============================================
# LOGGING CONFIGURATION
# ============================================
logging.level.com.coltwarren.sports_betting_analytics=DEBUG



# ============================================
# CLAUDE AI CONFIGURATION
# ============================================
claude.api.key=${CLAUDE_API_KEY}
claude.api.url=https://api.anthropic.com/v1/messages
claude.model=claude-sonnet-4-20250514

# ============================================
# ODDS API CONFIGURATION
# ============================================
odds.api.key=${ODDS_API_KEY}
odds.api.url=https://api.the-odds-api.com/v4
weather.api.key=313f9507472c126b8ce6fcf79f121f8f

# ============================================
# COLLEGE FOOTBALL/BASKETBALL DATA API
# ============================================
cfbd.api.key=${CFBD_API_KEY}
cfbd.api.url=https://api.collegefootballdata.com
```

---

## 🚀 PART 7: STEP-BY-STEP IMPLEMENTATION

### STEP 1: Update application.properties ✅

```bash
# Edit the file
nano /Users/coltwarren/projects/sports-betting-analytics/src/main/resources/application.properties

# Add these 4 lines at the end:
# ============================================
# COLLEGE FOOTBALL/BASKETBALL DATA API
# ============================================
cfbd.api.key=${CFBD_API_KEY}
cfbd.api.url=https://api.collegefootballdata.com
```

### STEP 2: Set Environment Variable ✅

```bash
export CFBD_API_KEY=HiNBYl/WBC0BLmmaGfXWz35rYqFBFpn0Nbz6g6vH8TC5nj8imYp2WwreJhewXY8Y
```

### STEP 3: Create college package directory ✅

```bash
mkdir -p /Users/coltwarren/projects/sports-betting-analytics/src/main/java/com/coltwarren/sports_betting_analytics/service/college
```

### STEP 4: Create CFBDataService.java ✅

Copy the template from Part 6 into:
```
/Users/coltwarren/projects/sports-betting-analytics/src/main/java/com/coltwarren/sports_betting_analytics/service/college/CFBDataService.java
```

### STEP 5: Test the service ✅

Create a test controller to verify API connectivity:

```java
@RestController
@RequestMapping("/test")
public class CFBDTestController {

    @Autowired
    private CFBDataService cfbDataService;

    @GetMapping("/cfbd/games")
    public List<Map<String, Object>> testGames() {
        return cfbDataService.getGames(2025, 1, null);
    }

    @GetMapping("/cfbd/ratings")
    public Map<String, Double> testRatings() {
        return cfbDataService.getSPRatings(2025);
    }
}
```

**Test URLs:**
- http://localhost:8080/test/cfbd/games
- http://localhost:8080/test/cfbd/ratings

### STEP 6: Update MultiSportBestBetsService ✅

Add CFBDataService autowiring and enhance college sports analysis.

### STEP 7: Restart application ✅

```bash
# Kill current application (Ctrl+C)

# Restart with all API keys
export CLAUDE_API_KEY=YOUR_CLAUDE_API_KEY_HERE && \
export ODDS_API_KEY=6f9a804009739bb562afa482297057e7 && \
export WEATHER_API_KEY=313f9507472c126b8ce6fcf79f121f8f && \
export CFBD_API_KEY=HiNBYl/WBC0BLmmaGfXWz35rYqFBFpn0Nbz6g6vH8TC5nj8imYp2WwreJhewXY8Y && \
./mvnw spring-boot:run
```

---

## 📊 PART 8: INTEGRATION WITH BEST BETS

### How CFBD Enhances Your Best Bets

**Current Flow:**
1. MultiSportBestBetsService gets games from LiveGameService
2. Gets odds from MultiSportOddsService
3. Sends prompt to Claude AI for analysis
4. Calculates Kelly based on AI confidence

**Enhanced Flow (with CFBD):**
1. MultiSportBestBetsService gets games from LiveGameService
2. **For CFB/CBB: Get SP+ ratings, team stats, rankings from CFBD**
3. Gets odds from MultiSportOddsService
4. **Send enriched prompt to Claude AI with SP+ ratings, stats, rankings**
5. **Calculate more accurate win probability using SP+ differential**
6. **Calculate Kelly with data-driven confidence instead of AI estimate**

### Example Enhanced Analysis

**Before (current):**
```java
String prompt = "Quick betting analysis for CFB game: Alabama vs Georgia";
```

**After (with CFBD):**
```java
Map<String, Double> spRatings = cfbDataService.getSPRatings(2026);
double alabamaSP = spRatings.get("Alabama");  // e.g., 28.5
double georgiaSP = spRatings.get("Georgia");  // e.g., 31.2

String prompt = String.format(
    "Betting analysis for CFB game: Alabama vs Georgia.\n" +
    "SP+ Ratings: Alabama %.1f, Georgia %.1f (Georgia +%.1f edge)\n" +
    "Current spread: Alabama +3.5\n" +
    "Analyze if Alabama +3.5 has value given SP+ differential.",
    alabamaSP, georgiaSP, georgiaSP - alabamaSP
);
```

---

## 🎓 PART 9: CFBD DATA USAGE EXAMPLES

### SP+ Rating Differential → Win Probability

```java
public double calculateWinProbability(double teamSP, double opponentSP, boolean isHome) {
    double homeAdvantage = isHome ? 2.5 : -2.5;  // ~2.5 point home advantage
    double differential = teamSP - opponentSP + homeAdvantage;

    // Convert SP+ differential to win probability
    // Formula based on historical correlation
    return 1 / (1 + Math.exp(-0.13 * differential));
}
```

**Example:**
- Alabama SP+: 28.5
- Georgia SP+: 31.2
- Georgia at home: differential = 31.2 - 28.5 + 2.5 = 5.2
- Win probability: 1 / (1 + e^(-0.13 * 5.2)) = 64.8%

### Rankings → Confidence Boost

```java
if (team.isRankedTop10()) {
    confidence += 1.0;  // Boost AI confidence for ranked teams
}
```

### Weather → Total Adjustments

```java
Map<String, Object> weather = cfbDataService.getGameWeather(gameId);
if (weather.get("windSpeed") > 20) {
    // High wind → bet Under
    recommendation = "Under is strong with 20+ mph winds";
}
```

---

## ✅ SUMMARY CHECKLIST

### Immediate Actions (Do Now):

- [x] **Verify Kelly Fix** - ALREADY DONE ✅
- [ ] **Update application.properties** - Add CFBD config
- [ ] **Set CFBD_API_KEY environment variable**
- [ ] **Create college package directory**
- [ ] **Create CFBDataService.java**
- [ ] **Test CFBD API connectivity**

### Phase 2 (After Basic Service Works):

- [ ] **Create test controller** for CFBD endpoints
- [ ] **Update MultiSportBestBetsService** to use CFBD
- [ ] **Enhance CFB analysis** with SP+ ratings
- [ ] **Enhance CBB analysis** with team stats

### Phase 3 (Advanced Features):

- [ ] **Create CollegeMatchupAnalyzer** service
- [ ] **Implement SP+ → win probability** calculator
- [ ] **Add historical line comparison** for CLV
- [ ] **Create college-specific dashboard** page

---

## 🔑 KEY TAKEAWAYS

1. **Kelly Criterion** - Already fixed, no action needed
2. **CFBD API Key** - Single key works for both CFB and CBB
3. **SP+ Ratings** - Most valuable CFBD data for betting
4. **Integration** - Enhances existing MultiSportBestBetsService
5. **Win Probability** - Can calculate from SP+ differential

---

**Next Steps:** Start with Step 1 and work through the implementation checklist!
