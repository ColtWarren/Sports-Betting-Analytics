# Sports Betting Analytics

AI-powered sports betting analytics platform with multi-factor recommendations, xG soccer analysis, pattern recognition, and backtesting capabilities.

## Features

- **Multi-Factor Recommendation Engine** - Combines 6 edge factors for unified betting advice
- **xG Soccer Analytics** - Expected Goals analysis with regression detection
- **Pattern Recognition** - Discovers profitable historical betting patterns
- **Historical Backtesting** - Test strategies with Kelly Criterion
- **Injury Analysis** - Player availability impact on spreads
- **Public Betting** - Contrarian opportunities when public is heavy on one side
- **Weather Intelligence** - Outdoor game conditions impact

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0+ (or H2 for development)

### Environment Variables

Create a `.env` file:

```bash
CLAUDE_API_KEY=your_claude_api_key
ODDS_API_KEY=your_odds_api_key
OPENWEATHERMAP_API_KEY=your_weather_api_key
```

### Run Locally

```bash
# Load environment variables
source .env

# Build and run
./mvnw spring-boot:run
```

### Access

- **Web UI**: http://localhost:8080
- **Best Bets**: http://localhost:8080/best-bets
- **Health**: http://localhost:8080/api/health
- **xG Info**: http://localhost:8080/api/xg/info

## API Endpoints

### Health & Monitoring
| Endpoint | Description |
|----------|-------------|
| `GET /api/health` | Comprehensive health check |
| `GET /api/health/live` | Kubernetes liveness probe |
| `GET /api/health/ready` | Kubernetes readiness probe |
| `GET /api/health/version` | Application version info |
| `POST /api/health/cache/clear` | Clear all caches |

### Best Bets
| Endpoint | Description |
|----------|-------------|
| `GET /best-bets` | Best bets dashboard (UI) |
| `GET /api/best-bets` | Best bets API |

### Recommendations
| Endpoint | Description |
|----------|-------------|
| `POST /api/recommendations/analyze` | Analyze single game |
| `POST /api/recommendations/analyze/batch` | Analyze multiple games |
| `GET /api/recommendations/test/epl` | Test EPL recommendations |
| `GET /api/recommendations/test/nfl` | Test NFL recommendations |
| `GET /api/recommendations/quick-summary` | Quick game summary |

### xG Analytics
| Endpoint | Description |
|----------|-------------|
| `GET /api/xg/team/{name}` | Team xG statistics |
| `GET /api/xg/match` | Match xG analysis |
| `GET /api/xg/regression-signals` | Teams due for regression |
| `GET /api/xg/test` | Test with sample matches |
| `GET /api/xg/info` | xG API documentation |

### Backtesting
| Endpoint | Description |
|----------|-------------|
| `POST /api/backtest/import/{league}` | Import historical data |
| `GET /api/backtest/run/{league}` | Run Kelly backtest |
| `GET /api/backtest/compare/{league}` | Compare strategies |
| `GET /api/backtest/stats` | Backtest statistics |

### Patterns
| Endpoint | Description |
|----------|-------------|
| `GET /api/patterns/discover/{league}` | Find profitable patterns |
| `GET /api/patterns/test/{league}` | Test pattern matching |
| `GET /api/patterns/leaderboard` | Pattern leaderboard |

### Weather
| Endpoint | Description |
|----------|-------------|
| `GET /api/test/weather/{sport}/{venue}` | Weather impact analysis |

### Injuries
| Endpoint | Description |
|----------|-------------|
| `GET /api/test/injuries/{sport}` | Sport injuries |
| `GET /api/test/injuries/{sport}/matchup` | Matchup comparison |

## Supported Sports & Leagues

| Sport | Leagues |
|-------|---------|
| NFL | National Football League |
| CFB | College Football (FBS) |
| NBA | National Basketball Association |
| CBB | College Basketball |
| NHL | National Hockey League |
| MLB | Major League Baseball |
| Soccer | EPL, La Liga, Bundesliga, Serie A, Ligue 1, MLS |

## Missouri-Legal Sportsbooks

DraftKings, FanDuel, BetMGM, Caesars, Fanatics, ESPN BET, Hard Rock, bet365

## Multi-Factor Engine

The recommendation engine combines 6 factors:

| Factor | Weight | Description |
|--------|--------|-------------|
| Pattern Recognition | 25% | Historical profitable patterns |
| Odds Value | 25% | Pure mathematical edge |
| xG Analysis | 15% | Expected Goals (soccer) |
| Injury Analysis | 15% | Player availability impact |
| Public Betting | 10% | Contrarian opportunities |
| Weather | 10% | Outdoor game conditions |

## xG Betting Edge

```
Team scoring MORE than xG = OVERPERFORMING = FADE (due for regression down)
Team scoring LESS than xG = UNDERPERFORMING = BET ON (due for positive regression)
```

## Docker Deployment

### Build and Run

```bash
# Build image
docker build -t sports-betting-analytics .

# Run with docker-compose
docker-compose up -d
```

### Environment Variables for Docker

```bash
ODDS_API_KEY=your_key
OPENWEATHERMAP_API_KEY=your_key
CLAUDE_API_KEY=your_key
DB_PASSWORD=your_db_password
```

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.x
- **Frontend**: Thymeleaf, JavaScript, CSS
- **Database**: MySQL / PostgreSQL
- **APIs**: The Odds API, OpenWeatherMap, ESPN, Claude AI
- **Caching**: In-memory with scheduled eviction

## Version History

| Version | Feature |
|---------|---------|
| v2.7.0 | xG Soccer Analytics |
| v2.6.0 | Multi-Factor Engine |
| v2.5.0 | Pattern Recognition |
| v2.4.0 | Historical Backtesting |
| v2.3.0 | Injury Tracking |
| v2.2.0 | Public Betting |
| v2.1.0 | Weather Intelligence |
| v2.0.0 | Soccer 3-Way Betting |

## Development Commands

```bash
# Run tests
./mvnw test

# Run single test
./mvnw test -Dtest=ClassName#methodName

# Build JAR
./mvnw clean package

# Build without tests
./mvnw clean package -DskipTests
```

## License

Private - All Rights Reserved
