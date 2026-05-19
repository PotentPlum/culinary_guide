import { UI } from "@/lib/strings";

export default function FuturePeek() {
  return (
    <section className="future-peek">
      <div className="fp-label">{UI.futurePeek.label}</div>
      <div className="fp-grid">
        {UI.futurePeek.items.map((it) => (
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
