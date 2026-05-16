import type { VegItem } from "@/lib/vegetables";
import { MONTHS, MONTHS_LONG } from "@/lib/vegetables";

type Props = { veg: VegItem; month: number; compact?: boolean };

export default function PeakBar({ veg, month, compact = false }: Props) {
  return (
    <div className={`peak-bar${compact ? " compact" : ""}`}>
      {MONTHS.map((m, i) => {
        const mIdx = i + 1;
        const isPeak = veg.peak.includes(mIdx);
        const isShoulder = veg.shoulder?.includes(mIdx) ?? false;
        const isNow = mIdx === month;
        let cls = "peak-cell";
        if (isPeak) cls += " on";
        else if (isShoulder) cls += " half";
        if (isNow) cls += " now";
        return (
          <div key={m} className={cls} title={`${MONTHS_LONG[i]}: ${isPeak ? "hoogtepunt" : isShoulder ? "beschikbaar" : "buiten seizoen"}`}>
            {!compact && <span className="peak-letter">{m[0]}</span>}
          </div>
        );
      })}
    </div>
  );
}
