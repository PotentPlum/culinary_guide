import type { VegStatus } from "@/lib/vegetables";

const STATUS_META: Record<VegStatus, { label: string; dot: string; fg: string }> = {
  peak:     { label: "In peak",       dot: "var(--peak)",    fg: "var(--peak-fg)" },
  soon:     { label: "Coming soon",   dot: "var(--soon)",    fg: "var(--soon-fg)" },
  ending:   { label: "Ending",        dot: "var(--ending)",  fg: "var(--ending-fg)" },
  shoulder: { label: "Available",     dot: "var(--primary-soft)", fg: "var(--primary)" },
  off:      { label: "Out of season", dot: "var(--muted-2)",      fg: "var(--ink-soft)" },
};

type Props = { status: VegStatus; withLabel?: boolean };

export default function StatusDot({ status, withLabel = false }: Props) {
  const meta = STATUS_META[status];
  return (
    <span className="status-dot" style={{ color: meta.fg }}>
      <span className="dot" style={{ background: meta.dot }} />
      {withLabel && <span>{meta.label}</span>}
    </span>
  );
}
