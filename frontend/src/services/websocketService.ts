import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { OddsUpdate } from "@/types/websocket";

export type ConnectionStatus = "disconnected" | "connecting" | "connected" | "error";

type OddsCallback = (update: OddsUpdate) => void;
type StatusCallback = (status: ConnectionStatus) => void;

function getCsrfToken(): string | undefined {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : undefined;
}

class WebSocketService {
  private client: Client | null = null;

  connect(onOddsUpdate: OddsCallback, onStatus: StatusCallback): void {
    if (this.client?.active) return;

    onStatus("connecting");

    const csrfToken = getCsrfToken();
    const connectHeaders: Record<string, string> = {};
    if (csrfToken) {
      connectHeaders["X-XSRF-TOKEN"] = csrfToken;
    }

    try {
      this.client = new Client({
        webSocketFactory: () => new SockJS("/ws"),
        connectHeaders,
        reconnectDelay: 5000,
        onConnect: () => {
          onStatus("connected");
          this.client?.subscribe("/topic/odds", (message) => {
            const update: OddsUpdate = JSON.parse(message.body);
            onOddsUpdate(update);
          });
        },
        onStompError: (frame) => {
          console.error("STOMP error:", frame.headers["message"]);
          onStatus("error");
        },
        onWebSocketClose: () => {
          onStatus("disconnected");
        },
      });

      this.client.activate();
    } catch (err) {
      console.error("WebSocket connection failed:", err);
      onStatus("error");
    }
  }

  disconnect(): void {
    if (this.client?.active) {
      this.client.deactivate();
      this.client = null;
    }
  }
}

export const websocketService = new WebSocketService();
