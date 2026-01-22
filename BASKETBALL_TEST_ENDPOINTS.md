# College Basketball (CBB) Test Endpoints

The CFBD API key works for **both** College Football (CFB) and College Basketball (CBB).

## Basketball Test URLs

Once your application is running, test these endpoints:

### 1. Basketball Status Check
**URL:** http://localhost:8080/test/cfbd/basketball/status

**What it does:**
- Tests all basketball endpoints
- Shows data availability
- Provides quick health check

**Expected Response:**
```json
{
  "service": "CFBD Basketball API Integration",
  "status": "Running",
  "sport": "College Basketball (CBB)",
  "ratingsEndpoint": "✅ Working (350 teams)",
  "gamesEndpoint": "✅ Working (5000+ games)",
  "rankingsEndpoint": "✅ Working",
  "statsEndpoint": "✅ Working (350 teams)"
}
```

---

### 2. Basketball SP+ Ratings (Top 25)
**URL:** http://localhost:8080/test/cfbd/basketball/ratings?year=2025

**What it shows:**
- Top 25 college basketball teams by SP+ rating
- Similar to KenPom ratings
- Used for win probability calculations

**Expected Response:**
```json
{
  "endpoint": "/ratings/sp/basketball",
  "sport": "College Basketball",
  "year": 2025,
  "teamsFound": 350,
  "dataQuality": "✅ Data available",
  "top25Teams": {
    "#1 Duke": "95.2 rating",
    "#2 Kansas": "93.7 rating",
    "#3 UConn": "92.5 rating",
    ...
  }
}
```

---

### 3. Basketball Games
**URL:** http://localhost:8080/test/cfbd/basketball/games?year=2025&seasonType=regular

**What it shows:**
- All college basketball games for the season
- Game schedules, scores, results
- First 10 games shown as samples

**Parameters:**
- `year` - Season year (default: 2025)
- `seasonType` - "regular" or "postseason" (default: regular)

**Expected Response:**
```json
{
  "endpoint": "/games",
  "sport": "College Basketball",
  "year": 2025,
  "seasonType": "regular",
  "gamesFound": 5247,
  "dataQuality": "✅ Data available",
  "sampleGames": [
    {
      "id": 401234567,
      "home_team": "Duke",
      "away_team": "North Carolina",
      "home_points": 78,
      "away_points": 75,
      "status": "completed"
    },
    ...
  ]
}
```

---

### 4. Basketball AP Poll Rankings
**URL:** http://localhost:8080/test/cfbd/basketball/rankings?year=2025&week=10

**What it shows:**
- AP Top 25 poll rankings
- Coaches Poll rankings
- Rankings by week

**Parameters:**
- `year` - Season year (default: 2025)
- `week` - Week number (default: 10)

**Expected Response:**
```json
{
  "endpoint": "/rankings",
  "sport": "College Basketball",
  "year": 2025,
  "week": 10,
  "pollsFound": 2,
  "dataQuality": "✅ Data available",
  "rankings": [...],
  "apTop25": [
    {"rank": 1, "school": "Duke", "points": 1500},
    {"rank": 2, "school": "Kansas", "points": 1450},
    ...
  ]
}
```

---

### 5. Basketball Team Statistics
**URL:** http://localhost:8080/test/cfbd/basketball/stats?year=2025

**What it shows:**
- Team offensive/defensive statistics
- Efficiency ratings
- Per-game averages

**Parameters:**
- `year` - Season year (default: 2025)
- `team` - Specific team (optional)

**Expected Response:**
```json
{
  "endpoint": "/stats/season",
  "sport": "College Basketball",
  "year": 2025,
  "teamsFound": 350,
  "dataQuality": "✅ Data available",
  "sampleStats": [
    {
      "team": "Duke",
      "games": 30,
      "points_per_game": 85.2,
      "field_goal_percentage": 48.5,
      "three_point_percentage": 38.2,
      "rebounds_per_game": 38.5
    },
    ...
  ]
}
```

---

## How to Use Basketball Data

### 1. Get Team Ratings for Win Probability

```java
// Get basketball SP+ ratings
Map<String, Double> ratings = cfbDataService.getBasketballRatings(2025);

// Calculate win probability
double dukeSP = ratings.get("Duke");  // e.g., 28.5
double uncSP = ratings.get("North Carolina");  // e.g., 25.2

// Win probability calculation (similar to football)
double winProbability = cfbDataService.calculateWinProbability(dukeSP, uncSP, true);
// Returns: 0.623 (62.3% for Duke at home)
```

### 2. Find High-Value Games

```java
// Get today's games
List<Map<String, Object>> games = cfbDataService.getBasketballGames(2025, "regular");

// Get rankings to identify top matchups
List<Map<String, Object>> rankings = cfbDataService.getBasketballRankings(2025, 10);

// Filter for ranked vs ranked games
// Compare SP+ ratings to betting lines
// Find value opportunities
```

### 3. Integrate with Best Bets Service

**In MultiSportBestBetsService.java:**

```java
private List<Map<String, Object>> analyzeSport(String sport) {
    if (sport.equals("CBB")) {
        // Get basketball-specific data
        Map<String, Double> spRatings = cfbDataService.getBasketballRatings(2026);
        List<Map<String, Object>> games = cfbDataService.getBasketballGames(2026, "regular");

        // Use SP+ ratings for better win probability estimates
        // More accurate Kelly Criterion calculations
        // Better bet recommendations
    }
    // ... existing logic
}
```

---

## Quick Start Commands

**1. Start application with all API keys:**
```bash
export CLAUDE_API_KEY=YOUR_CLAUDE_API_KEY_HERE && \
export ODDS_API_KEY=6f9a804009739bb562afa482297057e7 && \
export WEATHER_API_KEY=313f9507472c126b8ce6fcf79f121f8f && \
export CFBD_API_KEY=HiNBYl/WBC0BLmmaGfXWz35rYqFBFpn0Nbz6g6vH8TC5nj8imYp2WwreJhewXY8Y && \
./mvnw spring-boot:run
```

**2. Test basketball integration:**
```bash
# Open in browser
open http://localhost:8080/test/cfbd/basketball/status
```

**3. Get top 25 basketball teams:**
```bash
curl http://localhost:8080/test/cfbd/basketball/ratings?year=2025 | json_pp
```

---

## What's Different: CFB vs CBB

| Feature | College Football (CFB) | College Basketball (CBB) |
|---------|----------------------|--------------------------|
| **Endpoint** | `/games` | `/games?sport=basketball` |
| **Ratings** | `/ratings/sp` | `/ratings/sp/basketball` |
| **Rankings** | `/rankings` | `/rankings?sport=basketball` |
| **Stats** | `/stats/season` | `/stats/season?sport=basketball` |
| **Season** | Aug-Jan (weeks 1-15) | Nov-Mar (weeks 1-20) |
| **Teams** | ~130 FBS teams | ~350 D1 teams |
| **Home Advantage** | ~2.5 points | ~3.5 points |

---

## Basketball-Specific Considerations

### Win Probability Adjustments

**Basketball has larger home court advantage:**
```java
// Football: ~2.5 point home advantage
// Basketball: ~3.5 point home advantage

public double calculateBasketballWinProbability(double teamSP, double opponentSP, boolean isHome) {
    double homeAdvantage = isHome ? 3.5 : -3.5;  // Larger for basketball
    double differential = teamSP - opponentSP + homeAdvantage;
    return 1.0 / (1.0 + Math.exp(-0.13 * differential));
}
```

### Season Timing

- **Regular Season:** November - March (weeks 1-20)
- **Conference Tournaments:** Early March
- **March Madness:** Mid-March - Early April
- **Use different `week` parameter than football!**

---

## Testing Checklist

- [ ] Basketball status endpoint works
- [ ] SP+ ratings returned (should see ~350 teams)
- [ ] Top 25 displayed correctly
- [ ] Games endpoint returns data
- [ ] Rankings show AP/Coaches polls
- [ ] Team stats available
- [ ] Can filter by specific team
- [ ] Data quality checks pass

---

## Troubleshooting

**Issue: No basketball data returned**
- Basketball season is November-March
- Try `year=2024` if testing in off-season
- Use `week=10` (mid-season) for best data

**Issue: Team not found**
- Use full team names: "Duke" not "Duke Blue Devils"
- Check available teams in ratings response
- Case-sensitive team names

**Issue: Empty rankings**
- Rankings not available until season starts
- Week number must be valid (1-20)
- Try different week if empty

---

## Next Steps

Once basketball endpoints are working:

1. **Update MultiSportBestBetsService** - Add CBB to sports array
2. **Create Basketball Win Probability** - Adjust for 3.5 pt home advantage
3. **Integrate with Kelly Criterion** - Use SP+ ratings for better estimates
4. **Add to Best Bets Dashboard** - Show basketball recommendations
5. **Create CBB-Specific UI** - March Madness bracket analyzer

---

**All set!** Start your application and test the basketball endpoints! 🏀
