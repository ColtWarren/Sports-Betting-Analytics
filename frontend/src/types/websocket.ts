export interface OddsEvent {
  eventId: string;
  sportKey: string;
  homeTeam: string;
  awayTeam: string;
  commenceTime: string;
  bookmakersCount: number;
}

export interface LeagueSummary {
  league: string;
  eventCount: number;
  events: OddsEvent[];
}

export interface OddsUpdate {
  type: string;
  timestamp: string;
  leagueCount: number;
  totalEvents: number;
  leagues: LeagueSummary[];
}
