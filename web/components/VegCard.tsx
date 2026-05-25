import type { VegItem, VegStatus } from "@/lib/vegetables";
import { statusFor } from "@/lib/vegetables";
import VegToken from "./VegToken";
import StatusDot from "./StatusDot";
import PeakBar from "./PeakBar";

type Density = "cozy" | "comfortable" | "compact";

type Props = {
  veg: VegItem;
  month: number;
  density?: Density;
  onOpen: (v: VegItem) => void;
  listMode?: boolean;
};

export default function VegCard({ veg, month, density = "comfortable", onOpen, listMode }: Props) {
  const status: VegStatus = statusFor(veg, month);

  if (listMode) {
    return (
      <button className="veg-row" onClick={() => onOpen(veg)} aria-label={`Bekijk ${veg.nl}`} style={{ '--veg-color': veg.color } as React.CSSProperties}>
        <div className="veg-row-swatch" style={{ background: veg.color, borderColor: veg.accent }} />
        <div className="veg-row-text">
          <div className="veg-row-name">{veg.nl}</div>
        </div>
        <div className="veg-row-bar"><PeakBar veg={veg} month={month} compact /></div>
        <StatusDot status={status} withLabel />
        <span className="veg-row-arrow" aria-hidden="true">→</span>
      </button>
    );
  }

  const tokenSize = density === "compact" ? 64 : density === "cozy" ? 104 : 84;

  return (
    <button className={`veg-card density-${density}`} onClick={() => onOpen(veg)} aria-label={`Bekijk ${veg.nl}`}>
      <div className="veg-card-illus" style={{ '--veg-color': veg.color } as React.CSSProperties}>
        <VegToken veg={veg} size={tokenSize} />
        <div className="veg-card-status">
          <StatusDot status={status} />
        </div>
      </div>
      <div className="veg-card-text">
        <div className="veg-card-name">{veg.nl}</div>
        {density !== "compact" && <PeakBar veg={veg} month={month} />}
      </div>
    </button>
  );
}
