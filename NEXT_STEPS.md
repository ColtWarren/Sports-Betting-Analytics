# CFBD Integration - Next Steps

## ✅ COMPLETED (Done for you!)

1. **Kelly Criterion Fix** - Verified and confirmed working ✅
   - Line 548 in best-bets.html is correct
   - No `* 100` multiplication bug
   - Kelly will display as 2-8% as expected

2. **application.properties Updated** ✅
   - Added CFBD API configuration
   - Lines 44-48 added

3. **CFBDataService.java Created** ✅
   - Full service implementation
   - 9 methods for accessing CFBD data:
     - `getGames()` - Fetch game schedules
     - `getSPRatings()` - Get SP+ ratings
     - `getRankings()` - AP/Coaches polls
     - `getTeamStats()` - Team statistics
     - `getAdvancedStats()` - Advanced metrics
     - `getGameLines()` - Historical betting lines
     - `getTeamTalent()` - Recruiting composites
     - `calculateWinProbability()` - SP+ → win %
     - `getCurrentWeek()` - Current CFB week

4. **CFBDTestController.java Created** ✅
   - 8 test endpoints to verify CFBD API
   - Includes win probability calculator
   - Full API status checker

---

## 🚀 YOUR NEXT STEPS

### STEP 1: Set the CFBD API Key Environment Variable

**Run this command in your terminal:**

```bash
export CFBD_API_KEY=HiNBYl/WBC0BLmmaGfXWz35rYqFBFpn0Nbz6g6vH8TC5nj8imYp2WwreJhewXY8Y
```

### STEP 2: Restart Your Application

**Kill the current app (Ctrl+C), then restart with ALL API keys:**

```bash
export CLAUDE_API_KEY=YOUR_CLAUDE_API_KEY_HERE && \
export ODDS_API_KEY=6f9a804009739bb562afa482297057e7 && \
export WEATHER_API_KEY=313f9507472c126b8ce6fcf79f121f8f && \
export CFBD_API_KEY=HiNBYl/WBC0BLmmaGfXWz35rYqFBFpn0Nbz6g6vH8TC5nj8imYp2WwreJhewXY8Y && \
./mvnw spring-boot:run
```

### STEP 3: Test the CFBD Integration

**Open these URLs in your browser to verify CFBD API is working:**

1. **Status Check:**
   ```
   http://localhost:8080/test/cfbd/status
   ```

2. **SP+ Ratings (Top 10 teams):**
   ```
   http://localhost:8080/test/cfbd/ratings?year=2025
   ```

3. **Win Probability Calculator:**
   ```
   http://localhost:8080/test/cfbd/winprob?year=2025&team=Alabama&opponent=Georgia&home=true
   ```

4. **Games This Week:**
   ```
   http://localhost:8080/test/cfbd/games?year=2025&week=1
   ```

### STEP 4: Verify It's Working

**You should see JSON responses like this:**

**Status endpoint should show:**
```json
{
  "service": "CFBD API Integration",
  "status": "Running",
  "gamesEndpoint": "✅ Working",
  "ratingsEndpoint": "✅ Working (130 teams)",
  "rankingsEndpoint": "✅ Working"
}
```

**Win probability endpoint should show:**
```json
{
  "team": "Alabama",
  "opponent": "Georgia",
  "teamSP": 28.5,
  "opponentSP": 31.2,
  "winProbability": "42.3%",
  "impliedOdds": 136
}
```

---

## 📚 OPTIONAL: Make Environment Variable Permanent

To avoid setting CFBD_API_KEY every time you restart your terminal:

**For zsh (macOS default):**
```bash
echo 'export CFBD_API_KEY=HiNBYl/WBC0BLmmaGfXWz35rYqFBFpn0Nbz6g6vH8TC5nj8imYp2WwreJhewXY8Y' >> ~/.zshrc
source ~/.zshrc
```

**For bash:**
```bash
echo 'export CFBD_API_KEY=HiNBYl/WBC0BLmmaGfXWz35rYqFBFpn0Nbz6g6vH8TC5nj8imYp2WwreJhewXY8Y' >> ~/.bash_profile
source ~/.bash_profile
```

---

## 🎯 WHAT'S NEXT AFTER TESTING?

Once you confirm the CFBD API is working, you can:

### Phase 2: Integrate with Best Bets

**Update MultiSportBestBetsService.java to use CFBD data:**

```java
@Autowired
private CFBDataService cfbDataService;

private List<Map<String, Object>> analyzeSport(String sport) {
    if (sport.equals("CFB") || sport.equals("CBB")) {
        // Get SP+ ratings for more accurate analysis
        Map<String, Double> spRatings = cfbDataService.getSPRatings(2026);

        // Use SP+ to calculate win probability instead of AI estimate
        // This will make your Kelly Criterion calculations more accurate!
    }
    // ... rest of code
}
```

### Phase 3: Create College-Specific Features

- **SP+ Based Win Probability** - Replace AI confidence with data-driven probability
- **Talent Mismatch Detection** - Find games where recruiting disparity is high
- **Historical Line Analysis** - Use CFBD lines for CLV tracking
- **Weather-Adjusted Totals** - For outdoor games

---

## 📖 DOCUMENTATION

- **Full Implementation Plan:** See `CFBD_IMPLEMENTATION_PLAN.md`
- **Project Profile:** See `PROJECT_PROFILE.md`
- **CFBD API Docs:** https://collegefootballdata.com/api/docs

---

## 🐛 TROUBLESHOOTING

### Issue: "API key not configured"
**Solution:** Make sure you ran the export command and restarted the app

### Issue: "No data returned"
**Solution:**
- Check if it's CFB season (August - January)
- Try year=2024 instead of 2025
- Use week=12 for a week in the middle of season

### Issue: "Team not found in SP+ ratings"
**Solution:** Use exact team names (check /test/cfbd/ratings for available teams)

### Issue: Application won't start
**Solution:**
- Run `./mvnw clean compile` first
- Check if MySQL is running
- Verify all API keys are set

---

## ✅ SUCCESS CHECKLIST

- [ ] CFBD_API_KEY environment variable set
- [ ] Application restarted with new API key
- [ ] /test/cfbd/status shows all endpoints working
- [ ] SP+ ratings returned successfully
- [ ] Win probability calculator working
- [ ] Ready to integrate with MultiSportBestBetsService!

---

**You're all set!** Run the commands above and let me know if you see any errors.
