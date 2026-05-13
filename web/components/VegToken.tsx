import type { VegItem } from "@/lib/vegetables";

type Props = { veg: VegItem; size?: number };

export default function VegToken({ veg, size = 96 }: Props) {
  return (
    <svg width={size} height={size} viewBox="0 0 100 100" aria-hidden="true" style={{ display: "block" }}>
      {/*
        Avoid SVG <pattern> here — patterns require a unique DOM id, but the
        same vegetable token can appear in both the grid card AND the open
        detail panel at the same time, which would create duplicate ids.
        A flat fill + subtle accent overlay achieves the same visual result.
      */}
      <circle cx="50" cy="55" r="36" fill={veg.color} />
      <circle cx="50" cy="55" r="36" fill={veg.accent} opacity="0.05" />
      <circle cx="50" cy="55" r="36" fill="none" stroke={veg.accent} strokeWidth="0.8" opacity="0.35" />
      <rect x="48" y="14" width="4" height="10" rx="1.5" fill={veg.accent} opacity="0.7" />
      <ellipse cx="56" cy="16" rx="7" ry="3" fill={veg.accent} opacity="0.55" transform="rotate(-20 56 16)" />
    </svg>
  );
}
