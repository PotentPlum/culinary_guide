const ITEMS = [
  { label: "Recepten op ingrediënt",   note: "We stellen gerechten voor die gebruikmaken van wat er nu piekt." },
  { label: "Vervangers & combinaties", note: "Vind wat er in de plaats komt — en wat er goed bij past." },
  { label: "Plukkaart",                note: "Waar daslook, zuring en cantharellen groeien in Nederland." },
];

export default function FuturePeek() {
  return (
    <section className="future-peek">
      <div className="fp-label">Binnenkort</div>
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
