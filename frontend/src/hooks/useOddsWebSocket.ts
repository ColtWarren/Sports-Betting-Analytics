import { useEffect, useRef, useState } from "react";
import { websocketService, type ConnectionStatus } from "@/services/websocketService";
import type { OddsUpdate } from "@/types/websocket";

export function useOddsWebSocket() {
  const [latestUpdate, setLatestUpdate] = useState<OddsUpdate | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>("disconnected");
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const connected = useRef(false);

  useEffect(() => {
    if (connected.current) return;
    connected.current = true;

    websocketService.connect(
      (update) => {
        setLatestUpdate(update);
        setLastUpdated(new Date());
      },
      setConnectionStatus
    );

    return () => {
      connected.current = false;
      websocketService.disconnect();
    };
  }, []);

  return { latestUpdate, connectionStatus, lastUpdated };
}
