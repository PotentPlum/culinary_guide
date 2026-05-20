import type { VegItem } from "@/lib/vegetables";

type Props = { veg: VegItem; size?: number };

export default function VegToken({ veg, size = 96 }: Props) {
  return (
    <svg width={size} height={size} viewBox="0 0 100 100" aria-hidden="true" style={{ display: "block" }}>
      {/*
        Avoid SVG <pattern> here — patterns require a unique DOM id, but the
        same vegetable token can appear in both the grid card AND the open
        detail panel at the same time, which would create duplicate ids.
      */}
      <circle cx="50" cy="58" r="34" fill={veg.accent} opacity="0.07" />
      <circle cx="50" cy="54" r="34" fill={veg.color} />
      <ellipse cx="42" cy="43" rx="12" ry="8" fill="rgba(255,255,255,0.13)" transform="rotate(-15 42 43)" />
      <circle cx="50" cy="54" r="34" fill={veg.accent} opacity="0.04" />
      <circle cx="50" cy="54" r="34" fill="none" stroke={veg.accent} strokeWidth="0.8" opacity="0.32" />
      <circle cx="50" cy="54" r="38" fill="none" stroke={veg.accent} strokeWidth="0.5" opacity="0.10" />
      <rect x="48.5" y="14" width="3" height="10" rx="1.5" fill={veg.accent} opacity="0.65" />
      <ellipse cx="56" cy="16" rx="7.5" ry="3" fill={veg.accent} opacity="0.50" transform="rotate(-25 56 16)" />
      <ellipse cx="43" cy="15" rx="5.5" ry="2.5" fill={veg.accent} opacity="0.35" transform="rotate(22 43 15)" />
    </svg>
  );
}
