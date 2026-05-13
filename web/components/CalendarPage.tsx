import type { VegItem } from "@/lib/vegetables";
import { VEG_DATA, MONTHS, MONTHS_LONG } from "@/lib/vegetables";

type Props = { month: number; onOpen: (v: VegItem) => void };

/**
 * Returns the calendar month where a vegetable's season begins, accounting
 * for seasons that wrap across the December→January boundary.
 *
 * Strategy: find the largest gap between consecutive peak months (including
 * the wraparound gap from the last month back to the first). The season
 * starts at the month immediately after that gap.
 *
 * Examples:
 *   kale  [10,11,12,1,2,3] → largest gap is 3→10 (7 months) → starts Oct (10)
 *   kale was sorting to Jan (1) with Math.min, which was wrong.
 */
function seasonStart(peak: number[]): number {
  const sorted = [...peak].sort((a, b) => a - b);
  let maxGap = 0;
  let start = sorted[0];
  for (let i = 0; i < sorted.length; i++) {
    const nextIdx = (i + 1) % sorted.length;
    // Wraparound gap: distance from last month back around to the first.
    const gap = nextIdx === 0
      ? sorted[0] + 12 - sorted[i]
      : sorted[nextIdx] - sorted[i];
    if (gap > maxGap) {
      maxGap = gap;
      start = nextIdx === 0 ? sorted[0] : sorted[nextIdx];
    }
  }
  return start;
}

export default function CalendarPage({ month, onOpen }: Props) {
  const sorted = [...VEG_DATA].sort(
    (a, b) => seasonStart(a.peak) - seasonStart(b.peak) || a.name.localeCompare(b.name)
  );

  return (
    <div className="page calendar">
      <header className="page-hero">
        <div className="hero-eyebrow">
          <span className="hero-loc">📍 Netherlands</span>
          <span className="hero-dot">·</span>
          <span>Year at a glance</span>
        </div>
        <h1 className="hero-title">The <em>calendar</em></h1>
        <p className="hero-sub">When each vegetable peaks across the year. Filled = at its best, half = available.</p>
      </header>

      <div className="cal-table">
        <div className="cal-row cal-head">
          <div className="cal-name-cell" />
          {MONTHS.map((m, i) => (
            <div key={m} className={`cal-month${i + 1 === month ? " now" : ""}`}>{m}</div>
          ))}
        </div>
        {sorted.map((v) => (
          <button key={v.id} className="cal-row" onClick={() => onOpen(v)}>
            <div className="cal-name-cell">
              <div className="cal-swatch" style={{ background: v.color, borderColor: v.accent }} />
              <div>
                <div className="cal-name">{v.name}</div>
                <div className="cal-nl">{v.nl}</div>
              </div>
            </div>
            {MONTHS.map((m, i) => {
              const mi = i + 1;
              const peak = v.peak.includes(mi);
              const shoulder = v.shoulder?.includes(mi) ?? false;
              return (
                <div key={m} className={`cal-cell${peak ? " on" : shoulder ? " half" : ""}${mi === month ? " now" : ""}`}>
                  <div className="cal-fill" style={{ background: peak ? v.accent : shoulder ? v.color : "transparent" }} />
                </div>
              );
            })}
          </button>
        ))}
      </div>
    </div>
  );
}
