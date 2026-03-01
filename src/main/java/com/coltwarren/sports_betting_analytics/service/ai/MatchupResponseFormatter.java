package com.coltwarren.sports_betting_analytics.service.ai;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MatchupResponseFormatter {

    @SuppressWarnings("unchecked")
    public String formatAnalysis(String analysis, Map<String, Object> weather,
                                 Map<String, Object> injuries, Map<String, Object> stats) {
        if (analysis == null || analysis.isEmpty()) {
            return "Analysis unavailable";
        }

        StringBuilder formatted = new StringBuilder();

        // Add weather box if available
        if ((Boolean) weather.get("available")) {
            formatted.append(String.format("""
                <div style='background: rgba(59, 130, 246, 0.15); border: 2px solid rgba(59, 130, 246, 0.4); \
                            border-radius: 12px; padding: 15px; margin-bottom: 20px;'>
                    <h3 style='margin: 0 0 10px 0; color: #93c5fd;'>%s LIVE WEATHER - %s</h3>
                    <div style='display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px;'>
                        <div><strong>Temp:</strong> %d°F (feels %d°F)</div>
                        <div><strong>Wind:</strong> %d mph %s</div>
                        <div><strong>Condition:</strong> %s</div>
                        <div><strong>Humidity:</strong> %d%%</div>
                    </div>
                    <div style='margin-top: 10px; padding: 10px; background: rgba(0,0,0,0.2); border-radius: 8px;'>
                        <strong>📊 Impact:</strong> %s
                    </div>
                </div>
                """,
                weather.get("icon"),
                weather.get("city"),
                weather.get("temperature"),
                weather.get("feelsLike"),
                weather.get("windSpeed"),
                weather.get("windDirection"),
                weather.get("condition"),
                weather.get("humidity"),
                weather.get("bettingImpact")
            ));
        }

        // Add injury box if available
        if ((Boolean) injuries.get("available")) {
            List<Map<String, Object>> team1Injuries = (List<Map<String, Object>>) injuries.get("team1Injuries");
            List<Map<String, Object>> team2Injuries = (List<Map<String, Object>>) injuries.get("team2Injuries");

            StringBuilder injuryHtml = new StringBuilder();
            injuryHtml.append(String.format("""
                <div style='background: rgba(239, 68, 68, 0.15); border: 2px solid rgba(239, 68, 68, 0.4); \
                            border-radius: 12px; padding: 15px; margin-bottom: 20px;'>
                    <h3 style='margin: 0 0 10px 0; color: #fca5a5;'>🏥 LIVE INJURY REPORT (ESPN)</h3>
                    <div style='padding: 10px; background: rgba(0,0,0,0.2); border-radius: 8px; margin-bottom: 10px;'>
                        <strong>Summary:</strong> %s
                    </div>
                """, injuries.get("impactSummary")));

            if (!team1Injuries.isEmpty() || !team2Injuries.isEmpty()) {
                injuryHtml.append("<div style='display: grid; grid-template-columns: 1fr 1fr; gap: 15px;'>");

                // Team 1 injuries
                injuryHtml.append(String.format("<div><strong>%s:</strong><br>", injuries.get("team1")));
                if (team1Injuries.isEmpty()) {
                    injuryHtml.append("<span style='color: #6ee7b7;'>✅ No major injuries</span>");
                } else {
                    for (Map<String, Object> injury : team1Injuries) {
                        injuryHtml.append(String.format("%s %s (%s) - <em>%s</em><br>",
                            injury.get("icon"),
                            injury.get("name"),
                            injury.get("position"),
                            injury.get("status")
                        ));
                    }
                }
                injuryHtml.append("</div>");

                // Team 2 injuries
                injuryHtml.append(String.format("<div><strong>%s:</strong><br>", injuries.get("team2")));
                if (team2Injuries.isEmpty()) {
                    injuryHtml.append("<span style='color: #6ee7b7;'>✅ No major injuries</span>");
                } else {
                    for (Map<String, Object> injury : team2Injuries) {
                        injuryHtml.append(String.format("%s %s (%s) - <em>%s</em><br>",
                            injury.get("icon"),
                            injury.get("name"),
                            injury.get("position"),
                            injury.get("status")
                        ));
                    }
                }
                injuryHtml.append("</div>");

                injuryHtml.append("</div>");
            }

            injuryHtml.append("</div>");
            formatted.append(injuryHtml);
        }

        // Add stats box if available
        if ((Boolean) stats.get("available")) {
            Map<String, Object> team1Stats = (Map<String, Object>) stats.get("team1Stats");
            Map<String, Object> team2Stats = (Map<String, Object>) stats.get("team2Stats");

            formatted.append(String.format("""
                <div style='background: rgba(34, 197, 94, 0.15); border: 2px solid rgba(34, 197, 94, 0.4); \
                            border-radius: 12px; padding: 15px; margin-bottom: 20px;'>
                    <h3 style='margin: 0 0 10px 0; color: #86efac;'>📊 VERIFIED STATS (YOUR DATABASE)</h3>
                    <div style='padding: 10px; background: rgba(0,0,0,0.2); border-radius: 8px; margin-bottom: 10px;'>
                        <strong>Summary:</strong> %s
                    </div>
                    <div style='display: grid; grid-template-columns: 1fr 1fr; gap: 15px;'>
                        <div>
                            <strong>%s:</strong><br>
                            ATS: %s (%.1f%%)<br>
                            O/U: %s<br>
                            Recent: %s
                        </div>
                        <div>
                            <strong>%s:</strong><br>
                            ATS: %s (%.1f%%)<br>
                            O/U: %s<br>
                            Recent: %s
                        </div>
                    </div>
                </div>
                """,
                stats.get("summary"),
                stats.get("team1"),
                team1Stats.get("atsRecord"),
                team1Stats.get("atsPercentage"),
                team1Stats.get("ouRecord"),
                team1Stats.get("recentATS"),
                stats.get("team2"),
                team2Stats.get("atsRecord"),
                team2Stats.get("atsPercentage"),
                team2Stats.get("ouRecord"),
                team2Stats.get("recentATS")
            ));
        }

        // Add the main analysis with section headers
        formatted.append(analysis
            .replace("KEY FACTORS:", "<h3>🎯 KEY FACTORS:</h3>")
            .replace("TRENDS:", "<h3>📊 TRENDS:</h3>")
            .replace("LINE VALUE ASSESSMENT:", "<h3>💎 LINE VALUE ASSESSMENT:</h3>")
            .replace("CONFIDENCE:", "<h3>📈 CONFIDENCE:</h3>")
            .replace("RECOMMENDATION:", "<h3>💡 RECOMMENDATION:</h3>")
            .replace("\n", "<br>"));

        return formatted.toString();
    }
}
