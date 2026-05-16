import type { VegStatus } from "@/lib/vegetables";

const STATUS_META: Record<VegStatus, { label: string; dot: string; fg: string }> = {
  peak:     { label: "Op hoogtepunt",    dot: "var(--peak)",    fg: "var(--peak-fg)" },
  soon:     { label: "Binnenkort",       dot: "var(--soon)",    fg: "var(--soon-fg)" },
  ending:   { label: "Aflopend",         dot: "var(--ending)",  fg: "var(--ending-fg)" },
  shoulder: { label: "Beschikbaar",      dot: "var(--primary-soft)", fg: "var(--primary)" },
  off:      { label: "Buiten seizoen",   dot: "var(--muted-2)",      fg: "var(--ink-soft)" },
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
