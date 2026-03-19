import { Badge } from "@/components/ui/badge";
import type { ConnectionStatus } from "@/services/websocketService";

const statusConfig: Record<
  ConnectionStatus,
  { label: string; variant: "success" | "warning" | "destructive" | "default"; pulse: boolean }
> = {
  connected: { label: "Live", variant: "success", pulse: true },
  connecting: { label: "Connecting", variant: "warning", pulse: true },
  disconnected: { label: "Disconnected", variant: "destructive", pulse: false },
  error: { label: "Error", variant: "destructive", pulse: false },
};

interface Props {
  status: ConnectionStatus;
}

export function ConnectionStatusBadge({ status }: Props) {
  const config = statusConfig[status];

  return (
    <Badge variant={config.variant} className="gap-1.5">
      <span
        className={`inline-block h-2 w-2 rounded-full bg-current ${config.pulse ? "animate-pulse" : ""}`}
      />
      {config.label}
    </Badge>
  );
}
