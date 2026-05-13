import type { VegItem } from "@/lib/vegetables";
import { VEG_DATA, MONTHS, MONTHS_LONG } from "@/lib/vegetables";

type Props = { month: number; onOpen: (v: VegItem) => void };

export default function CalendarPage({ month, onOpen }: Props) {
  const sorted = [...VEG_DATA].sort(
    (a, b) => Math.min(...a.peak) - Math.min(...b.peak) || a.name.localeCompare(b.name)
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
