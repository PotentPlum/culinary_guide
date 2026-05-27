"use client";

export type RadarAxis = {
  label: string;   // Dutch label e.g. "Thermische Controle"
  frequency: number;   // 0–1 normalized
  proficiency: number; // 0–1 normalized
};

type Props = {
  axes: RadarAxis[];   // exactly 4 axes for 4 disciplines
  size?: number;       // SVG size in px, default 300
};

// Convert polar coordinates to Cartesian
function polarToCartesian(
  cx: number,
  cy: number,
  radius: number,
  value: number,
  angleDeg: number,
): [number, number] {
  const angleRad = (angleDeg * Math.PI) / 180;
  return [
    cx + radius * value * Math.cos(angleRad),
    cy + radius * value * Math.sin(angleRad),
  ];
}

// Build an SVG polygon points string from an array of [x, y] pairs
function pointsStr(pts: [number, number][]): string {
  return pts.map(([x, y]) => `${x.toFixed(3)},${y.toFixed(3)}`).join(" ");
}

// Angles for 4 axes: top, right, bottom, left
const AXIS_ANGLES = [-90, 0, 90, 180]; // degrees

export default function RadarChart({ axes, size = 300 }: Props) {
  // Guard: we require exactly 4 axes but render gracefully with whatever is given
  const safeAxes = axes.slice(0, 4);
  while (safeAxes.length < 4) {
    safeAxes.push({ label: "", frequency: 0, proficiency: 0 });
  }

  const cx = size / 2;
  const cy = size / 2;
  const maxRadius = size * 0.38; // ~38% of size leaves room for labels

  // Grid ring levels
  const gridLevels = [0.25, 0.5, 0.75, 1.0];

  // Build grid polygon points for each level
  function gridPolygonPoints(level: number): string {
    const pts = AXIS_ANGLES.map(
      (angle) => polarToCartesian(cx, cy, maxRadius, level, angle),
    );
    return pointsStr(pts);
  }

  // Axis tip points (for axis lines)
  const axisTips = AXIS_ANGLES.map((angle) =>
    polarToCartesian(cx, cy, maxRadius, 1, angle),
  );

  // Frequency data polygon
  const freqPoints = safeAxes.map((axis, i) =>
    polarToCartesian(cx, cy, maxRadius, Math.max(0, Math.min(1, axis.frequency)), AXIS_ANGLES[i]),
  );

  // Proficiency data polygon
  const profPoints = safeAxes.map((axis, i) =>
    polarToCartesian(cx, cy, maxRadius, Math.max(0, Math.min(1, axis.proficiency)), AXIS_ANGLES[i]),
  );

  // Label offset — push labels slightly beyond the axis tip
  const labelOffset = maxRadius + 18;

  return (
    <svg
      className="radar-chart"
      viewBox={`0 0 ${size} ${size}`}
      width={size}
      height={size}
      role="img"
      aria-label="Radar diagram"
    >
      {/* Grid polygons */}
      {gridLevels.map((level) => (
        <polygon
          key={level}
          className="radar-grid-line"
          points={gridPolygonPoints(level)}
        />
      ))}

      {/* Axis lines from center to each tip */}
      {axisTips.map(([x, y], i) => (
        <line
          key={i}
          className="radar-axis-line"
          x1={cx}
          y1={cy}
          x2={x}
          y2={y}
        />
      ))}

      {/* Frequency fill polygon */}
      <polygon
        className="radar-overlay-freq"
        points={pointsStr(freqPoints)}
      />

      {/* Proficiency stroke polygon */}
      <polygon
        className="radar-overlay-prof"
        points={pointsStr(profPoints)}
      />

      {/* Data point dots — frequency */}
      {freqPoints.map(([x, y], i) => (
        <circle
          key={`freq-dot-${i}`}
          className="radar-dot radar-dot--freq"
          cx={x}
          cy={y}
          r={4}
        />
      ))}

      {/* Data point dots — proficiency */}
      {profPoints.map(([x, y], i) => (
        <circle
          key={`prof-dot-${i}`}
          className="radar-dot radar-dot--prof"
          cx={x}
          cy={y}
          r={3.5}
        />
      ))}

      {/* Axis labels */}
      {safeAxes.map((axis, i) => {
        const angle = AXIS_ANGLES[i];
        const [tx, ty] = polarToCartesian(cx, cy, labelOffset, 1, angle);

        // Anchor and baseline based on quadrant
        let textAnchor: "inherit" | "middle" | "start" | "end" = "middle";
        let dominantBaseline: "inherit" | "auto" | "middle" | "hanging" | "text-bottom" | "text-top" | "alphabetic" | "ideographic" | "mathematical" = "middle";

        if (angle === -90) {
          // Top
          textAnchor = "middle";
          dominantBaseline = "auto";
        } else if (angle === 0) {
          // Right
          textAnchor = "start";
          dominantBaseline = "middle";
        } else if (angle === 90) {
          // Bottom
          textAnchor = "middle";
          dominantBaseline = "hanging";
        } else if (angle === 180) {
          // Left
          textAnchor = "end";
          dominantBaseline = "middle";
        }

        return (
          <text
            key={`label-${i}`}
            className="radar-label"
            x={tx}
            y={ty}
            textAnchor={textAnchor}
            dominantBaseline={dominantBaseline}
          >
            {axis.label}
          </text>
        );
      })}
    </svg>
  );
}
