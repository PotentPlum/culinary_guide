"use client";

import { useEffect } from "react";
import type { Vegetable } from "@/lib/vegetables";
import { MONTHS } from "@/lib/vegetables";

type Props = {
  vegetable: Vegetable;
  onClose: () => void;
};

function seasonLabel(v: Vegetable): string {
  const start = MONTHS[v.start_month - 1];
  const end = MONTHS[v.end_month - 1];
  if (v.start_month === 1 && v.end_month === 12) return "Het hele jaar";
  if (v.start_month === v.end_month) return start;
  return `${start} – ${end}`;
}

export default function VegetableModal({ vegetable, onClose }: Props) {
  useEffect(() => {
    const handler = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 bg-black/40 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4"
      onClick={onClose}
    >
      <div
        className="bg-white w-full sm:max-w-lg rounded-t-2xl sm:rounded-2xl max-h-[85vh] overflow-y-auto shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="sticky top-0 bg-white border-b border-stone-100 px-5 py-4 flex items-start justify-between gap-3">
          <div>
            <h2 className="text-xl font-bold text-stone-800">{vegetable.name}</h2>
            <p className="text-green-700 text-sm font-medium mt-0.5">
              🌿 {seasonLabel(vegetable)}
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-stone-400 hover:text-stone-600 text-2xl leading-none mt-0.5"
            aria-label="Sluiten"
          >
            ×
          </button>
        </div>

        <div className="px-5 py-4 space-y-4 text-sm text-stone-700">
          <Section title="Kenmerken" body={vegetable.kenmerken} />
          <Section title="Beschikbaarheid" body={vegetable.beschikbaarheid} />
          {vegetable.voedingsstoffen && (
            <Section title="Voedingsstoffen" body={vegetable.voedingsstoffen} />
          )}
          {vegetable.in_de_keuken && (
            <Section title="In de keuken" body={vegetable.in_de_keuken} />
          )}
          {vegetable["hoe_bewaren?"] && (
            <Section title="Hoe bewaren?" body={vegetable["hoe_bewaren?"]} />
          )}
          <a
            href={vegetable.url}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-block text-green-700 underline underline-offset-2 text-xs"
          >
            Meer info op velt.nu →
          </a>
        </div>
      </div>
    </div>
  );
}

function Section({ title, body }: { title: string; body: string }) {
  return (
    <div>
      <h3 className="font-semibold text-stone-900 mb-1">{title}</h3>
      <p className="leading-relaxed text-stone-600">{body}</p>
    </div>
  );
}
