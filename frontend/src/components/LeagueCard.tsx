import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import type { LeagueSummary } from "@/types/websocket";

const leagueLabels: Record<string, string> = {
  americanfootball_nfl: "NFL",
  americanfootball_ncaaf: "NCAAF",
  basketball_nba: "NBA",
  basketball_wnba: "WNBA",
  basketball_ncaab: "NCAAB",
  basketball_wncaab: "WCBB",
  icehockey_nhl: "NHL",
  baseball_mlb: "MLB",
  soccer_epl: "EPL",
  soccer_usa_mls: "MLS",
  soccer_uefa_champs_league: "UCL",
  soccer_spain_la_liga: "La Liga",
  soccer_italy_serie_a: "Serie A",
  soccer_france_ligue_one: "Ligue 1",
  soccer_germany_bundesliga: "Bundesliga",
};

function formatLeague(key: string): string {
  return leagueLabels[key] ?? key;
}

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString(undefined, {
      month: "short",
      day: "numeric",
      hour: "numeric",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}

interface Props {
  league: LeagueSummary;
}

export function LeagueCard({ league }: Props) {
  return (
    <Card className="hover:border-primary/50 transition-colors">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg">{formatLeague(league.league)}</CardTitle>
          <Badge variant="outline">{league.eventCount} events</Badge>
        </div>
      </CardHeader>
      <CardContent>
        <ul className="space-y-2">
          {league.events.map((event) => (
            <li
              key={event.eventId}
              className="flex items-center justify-between rounded-lg bg-background px-3 py-2 text-sm"
            >
              <div className="min-w-0 flex-1">
                <span className="text-text">{event.awayTeam}</span>
                <span className="text-text-muted mx-1.5">@</span>
                <span className="text-text">{event.homeTeam}</span>
              </div>
              <div className="ml-3 flex shrink-0 items-center gap-2 text-xs text-text-muted">
                <span>{formatTime(event.commenceTime)}</span>
                <Badge variant="secondary" className="text-[10px] px-1.5 py-0">
                  {event.bookmakersCount} books
                </Badge>
              </div>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}
