import type { VegItem } from "@/lib/vegetables";

type Props = { veg: VegItem; size?: number };

export default function VegToken({ veg, size = 96 }: Props) {
  return (
    <svg width={size} height={size} viewBox="0 0 100 100" aria-hidden="true" style={{ display: "block" }}>
      <defs>
        <pattern id={`grain-${veg.id}`} x="0" y="0" width="3" height="3" patternUnits="userSpaceOnUse">
          <rect width="3" height="3" fill={veg.color} />
          <rect width="3" height="1" fill={veg.accent} opacity="0.08" />
        </pattern>
      </defs>
      <circle cx="50" cy="55" r="36" fill={`url(#grain-${veg.id})`} />
      <circle cx="50" cy="55" r="36" fill="none" stroke={veg.accent} strokeWidth="0.8" opacity="0.35" />
      <rect x="48" y="14" width="4" height="10" rx="1.5" fill={veg.accent} opacity="0.7" />
      <ellipse cx="56" cy="16" rx="7" ry="3" fill={veg.accent} opacity="0.55" transform="rotate(-20 56 16)" />
    </svg>
  );
}
