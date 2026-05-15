import type { VegItem, VegStatus } from "@/lib/vegetables";
import VegToken from "./VegToken";
import StatusDot from "./StatusDot";
import PeakBar from "./PeakBar";

type Density = "cozy" | "comfortable" | "compact";

type Props = {
  veg: VegItem;
  /** Pre-computed seasonal status — avoids recomputing what the parent already knows. */
  status: VegStatus;
  month: number;
  density?: Density;
  onOpen: (v: VegItem) => void;
  listMode?: boolean;
};

export default function VegCard({ veg, status, month, density = "comfortable", onOpen, listMode }: Props) {
  const offCls = status === "off" ? " off-season" : "";

  if (listMode) {
    return (
      <button className={`veg-row${offCls}`} onClick={() => onOpen(veg)}>
        <div className="veg-row-swatch" style={{ background: veg.color, borderColor: veg.accent }} />
        <div className="veg-row-text">
          <div className="veg-row-name">{veg.name}</div>
          <div className="veg-row-nl">{veg.nl}</div>
        </div>
        <div className="veg-row-bar"><PeakBar veg={veg} month={month} compact /></div>
        <StatusDot status={status} withLabel />
        <span className="veg-row-arrow" aria-hidden="true">→</span>
      </button>
    );
  }

  const tokenSize = density === "compact" ? 64 : density === "cozy" ? 104 : 84;

  return (
    <button className={`veg-card density-${density}${offCls}`} onClick={() => onOpen(veg)}>
      <div className="veg-card-illus">
        <VegToken veg={veg} size={tokenSize} />
        <div className="veg-card-status">
          <StatusDot status={status} withLabel />
        </div>
      </div>
      <div className="veg-card-text">
        <div className="veg-card-name">{veg.name}</div>
        <div className="veg-card-nl">{veg.nl}</div>
        {density !== "compact" && <PeakBar veg={veg} month={month} />}
      </div>
    </button>
  );
}
