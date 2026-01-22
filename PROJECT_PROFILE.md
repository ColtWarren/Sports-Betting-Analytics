# Sports Betting Analytics Platform - Project Profile

**Last Updated:** January 20, 2026
**Version:** 0.0.1-SNAPSHOT
**Developer:** Colt Warren
**Deployment:** Railway (Production)

---

## 1. PROJECT OVERVIEW

### Purpose
AI-powered sports betting analytics platform that provides real-time odds tracking, advanced statistical analysis, Kelly Criterion betting recommendations, tax reporting, and multi-sport best bet identification across NFL, CFB, NBA, CBB, MLB, and NHL.

### Tech Stack

**Backend:**
- **Java:** 17
- **Spring Boot:** 4.0.1
- **ORM:** Hibernate/JPA with auto-DDL schema updates
- **Database:** MySQL 8.0
- **HTTP Client:** Spring WebFlux (for external API calls)
- **Build Tool:** Maven

**Frontend:**
- **Template Engine:** Thymeleaf
- **UI Theme:** Dark Cyberpunk (#0a0e27 background, #7c3aed purple accents)
- **JavaScript:** Vanilla JS (no frameworks)
- **Fonts:** 'Inter' for text, 'JetBrains Mono' for numbers

**Database:**
- **Host:** localhost:3306 (development), Railway MySQL (production)
- **Database Name:** betting_analytics
- **Schema Management:** Hibernate auto-DDL (spring.jpa.hibernate.ddl-auto=update)

**Deployment:**
- **Platform:** Railway
- **Port:** 8080 (HTTP)

---

## 2. PROJECT STRUCTURE

### Directory Tree
```
sports-betting-analytics/
├── src/
│   ├── main/
│   │   ├── java/com/coltwarren/sports_betting_analytics/
│   │   │   ├── BettingAnalyticsApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── ActiveBetsController.java
│   │   │   │   ├── ActiveBetsPageController.java
│   │   │   │   ├── AIController.java
│   │   │   │   ├── AutoSettleController.java
│   │   │   │   ├── BestBetsPageController.java
│   │   │   │   ├── BetController.java
│   │   │   │   ├── CLVController.java
│   │   │   │   ├── DashboardController.java
│   │   │   │   ├── EspnTestController.java
│   │   │   │   ├── EVController.java
│   │   │   │   ├── GameSyncController.java
│   │   │   │   ├── KellyController.java
│   │   │   │   ├── LiveBettingController.java
│   │   │   │   ├── LiveBettingPageController.java
│   │   │   │   ├── MatchupAnalysisController.java
│   │   │   │   ├── MultiSportBestBetsController.java
│   │   │   │   ├── MultiSportOddsController.java
│   │   │   │   ├── NotificationController.java
│   │   │   │   ├── OddsBackfillController.java
│   │   │   │   ├── OddsController.java
│   │   │   │   ├── TaxController.java
│   │   │   │   └── WeatherTestController.java
│   │   │   ├── model/
│   │   │   │   ├── Bankroll.java
│   │   │   │   ├── Bet.java
│   │   │   │   ├── GameStats.java
│   │   │   │   ├── W2GForm.java
│   │   │   │   └── odds/
│   │   │   │       └── OddsResponse.java
│   │   │   ├── repository/
│   │   │   │   ├── BankrollRepository.java
│   │   │   │   ├── BetRepository.java
│   │   │   │   ├── GameStatsRepository.java
│   │   │   │   └── W2GFormRepository.java
│   │   │   └── service/
│   │   │       ├── ActiveBetsTrackerService.java
│   │   │       ├── AdvancedEVCalculator.java
│   │   │       ├── AutoSettleService.java
│   │   │       ├── BankrollService.java
│   │   │       ├── BetService.java
│   │   │       ├── CLVTracker.java
│   │   │       ├── EspnGameSyncService.java
│   │   │       ├── EspnService.java
│   │   │       ├── KellyCriterionService.java          ⭐ KEY SERVICE
│   │   │       ├── LiveGameService.java
│   │   │       ├── MultiSportBestBetsService.java      ⭐ KEY SERVICE
│   │   │       ├── NotificationService.java
│   │   │       ├── OddsBackfillService.java
│   │   │       ├── StatsService.java
│   │   │       ├── TaxReportService.java               ⭐ KEY SERVICE
│   │   │       ├── WeatherService.java
│   │   │       ├── ai/
│   │   │       │   ├── ClaudeAIService.java
│   │   │       │   └── MatchupAnalyzerService.java
│   │   │       ├── espn/
│   │   │       │   └── ESPNApiService.java
│   │   │       └── odds/
│   │   │           ├── BestBetsAnalyzer.java
│   │   │           ├── MultiSportOddsService.java
│   │   │           └── OddsService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/
│   │       └── templates/
│   │           ├── active-bets.html
│   │           ├── ai-analysis.html
│   │           ├── bankroll.html
│   │           ├── best-bets.html                      ⭐ KEY TEMPLATE
│   │           ├── bet-form.html
│   │           ├── charts.html
│   │           ├── clv.html
│   │           ├── dashboard.html                      ⭐ KEY TEMPLATE
│   │           ├── live-betting.html
│   │           ├── notifications.html
│   │           └── tax/
│   │               ├── dashboard.html
│   │               ├── deductions.html
│   │               ├── w2g.html
│   │               └── win-loss.html
│   └── test/
│       └── java/com/coltwarren/sports_betting_analytics/
│           └── BettingAnalyticsApplicationTests.java
├── pom.xml
└── README.md
```

---

## 3. DATABASE SCHEMA

### Tables

#### `bets`
Primary table for tracking all placed bets.

| Column | Type | Description |
|--------|------|-------------|
| id | bigint | Primary key (auto-increment) |
| sport | varchar(50) | Sport type (NFL, NBA, CFB, etc.) |
| event_name | varchar(200) | Game/event description |
| bet_type | varchar(50) | SPREAD, MONEYLINE, TOTAL, etc. |
| selection | varchar(100) | Specific bet selection |
| odds | decimal(6,2) | American odds format |
| stake | decimal(10,2) | Amount wagered |
| potential_payout | decimal(10,2) | Potential winnings |
| actual_payout | decimal(10,2) | Actual payout (when settled) |
| profit_loss | decimal(10,2) | Net profit/loss |
| status | varchar(20) | PENDING, WON, LOST, PUSH |
| sportsbook_name | varchar(50) | Sportsbook used |
| sportsbook_location | varchar(100) | Location (for tax purposes) |
| placed_at | datetime(6) | Timestamp bet was placed |
| settled_at | datetime(6) | Timestamp bet was settled |
| event_start_time | datetime(6) | Game start time |
| closing_odds | decimal(6,2) | Closing line odds |
| beat_closing_line | bit(1) | CLV tracking |
| triggers_w2g | bit(1) | W-2G tax form required |
| w2g_issued | bit(1) | W-2G form received |
| notes | varchar(500) | User notes |

#### `bankroll`
Tracks bankroll balance over time.

| Column | Type | Description |
|--------|------|-------------|
| id | bigint | Primary key |
| balance | decimal(10,2) | Current balance |
| timestamp | datetime(6) | Balance snapshot time |

#### `game_stats`
Stores statistics for games (ESPN integration).

| Column | Type | Description |
|--------|------|-------------|
| id | bigint | Primary key |
| game_id | varchar(50) | External game ID |
| home_team | varchar(100) | Home team name |
| away_team | varchar(100) | Away team name |
| home_score | int | Home team score |
| away_score | int | Away team score |
| status | varchar(50) | Game status |
| game_date | datetime(6) | Game date/time |
| sport | varchar(50) | Sport type |

#### `w2g_forms`
Tracks W-2G tax forms for large wins.

| Column | Type | Description |
|--------|------|-------------|
| id | bigint | Primary key |
| bet_id | bigint | Reference to bets table |
| date_won | date | Date of winning bet |
| gross_winnings | decimal(10,2) | Total payout |
| wager_amount | decimal(10,2) | Original stake |
| net_winnings | decimal(10,2) | Profit amount |
| sportsbook_name | varchar(50) | Sportsbook name |
| tax_year | int | Tax year |
| form_received | bit(1) | Form receipt status |
| created_at | datetime(6) | Record creation time |
| updated_at | datetime(6) | Last update time |

---

## 4. API INTEGRATIONS

### API Keys Configuration

**Environment Variables:**
```bash
export CLAUDE_API_KEY=sk-ant-api03-...
export ODDS_API_KEY=6f9a804009739bb562afa482297057e7
export WEATHER_API_KEY=313f9507472c126b8ce6fcf79f121f8f
```

**application.properties Configuration:**
```properties
# Claude AI
claude.api.key=${CLAUDE_API_KEY}
claude.api.url=https://api.anthropic.com/v1/messages
claude.model=claude-sonnet-4-20250514

# The Odds API
odds.api.key=${ODDS_API_KEY}
odds.api.url=https://api.the-odds-api.com/v4

# Weather API
weather.api.key=313f9507472c126b8ce6fcf79f121f8f
```

### API Services

#### 1. **Claude AI (Anthropic)**
- **Service:** `ClaudeAIService.java`
- **Usage:** AI-powered bet analysis, matchup predictions, confidence scoring
- **Endpoints:** `/v1/messages`
- **Model:** claude-sonnet-4-20250514

#### 2. **The Odds API**
- **Service:** `MultiSportOddsService.java`, `OddsService.java`
- **Usage:** Real-time odds fetching across 6+ sports
- **Sports Covered:** NFL, CFB, NBA, CBB, MLB, NHL
- **Features:** Line shopping, best odds detection, CLV tracking

#### 3. **Weather API**
- **Service:** `WeatherService.java`
- **Usage:** Weather data for outdoor games (NFL, MLB)
- **Impact:** Betting analysis for totals

#### 4. **ESPN API (Unofficial)**
- **Service:** `ESPNApiService.java`, `EspnService.java`
- **Usage:** Game schedules, live scores, auto-settlement
- **Features:** Real-time score updates, game status tracking

---

## 5. KELLY CRITERION ISSUE (RESOLVED)

### The Problem

Kelly percentages were displaying as **773%**, **898%**, etc., instead of the expected **2-5%** range.

### Root Cause Analysis

The issue was a **double multiplication by 100** in the Kelly calculation pipeline:

**Step 1 - KellyCriterionService.java (Line 65):**
```java
// Kelly calculation returns decimal (e.g., 0.05 for 5%)
double kellyPercentage = ((b * p) - q) / b;

// FIRST multiplication by 100
result.put("kellyPercentage", kellyPercentage * 100);  // ⚠️ Returns 5.0
```

**Step 2 - MultiSportBestBetsService.java (Line 275):**
```java
Double kellyPercentage = (Double) kellyResult.get("kellyPercentage");
// kellyPercentage = 5.0 (already a percentage)

return kellyPercentage != null ? kellyPercentage : 0.0;
// Returns 5.0
```

**Step 3 - MultiSportBestBetsService.java (Line 135):**
```java
bet.put("kellyPercent", kellyPercent);
// Stores 5.0
```

**Step 4 - best-bets.html (Line 548) - THE BUG:**
```html
<!-- BEFORE FIX: This was multiplying again! -->
💰 Kelly: <span class="kelly-percent">${formatKelly(bet.kellyPercent * 100)}%</span>
<!-- This would show 500% instead of 5% -->
```

### The Fix

**File:** `best-bets.html`
**Location:** Line 548

**BEFORE (Buggy):**
```html
💰 Kelly: <span class="kelly-percent">${formatKelly(bet.kellyPercent * 100)}%</span>
```

**AFTER (Fixed):**
```html
💰 Kelly: <span class="kelly-percent">${formatKelly(bet.kellyPercent)}%</span>
```

**Result:** Removed the `* 100` multiplication since `kellyPercent` is already a percentage value from the backend.

### Code Flow (Correct)

```
KellyCriterionService:
  kellyPercentage = 0.05 (decimal)
  * 100
  → returns 5.0 (percentage)

MultiSportBestBetsService:
  retrieves 5.0
  → returns 5.0 (percentage)

best-bets.html:
  displays 5.0 + "%"
  → shows "5.0%"
```

### Kelly Formula Implementation

**File:** `KellyCriterionService.java` (Lines 39-52)

```java
/**
 * Kelly Criterion Formula: Kelly % = (bp - q) / b
 * Where:
 * - b = net decimal odds (decimal odds - 1)
 * - p = probability of winning
 * - q = probability of losing (1 - p)
 */

double b = decimalOdds - 1;
double kellyPercentage = ((b * p) - q) / b;

// Quarter Kelly for safety (recommended)
if (fractional) {
    kellyPercentage = kellyPercentage * 0.25;
}

// Ensure non-negative
if (kellyPercentage < 0) {
    kellyPercentage = 0;
}

result.put("kellyPercentage", kellyPercentage * 100);  // Convert to %
```

### Expected Kelly Ranges

- **Full Kelly:** 0-50% (aggressive, high risk)
- **Quarter Kelly:** 0-12.5% (conservative, recommended)
- **Typical Values:** 2-8% for most bets

---

## 6. KEY FEATURES

### Current Working Features

✅ **Multi-Sport Odds Tracking**
- Real-time odds from The Odds API
- Support for NFL, CFB, NBA, CBB, MLB, NHL
- Automatic line shopping across sportsbooks

✅ **AI-Powered Analysis**
- Claude AI integration for matchup analysis
- Confidence scoring (1-10 scale)
- Automated bet recommendations

✅ **Kelly Criterion Calculator**
- Full Kelly and Quarter Kelly calculations
- Bankroll-based stake recommendations
- Expected value (EV) calculations

✅ **Best Bets Dashboard**
- Multi-sport parallel analysis
- Top 20 bets ranked by confidence
- Kelly percentage recommendations
- Real-time game data integration

✅ **Active Bets Tracker**
- Live score updates
- Real-time P/L calculations
- Position monitoring
- Auto-settlement via ESPN API

✅ **Tax Center (IRS-Compliant)**
- W-2G form tracking (wins ≥ $600 AND ≥ 300x wager)
- Win/Loss statements by tax year
- Deductible loss calculations
- Tax impact analysis (standard vs itemized)

✅ **Closing Line Value (CLV) Tracker**
- Automatic CLV calculation on bet settlement
- Historical CLV performance metrics
- Beat-the-line percentage tracking

✅ **Bankroll Management**
- Balance tracking over time
- Win rate calculations
- ROI analytics

✅ **Notifications System**
- Bet settlement alerts
- Line movement notifications
- W-2G form reminders

---

## 7. CURRENT STATE

### What's Working
- ✅ Application runs on localhost:8080
- ✅ MySQL database connected and schema up-to-date
- ✅ All 6 sports odds fetching successfully
- ✅ Claude AI analysis generating recommendations
- ✅ Kelly Criterion calculations accurate (2-8% range)
- ✅ Tax Center fully functional with 4 pages
- ✅ Active bets tracking with live updates
- ✅ Dark cyberpunk UI consistent across all pages

### Recently Fixed
- ✅ Kelly Criterion double-multiplication bug (773% → 5%)
- ✅ Tax Center navigation added to dashboard
- ✅ W-2G auto-detection on bet settlement
- ✅ Database schema updated with tax fields

### Known Issues / To-Do
- ⚠️ PDF export functionality (placeholder buttons)
- ⚠️ Email sharing with accountant (placeholder)
- ⚠️ W-2G form upload feature (placeholder)
- ⚠️ Historical odds backfill (incomplete)

---

## 8. NAVIGATION STRUCTURE

### Main Pages

| Route | Controller | Template | Description |
|-------|-----------|----------|-------------|
| `/` | DashboardController | dashboard.html | Main command center |
| `/bet/new` | BetController | bet-form.html | Place new bet |
| `/active-bets` | ActiveBetsPageController | active-bets.html | Track live bets |
| `/best-bets` | BestBetsPageController | best-bets.html | AI recommendations |
| `/live-betting` | LiveBettingPageController | live-betting.html | Live games |
| `/charts` | DashboardController | charts.html | Performance charts |
| `/ai-analysis` | AIController | ai-analysis.html | Matchup analyzer |
| `/bankroll` | DashboardController | bankroll.html | Bankroll tracker |
| `/clv` | CLVController | clv.html | CLV analytics |
| `/notifications` | NotificationController | notifications.html | Alerts |
| `/tax` | TaxController | tax/dashboard.html | Tax center |
| `/tax/win-loss` | TaxController | tax/win-loss.html | W/L statement |
| `/tax/w2g` | TaxController | tax/w2g.html | W-2G tracker |
| `/tax/deductions` | TaxController | tax/deductions.html | Deduction calc |

---

## 9. STARTUP INSTRUCTIONS

### Local Development

```bash
# Navigate to project
cd ~/projects/sports-betting-analytics

# Set environment variables
export CLAUDE_API_KEY=sk-ant-api03-...
export ODDS_API_KEY=6f9a804009739bb562afa482297057e7
export WEATHER_API_KEY=313f9507472c126b8ce6fcf79f121f8f

# Start MySQL (if not running)
# Application will auto-create database if needed

# Run application
./mvnw spring-boot:run

# Access at http://localhost:8080
```

### Database Setup
```sql
-- MySQL connection
Host: localhost:3306
Database: betting_analytics
Username: root
Password: root

-- Schema auto-created by Hibernate
-- No manual SQL required
```

---

## 10. DEPLOYMENT (RAILWAY)

### Environment Variables (Production)
```
CLAUDE_API_KEY=sk-ant-api03-...
ODDS_API_KEY=6f9a804009739bb562afa482297057e7
WEATHER_API_KEY=313f9507472c126b8ce6fcf79f121f8f
SPRING_DATASOURCE_URL=<Railway MySQL URL>
SPRING_DATASOURCE_USERNAME=<Railway DB User>
SPRING_DATASOURCE_PASSWORD=<Railway DB Password>
```

### Git Deployment
```bash
# Commit and push
git add .
git commit -m "Your commit message"
git push origin main

# Railway auto-deploys from main branch
```

---

## 11. CODE SAMPLES

### Kelly Criterion Calculation

**Service:** `KellyCriterionService.java`

```java
public Map<String, Object> calculateKelly(int americanOdds, double winProbability, boolean fractional) {
    Map<String, Object> result = new HashMap<>();

    // Convert American odds to decimal
    double decimalOdds = americanToDecimal(americanOdds);

    // Calculate probabilities
    double p = winProbability;
    double q = 1 - winProbability;

    // Kelly formula: (bp - q) / b
    double b = decimalOdds - 1;
    double kellyPercentage = ((b * p) - q) / b;

    // Quarter Kelly for safety
    if (fractional) {
        kellyPercentage = kellyPercentage * 0.25;
    }

    // Non-negative constraint
    if (kellyPercentage < 0) {
        kellyPercentage = 0;
    }

    // Get current bankroll
    BigDecimal currentBankroll = bankrollService.getCurrentBankroll();

    // Calculate recommended stake
    BigDecimal recommendedStake = currentBankroll
        .multiply(BigDecimal.valueOf(kellyPercentage))
        .setScale(2, RoundingMode.HALF_UP);

    // Calculate EV
    double ev = (decimalOdds * p) - 1;

    result.put("kellyPercentage", kellyPercentage * 100);  // Convert to %
    result.put("recommendedStake", recommendedStake);
    result.put("currentBankroll", currentBankroll);
    result.put("expectedValue", ev * 100);

    return result;
}
```

### W-2G Auto-Detection

**Model:** `Bet.java`

```java
/**
 * W-2G required if: winnings >= $600 AND payout is 300x or more the wager
 */
public boolean checkIfTriggersW2G() {
    if (status != null && status.equals("WON") && actualPayout != null && stake != null) {
        BigDecimal netWinnings = actualPayout.subtract(stake);
        boolean meetsAmountThreshold = netWinnings.compareTo(new BigDecimal("600")) >= 0;

        BigDecimal ratio = actualPayout.divide(stake, 2, RoundingMode.HALF_UP);
        boolean meets300xThreshold = ratio.compareTo(new BigDecimal("300")) >= 0;

        this.triggersW2G = meetsAmountThreshold && meets300xThreshold;
        return this.triggersW2G;
    }
    this.triggersW2G = false;
    return false;
}
```

### Multi-Sport Best Bets Analysis

**Service:** `MultiSportBestBetsService.java`

```java
public List<Map<String, Object>> getBestBetsAcrossAllSports() {
    // Parallel processing for 6 sports
    ExecutorService executor = Executors.newFixedThreadPool(6);
    List<Future<List<Map<String, Object>>>> futures = new ArrayList<>();

    for (String sport : SPORTS) {
        Future<List<Map<String, Object>>> future = executor.submit(() -> analyzeSport(sport));
        futures.add(future);
    }

    // Collect all bets
    List<Map<String, Object>> allBets = new ArrayList<>();
    for (Future<List<Map<String, Object>>> future : futures) {
        List<Map<String, Object>> sportBets = future.get(30, TimeUnit.SECONDS);
        allBets.addAll(sportBets);
    }

    executor.shutdown();

    // Sort by confidence and return top 20
    allBets.sort((a, b) -> {
        Double scoreA = (Double) a.getOrDefault("confidence", 0.0);
        Double scoreB = (Double) b.getOrDefault("confidence", 0.0);
        return scoreB.compareTo(scoreA);
    });

    return allBets.stream().limit(20).collect(Collectors.toList());
}
```

---

## 12. RECENT GIT COMMITS

```
60d7307 SESSION: DARK CYBERPUNK THEME + NAVIGATION FIXES
a15a437 UPDATE: Dashboard navigation with Active Bets and Live Games links
afdf017 SESSION: ACTIVE BETS UI - REAL-TIME BET TRACKING
9085c5d MINI SESSION: MULTI-SPORT BEST BETS WITH AI ANALYSIS
f14a8b7 SESSION 8 FINALE: ACTIVE BETS TRACKER + POSITION MONITORING
```

---

## 13. DEPENDENCIES (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 14. CONTACT & SUPPORT

**GitHub Issues:** https://github.com/coltwarren/sports-betting-analytics/issues
**Developer:** Colt Warren
**Project Start:** January 2026

---

**NOTE:** This project is for educational and analytical purposes. Always bet responsibly and consult with tax professionals regarding tax obligations.
