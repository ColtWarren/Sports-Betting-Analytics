# SportsDataIO API Test Results

**Test Date:** January 27, 2026
**API Key:** 5b255ff80627441a9757ad28bf9578c1
**Purpose:** Evaluate if SportsDataIO can replace The Odds API ($25-75/mo)

---

## Executive Summary

### Can it replace The Odds API? **NO**

| Criteria | SportsDataIO Free Trial | The Odds API |
|----------|------------------------|--------------|
| Sportsbook Names | **SCRAMBLED** (obfuscated) | Real names |
| Soccer 3-Way Odds | **401 UNAUTHORIZED** | Full support |
| Missouri Books | **CANNOT VERIFY** | Verified working |
| Pricing | Contact sales (unclear) | $25-75/mo (transparent) |
| Live Updates | 10-minute delay | Real-time |

**Bottom Line:** The SportsDataIO free trial obfuscates ALL critical data we need to verify. Soccer endpoints are completely inaccessible. This API is **NOT viable** for our Missouri sports betting platform without purchasing an expensive enterprise plan.

---

## Test Results by Sport

### NFL Betting Odds
| Metric | Result |
|--------|--------|
| Endpoint | `/v3/nfl/odds/json/GameOddsByWeek/2025/1` |
| Status | **SUCCESS** (data returned) |
| Response Time | ~800ms |
| Sportsbook Names | **"Scrambled"** (all obfuscated) |
| SportsbookIds Found | 7, 8, 9, 10, 12, 14, 19, 22, 24, 40 |
| Sportsbook Count | 10 books |

**Sample Response:**
```json
{
  "Sportsbook": "Scrambled",
  "SportsbookId": 7,
  "HomeMoneyLine": -561,
  "AwayMoneyLine": 433,
  "HomePointSpread": -10.8,
  "OverUnder": 60.5
}
```

**Issue:** Cannot identify which SportsbookId maps to DraftKings, FanDuel, etc.

---

### NBA Betting Odds
| Metric | Result |
|--------|--------|
| Endpoint | `/v3/nba/odds/json/GameOddsByDate/2026-01-27` |
| Status | **SUCCESS** (data returned) |
| Response Time | ~750ms |
| Sportsbook Names | **"Scrambled"** (all obfuscated) |
| SportsbookIds Found | 7, 8, 9, 10, 12, 14, 19, 22, 24, 40 |

---

### Soccer Betting Odds (CRITICAL)
| Competition | Status | Error |
|-------------|--------|-------|
| EPL (Premier League) | **401 UNAUTHORIZED** | "Not authorized to access this soccer competition" |
| MLS | **401 UNAUTHORIZED** | "Not authorized to access this soccer competition" |
| Bundesliga | **401 UNAUTHORIZED** | "Not authorized to access this soccer competition" |
| La Liga | **401 UNAUTHORIZED** | "Not authorized to access this soccer competition" |

**CRITICAL FAILURE:** Soccer endpoints are **completely inaccessible** in the free trial. We CANNOT verify 3-way betting (Home/Draw/Away) support.

---

### MLB Betting Odds
| Metric | Result |
|--------|--------|
| Status | **SUCCESS** (endpoint works) |
| Odds Available | No (season not started) |
| Note | Games scheduled but no odds posted yet |

---

### NHL Betting Odds
| Metric | Result |
|--------|--------|
| Status | **SUCCESS** (data returned) |
| Sportsbook Names | **"Scrambled"** |
| SportsbookIds Found | 7, 8, 9, 10, 12, 14, 19, 22, 24, 40 |

---

## Missouri Sportsbooks Verification

### Required Missouri-Legal Sportsbooks (8 total):

| Sportsbook | Status | Notes |
|------------|--------|-------|
| DraftKings | **CANNOT VERIFY** | Names scrambled |
| FanDuel | **CANNOT VERIFY** | Names scrambled |
| BetMGM | **CANNOT VERIFY** | Names scrambled |
| Caesars | **CANNOT VERIFY** | Names scrambled |
| Fanatics | **CANNOT VERIFY** | Names scrambled |
| bet365 | **CANNOT VERIFY** | Names scrambled |
| Circa Sports | **CANNOT VERIFY** | Names scrambled |
| theScore Bet | **CANNOT VERIFY** | Names scrambled |

**Result:** 0 of 8 Missouri sportsbooks verified. All names show as "Scrambled" in free trial.

---

## Soccer 3-Way Odds Verification

### Required for Soccer Betting:
- Home Win odds (1)
- Draw odds (X)
- Away Win odds (2)

### Result: **CANNOT VERIFY**

All soccer endpoints return `401 Unauthorized`. The free trial does not include access to ANY soccer competitions (EPL, MLS, La Liga, Bundesliga, Serie A, Ligue 1, Champions League).

This is a **CRITICAL FAILURE** for our platform which requires 3-way soccer betting support.

---

## API Performance

| Metric | SportsDataIO Free Trial | The Odds API |
|--------|------------------------|--------------|
| Average Response Time | 750-800ms | ~500ms |
| Data Format | JSON | JSON |
| Live Update Delay | **10 minutes** | Real-time |
| Monthly API Calls | 1,000 (free trial) | 500-unlimited |

---

## Pricing Comparison

### SportsDataIO
- Free Trial: 1,000 calls/month, scrambled data, no soccer
- Paid Plans: **Not publicly disclosed** - must contact sales
- Likely enterprise pricing ($$$$)

### The Odds API (Current)
| Plan | Price | Requests |
|------|-------|----------|
| Free | $0/mo | 500/month |
| Starter | $25/mo | 20,000/month |
| Standard | $50/mo | 50,000/month |
| Professional | $75/mo | 100,000/month |

---

## Data Quality Comparison

| Feature | SportsDataIO | The Odds API |
|---------|--------------|--------------|
| Sportsbook Names | Scrambled | Real names (DraftKings, FanDuel, etc.) |
| Missouri Books | Cannot verify | All 8 verified |
| Soccer 3-Way | Not accessible | Full support |
| Odds Format | American (+/-) | American (+/-) |
| Live Odds | 10-min delay | Real-time |
| Documentation | Comprehensive | Good |

---

## Critical Issues Found

### 1. Sportsbook Names Scrambled
**Severity: CRITICAL**

All sportsbook names appear as "Scrambled" instead of actual names. We receive `SportsbookId` numbers (7, 8, 9, etc.) but no mapping to identify which is DraftKings, FanDuel, etc.

### 2. Soccer Not Available
**Severity: CRITICAL**

ALL soccer endpoints return 401 Unauthorized:
```json
{
  "HttpStatusCode": 401,
  "Code": 401,
  "Description": "Unauthorized Competition: You are not authorized to access this soccer competition."
}
```

### 3. 10-Minute Live Delay
**Severity: HIGH**

Free trial has 10-minute delay on live updates, making it unsuitable for live betting applications.

### 4. Hidden Pricing
**Severity: MEDIUM**

No public pricing - must contact sales. Enterprise APIs typically cost $500-2000+/month.

---

## Recommendation

### **KEEP The Odds API**

| Factor | Decision |
|--------|----------|
| Replace with SportsDataIO? | **NO** |
| Use both APIs? | **NO** (redundant, expensive) |
| Continue with The Odds API? | **YES** |

### Reasons to Keep The Odds API:

1. **Real Sportsbook Names** - Actually shows DraftKings, FanDuel, BetMGM, etc.
2. **Soccer 3-Way Betting** - Full support for Home/Draw/Away odds
3. **Missouri Compliance** - All 8 legal sportsbooks verified and working
4. **Transparent Pricing** - $25-75/month, no surprises
5. **Real-Time Data** - No artificial delays
6. **Proven Integration** - Already working in our platform

### Why NOT SportsDataIO:

1. Free trial is too limited to properly evaluate
2. Would need paid plan just to verify Missouri sportsbooks
3. Soccer not accessible - our platform REQUIRES 3-way betting
4. Unclear pricing - likely much more than The Odds API
5. 10-minute delay unacceptable for live betting
6. No clear advantage over The Odds API

---

## Action Items

1. **Continue using The Odds API** - No changes needed
2. **Do not purchase SportsDataIO** - Too many unknowns
3. **Monitor The Odds API usage** - Stay within $75/month Professional plan
4. **Re-evaluate in 6 months** - If SportsDataIO improves their free trial

---

## Test Service Created

A Java test service has been created at:
```
src/main/java/com/coltwarren/sports_betting_analytics/service/testing/SportsDataIOTestService.java
```

This service can be used to run additional tests if we ever want to re-evaluate.

---

## Sources

- [SportsDataIO Free Trial](https://sportsdata.io/free-trial)
- [SportsDataIO Live Odds API](https://sportsdata.io/live-odds-api)
- [SportsDataIO FAQ](https://sportsdata.io/developers/faq)
- [The Odds API](https://the-odds-api.com/)

---

**Report Generated:** January 27, 2026
**Recommendation:** Keep The Odds API ($25-75/mo)
