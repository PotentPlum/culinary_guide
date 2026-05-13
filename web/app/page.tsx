"use client";

import { useState } from "react";
import { getInSeason, MONTHS, type Vegetable } from "@/lib/vegetables";
import VegetableCard from "@/components/VegetableCard";
import VegetableModal from "@/components/VegetableModal";

export default function Home() {
  const currentMonth = new Date().getMonth() + 1;
  const [selectedMonth, setSelectedMonth] = useState(currentMonth);
  const [selected, setSelected] = useState<Vegetable | null>(null);

  const inSeason = getInSeason(selectedMonth);

  return (
    <main className="min-h-screen bg-stone-50">
      <header className="bg-green-800 text-white px-4 py-6 shadow">
        <h1 className="text-2xl font-bold tracking-tight">Seizoenswijzer</h1>
        <p className="text-green-200 text-sm mt-1">Wat is er nu in het seizoen?</p>
      </header>

      <section className="px-4 py-5 max-w-4xl mx-auto">
        <div className="flex gap-1.5 flex-wrap justify-center mb-6">
          {MONTHS.map((label, i) => {
            const month = i + 1;
            return (
              <button
                key={month}
                onClick={() => setSelectedMonth(month)}
                className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
                  selectedMonth === month
                    ? "bg-green-700 text-white shadow"
                    : "bg-white text-stone-600 border border-stone-200 hover:border-green-400 hover:text-green-700"
                }`}
              >
                {label}
              </button>
            );
          })}
        </div>

        <p className="text-stone-500 text-sm text-center mb-4">
          <span className="font-semibold text-green-700">{inSeason.length}</span>{" "}
          groenten &amp; fruit in seizoen in{" "}
          <span className="font-semibold text-stone-700">{MONTHS[selectedMonth - 1]}</span>
        </p>

        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
          {inSeason.map((v) => (
            <VegetableCard key={v.name} vegetable={v} onClick={() => setSelected(v)} />
          ))}
        </div>
      </section>

      {selected && (
        <VegetableModal vegetable={selected} onClose={() => setSelected(null)} />
      )}
    </main>
  );
}
