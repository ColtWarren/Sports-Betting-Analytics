# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AI-powered sports betting analytics platform built with Spring Boot 4.0.1 and Java 17. Integrates Claude AI for bet analysis, The Odds API for live sports data, ESPN API for game statistics, and weather data for outdoor sports.

## AI Behavior Rules

- Do not hallucinate APIs, endpoints, or SDKs
- Ask before introducing new dependencies
- Prefer correctness and clarity over cleverness
- Explain reasoning when making architectural changes
- Respect existing package structure

## Development Commands

### Running the Application
```bash
./mvnw spring-boot:run
```
Application runs on http://localhost:8080

### Building
```bash
./mvnw clean install
```

### Running Tests
```bash
./mvnw test
```

### Running a Single Test
```bash
./mvnw test -Dtest=ClassName#methodName
```

### Cleaning Build Artifacts
```bash
./mvnw clean
```

## Environment Setup

Required environment variables in `.env` file:
- `CLAUDE_API_KEY` - Anthropic Claude API key for AI-powered bet analysis
- `ODDS_API_KEY` - The Odds API key for fetching live sports odds

Database configuration in `application.properties`:
- MySQL database `betting_analytics` on localhost:3306
- Default credentials: root/root (configurable)
- Database auto-created if it doesn't exist

## Architecture

### Package Structure
```
com.coltwarren.sports_betting_analytics/
├── controller/          # REST endpoints and page controllers
├── service/             # Business logic layer
│   ├── ai/             # Claude AI integration services
│   ├── espn/           # ESPN API integration
│   └── odds/           # Odds API and betting analysis
├── repository/          # JPA repositories for data access
└── model/              # JPA entities and domain models
    └── odds/           # Odds-specific models
```

### Core Domain Models

**Bet Entity** (`model/Bet.java`)
- Central entity representing a single sports bet
- Tracks: sport, event, bet type, stake, odds, status (PENDING/WON/LOST/PUSH)
- Includes CLV (Closing Line Value) tracking
- Business methods: `markAsWon()`, `markAsLost()`, `markAsPush()`, `calculateCLV()`

**Bankroll Entity** (`model/Bankroll.java`)
- Manages betting bankroll with timestamps
- Used by Kelly Criterion calculations for optimal bet sizing

**GameStats Entity** (`model/GameStats.java`)
- Stores historical game statistics from ESPN
- Used for AI-powered matchup analysis

### Key Services

**ClaudeAIService** (`service/ai/ClaudeAIService.java`)
- Integrates with Claude API for bet analysis
- Provides EV (Expected Value) calculations with AI reasoning
- Uses WebClient for async communication
- Model configured: claude-sonnet-4-20250514

**BetService** (`service/BetService.java`)
- CRUD operations for bets
- Calculates portfolio statistics and performance metrics

**OddsService** (`service/odds/OddsService.java`)
- Fetches live odds from The Odds API
- Supports multiple sports

**BestBetsAnalyzer** (`service/odds/BestBetsAnalyzer.java`)
- Analyzes odds across sportsbooks to identify best value

**MultiSportBestBetsService** (`service/MultiSportBestBetsService.java`)
- Provides AI-powered best bet recommendations across all sports

**ActiveBetsTrackerService** (`service/ActiveBetsTrackerService.java`)
- Real-time tracking of active bets with live odds updates

**KellyCriterionService** (`service/KellyCriterionService.java`)
- Calculates optimal bet sizing using Kelly Criterion formula

**CLVTracker** (`service/CLVTracker.java`)
- Tracks Closing Line Value to measure bet timing quality

**AutoSettleService** (`service/AutoSettleService.java`)
- Automatically settles bets based on ESPN game results

**WeatherService** (`service/WeatherService.java`)
- Fetches weather data for outdoor sports analysis

### Frontend

Thymeleaf templates in `src/main/resources/templates/`:
- `dashboard.html` - Main dashboard with navigation
- `active-bets.html` - Real-time bet tracking
- `best-bets.html` - AI-recommended bets across sports
- `live-betting.html` - Live game betting interface
- `bet-form.html` - Bet entry form
- `bankroll.html` - Bankroll management
- `clv.html` - CLV analysis and tracking
- Dark cyberpunk theme with gradient effects

### Scheduled Tasks

Application uses `@EnableScheduling` for background jobs. Services with scheduled tasks use `@Scheduled` annotations for:
- Live odds updates
- Bet auto-settlement
- Game data synchronization

## Key Patterns

1. **Standard Spring MVC**: Controllers handle HTTP requests, delegate to services, services use repositories
2. **JPA Entities**: Use standard JPA annotations, Hibernate handles DDL with `ddl-auto=update`
3. **API Integration**: WebClient for async external API calls (Claude, Odds API, ESPN, Weather)
4. **American Odds Format**: All odds stored/displayed in American format (e.g., +150, -110)
5. **BigDecimal for Money**: All monetary values use BigDecimal for precision
6. **Lombok**: Project uses Lombok for boilerplate reduction (configured in maven compiler plugin)

## Database

- MySQL with Hibernate auto-update (development mode)
- SQL logging enabled (`show-sql=true`)
- Formatted SQL output for debugging
- Timezone set to UTC

## Important Notes

- Port 8080 is default (configurable in `application.properties`)
- Debug logging enabled for the application package
- Weather API key is hardcoded in properties (should be externalized)
- Application requires MySQL server running on localhost:3306
