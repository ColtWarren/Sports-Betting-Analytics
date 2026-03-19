import { useOddsWebSocket } from "@/hooks/useOddsWebSocket";
import { ConnectionStatusBadge } from "@/components/ConnectionStatusBadge";
import { LeagueCard } from "@/components/LeagueCard";

function formatTimestamp(ts: string): string {
  try {
    return new Date(ts).toLocaleTimeString();
  } catch {
    return ts;
  }
}

export function DashboardPage() {
  const { latestUpdate, connectionStatus, lastUpdated } = useOddsWebSocket();

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-border px-6 py-4">
        <div className="mx-auto flex max-w-7xl items-center justify-between">
          <h1 className="text-2xl font-bold">
            <span className="bg-gradient-to-r from-primary via-secondary to-accent bg-clip-text text-transparent">
              Live Odds Feed
            </span>
          </h1>
          <div className="flex items-center gap-4">
            {lastUpdated && (
              <span className="text-sm text-text-muted">
                Updated {lastUpdated.toLocaleTimeString()}
              </span>
            )}
            <ConnectionStatusBadge status={connectionStatus} />
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-6 py-6">
        {latestUpdate && (
          <div className="mb-6 flex flex-wrap gap-4 rounded-lg border border-border bg-surface px-4 py-3 text-sm text-text-muted">
            <span>
              <strong className="text-text">{latestUpdate.leagueCount}</strong> leagues
            </span>
            <span>
              <strong className="text-text">{latestUpdate.totalEvents}</strong> events
            </span>
            <span>Broadcast at {formatTimestamp(latestUpdate.timestamp)}</span>
          </div>
        )}

        {connectionStatus === "connecting" && !latestUpdate && (
          <div className="py-20 text-center text-text-muted">
            <p className="text-lg">Connecting to live odds feed...</p>
          </div>
        )}

        {connectionStatus === "connected" && !latestUpdate && (
          <div className="py-20 text-center text-text-muted">
            <p className="text-lg">Waiting for odds broadcast...</p>
            <p className="mt-2 text-sm">
              Odds are broadcast every 15 minutes, or on server startup.
            </p>
          </div>
        )}

        {connectionStatus === "disconnected" && !latestUpdate && (
          <div className="py-20 text-center text-text-muted">
            <p className="text-lg">Disconnected from live odds feed</p>
            <p className="mt-2 text-sm">
              Check that the backend is running and you are logged in at{" "}
              <a href="/login" className="text-primary hover:text-primary-hover underline">
                /login
              </a>.
            </p>
          </div>
        )}

        {connectionStatus === "error" && (
          <div className="py-20 text-center text-error">
            <p className="text-lg">Connection error</p>
            <p className="mt-2 text-sm text-text-muted">
              Check that you are logged in and the backend is running.
            </p>
          </div>
        )}

        {latestUpdate && latestUpdate.leagues.length > 0 && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {latestUpdate.leagues.map((league) => (
              <LeagueCard key={league.league} league={league} />
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
