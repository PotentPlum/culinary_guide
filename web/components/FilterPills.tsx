export type Counts = { all: number; peak: number; shoulder: number; soon: number; ending: number };
export type Filter = "all" | "peak" | "shoulder" | "soon" | "ending";

type Props = { value: Filter; onChange: (f: Filter) => void; counts: Counts };

const OPTIONS: { id: Filter; label: string }[] = [
  { id: "all",      label: "Alles in seizoen" },
  { id: "peak",     label: "Op hoogtepunt" },
  { id: "shoulder", label: "Beschikbaar" },
  { id: "soon",     label: "Binnenkort" },
  { id: "ending",   label: "Aflopend" },
];

export default function FilterPills({ value, onChange, counts }: Props) {
  return (
    <div className="filter-pills" role="tablist">
      {OPTIONS.map((opt) => (
        <button
          key={opt.id}
          role="tab"
          aria-selected={value === opt.id}
          className={`pill${value === opt.id ? " active" : ""}`}
          onClick={() => onChange(opt.id)}
        >
          <span>{opt.label}</span>
          <span className="pill-count">{counts[opt.id] ?? 0}</span>
        </button>
      ))}
    </div>
  );
}
