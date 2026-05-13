const ITEMS = [
  { label: "Recipes by ingredient",    note: "We'll suggest dishes that use what's peaking this week." },
  { label: "Substitutions & pairings", note: "Find what works instead — and what to put it next to." },
  { label: "Foraging map",             note: "Where wild garlic, sorrel and chanterelles grow in NL." },
];

export default function FuturePeek() {
  return (
    <section className="future-peek">
      <div className="fp-label">Coming next</div>
      <div className="fp-grid">
        {ITEMS.map((it) => (
          <div className="fp-card" key={it.label}>
            <div className="fp-card-title">{it.label}</div>
            <div className="fp-card-note">{it.note}</div>
            <div className="fp-card-stripe" aria-hidden="true">
              {Array.from({ length: 20 }).map((_, i) => <span key={i} />)}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
