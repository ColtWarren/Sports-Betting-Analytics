package com.coltwarren.sports_betting_analytics.service.ai;

import com.coltwarren.sports_betting_analytics.service.wnba.WNBADataService;
import com.coltwarren.sports_betting_analytics.service.college.WNCAAWDataService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MatchupPromptBuilder {

    private final WNBADataService wnbaDataService;
    private final WNCAAWDataService wcbbDataService;

    public MatchupPromptBuilder(WNBADataService wnbaDataService, WNCAAWDataService wcbbDataService) {
        this.wnbaDataService = wnbaDataService;
        this.wcbbDataService = wcbbDataService;
    }

    @SuppressWarnings("unchecked")
    public String buildAnalysisPrompt(String game, String betType, String selection,
                                      int bestOdds, int worstOdds, double valuePoints,
                                      Map<String, Object> weather,
                                      Map<String, Object> injuries,
                                      Map<String, Object> stats) {

        StringBuilder weatherInfo = new StringBuilder();
        if ((Boolean) weather.get("available")) {
            weatherInfo.append(String.format("""

                LIVE WEATHER DATA:
                Temperature: %d°F (Feels like %d°F)
                Wind: %d mph %s
                Condition: %s
                Humidity: %d%%
                Betting Impact: %s
                """,
                weather.get("temperature"),
                weather.get("feelsLike"),
                weather.get("windSpeed"),
                weather.get("windDirection"),
                weather.get("condition"),
                weather.get("humidity"),
                weather.get("bettingImpact")
            ));
        }

        StringBuilder injuryInfo = new StringBuilder();
        if ((Boolean) injuries.get("available")) {
            injuryInfo.append("\nLIVE INJURY REPORT (ESPN):\n");
            injuryInfo.append(String.format("Summary: %s\n", injuries.get("impactSummary")));

            List<Map<String, Object>> team1Injuries = (List<Map<String, Object>>) injuries.get("team1Injuries");
            List<Map<String, Object>> team2Injuries = (List<Map<String, Object>>) injuries.get("team2Injuries");

            if (!team1Injuries.isEmpty()) {
                injuryInfo.append(String.format("\n%s Injuries:\n", injuries.get("team1")));
                for (Map<String, Object> injury : team1Injuries) {
                    injuryInfo.append(String.format("  %s %s (%s) - %s: %s\n",
                        injury.get("icon"),
                        injury.get("name"),
                        injury.get("position"),
                        injury.get("status"),
                        injury.get("type")
                    ));
                }
            }

            if (!team2Injuries.isEmpty()) {
                injuryInfo.append(String.format("\n%s Injuries:\n", injuries.get("team2")));
                for (Map<String, Object> injury : team2Injuries) {
                    injuryInfo.append(String.format("  %s %s (%s) - %s: %s\n",
                        injury.get("icon"),
                        injury.get("name"),
                        injury.get("position"),
                        injury.get("status"),
                        injury.get("type")
                    ));
                }
            }
        }

        StringBuilder statsInfo = new StringBuilder();
        if ((Boolean) stats.get("available")) {
            statsInfo.append("\nVERIFIED HISTORICAL STATS:\n");
            statsInfo.append(String.format("Summary: %s\n", stats.get("summary")));

            Map<String, Object> team1Stats = (Map<String, Object>) stats.get("team1Stats");
            Map<String, Object> team2Stats = (Map<String, Object>) stats.get("team2Stats");

            if ((Boolean) team1Stats.get("available")) {
                statsInfo.append(String.format("\n%s Stats:\n", stats.get("team1")));
                statsInfo.append(String.format("  ATS Record: %s (%.1f%%)\n",
                    team1Stats.get("atsRecord"), team1Stats.get("atsPercentage")));
                statsInfo.append(String.format("  O/U Record: %s (%.1f%% Over)\n",
                    team1Stats.get("ouRecord"), team1Stats.get("overPercentage")));
                statsInfo.append(String.format("  Recent ATS: %s\n", team1Stats.get("recentATS")));
            }

            if ((Boolean) team2Stats.get("available")) {
                statsInfo.append(String.format("\n%s Stats:\n", stats.get("team2")));
                statsInfo.append(String.format("  ATS Record: %s (%.1f%%)\n",
                    team2Stats.get("atsRecord"), team2Stats.get("atsPercentage")));
                statsInfo.append(String.format("  O/U Record: %s (%.1f%% Over)\n",
                    team2Stats.get("ouRecord"), team2Stats.get("overPercentage")));
                statsInfo.append(String.format("  Recent ATS: %s\n", team2Stats.get("recentATS")));
            }
        }

        StringBuilder womensBasketballInfo = new StringBuilder();
        String detectedSport = detectSportFromGame(game);
        if ("WNBA".equals(detectedSport)) {
            womensBasketballInfo.append(buildWNBAContext(game));
        } else if ("WCBB".equals(detectedSport)) {
            womensBasketballInfo.append(buildWCBBContext(game));
        }

        return String.format("""
            You are a professional sports betting analyst. Analyze this betting opportunity:

            GAME: %s
            BET TYPE: %s
            SELECTION: %s
            BEST ODDS: %s (best available)
            WORST ODDS: %s (worst available)
            MARKET VALUE: %.0f points (spread between books)
            %s%s%s%s

            Provide a detailed matchup analysis in this format:

            KEY FACTORS:
            - List 3-5 important factors (injuries, trends, matchups, weather)
            - Use ✅ for factors favoring the bet
            - Use ⚠️ for concerns
            - Incorporate ALL live data provided above (weather, injuries, verified stats)

            TRENDS:
            - Use the VERIFIED STATS provided above (don't guess!)
            - If stats unavailable, note that data is limited

            LINE VALUE ASSESSMENT:
            - Is this line value strong, fair, or weak?
            - How does %s odds compare to market?

            CONFIDENCE: [HIGH/MEDIUM-HIGH/MEDIUM/MEDIUM-LOW/LOW]

            RECOMMENDATION:
            - 2-3 sentences summarizing your analysis
            - Should this bet be placed based on the value and factors?
            - What's the main risk?

            Keep it concise, actionable, and data-focused. Only use verified statistics provided above.
            """,
            game, betType, selection,
            formatOdds(bestOdds), formatOdds(worstOdds), valuePoints,
            weatherInfo.toString(),
            injuryInfo.toString(),
            statsInfo.toString(),
            womensBasketballInfo.toString(),
            formatOdds(bestOdds)
        );
    }

    String detectSportFromGame(String game) {
        if (game == null) return "";
        String gameLower = game.toLowerCase();

        List<String> wnbaTeams = List.of(
            "aces", "liberty", "sun", "lynx", "storm", "mercury",
            "sky", "fever", "sparks", "wings", "dream", "mystics",
            "las vegas", "new york liberty", "connecticut sun", "minnesota lynx",
            "seattle storm", "phoenix mercury", "chicago sky", "indiana fever",
            "los angeles sparks", "dallas wings", "atlanta dream", "washington mystics"
        );
        for (String team : wnbaTeams) {
            if (gameLower.contains(team)) return "WNBA";
        }

        if (gameLower.contains("wcbb") || gameLower.contains("women's") ||
            gameLower.contains("(w)") || gameLower.contains("wbb")) {
            return "WCBB";
        }

        return "";
    }

    private String buildWNBAContext(String game) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("""

            WOMEN'S BASKETBALL (WNBA) MARKET INSIGHT:
            - WNBA betting markets are LESS EFFICIENT than NBA - edges are larger and more frequent
            - Star player availability is critical (A'ja Wilson, Caitlin Clark, Breanna Stewart shift lines 3-5 pts)
            - Shorter 40-game season - rest days and back-to-backs matter significantly more than NBA
            - 12-team league means team chemistry and roster continuity are outsized factors
            """);

        try {
            String[] parts = game.split("@|vs|VS");
            if (parts.length == 2) {
                String team1 = parts[0].trim();
                String team2 = parts[1].trim();

                Map<String, Object> stats1 = wnbaDataService.getTeamStats(team1);
                Map<String, Object> stats2 = wnbaDataService.getTeamStats(team2);

                if (stats1 != null) {
                    ctx.append(String.format("WNBA DATA - %s: %.1f PPG, ATS %d-%d (%.1f%%)\n",
                        team1, stats1.get("pointsPerGame"),
                        stats1.get("atsWins"), stats1.get("atsLosses"), stats1.get("atsPercentage")));
                }
                if (stats2 != null) {
                    ctx.append(String.format("WNBA DATA - %s: %.1f PPG, ATS %d-%d (%.1f%%)\n",
                        team2, stats2.get("pointsPerGame"),
                        stats2.get("atsWins"), stats2.get("atsLosses"), stats2.get("atsPercentage")));
                }
            }
        } catch (Exception e) {
            // Stats unavailable - context still useful without them
        }

        return ctx.toString();
    }

    @SuppressWarnings("unchecked")
    private String buildWCBBContext(String game) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("""

            WOMEN'S COLLEGE BASKETBALL (WCBB) MARKET INSIGHT:
            - WCBB betting markets are SIGNIFICANTLY less efficient than men's CBB - largest edges in basketball
            - Dominant programs (South Carolina, UConn) create lopsided matchups - watch for inflated lines
            - Historic powerhouses (UConn, Tennessee) attract disproportionate public money
            - Conference strength varies widely - cross-conference matchups are often mispriced
            - Tournament seed implications drive late-season motivation differences
            - The Caitlin Clark effect has increased betting volume but lines remain soft
            """);

        try {
            int season = wcbbDataService.getCurrentSeason();
            List<Map<String, Object>> ratings = wcbbDataService.getTeamRatings(season);

            String[] parts = game.split("@|vs|VS");
            if (parts.length == 2) {
                String team1 = parts[0].trim();
                String team2 = parts[1].trim();

                for (Map<String, Object> rating : ratings) {
                    String teamName = (String) rating.get("team");
                    if (teamName != null && (team1.contains(teamName) || teamName.contains(team1) ||
                                             team2.contains(teamName) || teamName.contains(team2))) {
                        ctx.append(String.format("WCBB DATA - %s: SRS %.1f, Conference: %s\n",
                            teamName, rating.get("srs"), rating.get("conference")));
                    }
                }
            }
        } catch (Exception e) {
            // Stats unavailable - context still useful without them
        }

        return ctx.toString();
    }

    String formatOdds(int odds) {
        return odds > 0 ? "+" + odds : String.valueOf(odds);
    }
}
