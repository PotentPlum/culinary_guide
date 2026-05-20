"use client";

import { useState } from "react";
import { UI } from "@/lib/strings";

type Props = {
  recipe: {
    name: string;
    ingredients: string[];
    instructions: string;
    source_url?: string;
  };
  onSaved?: () => void;
};

export default function RecipeSaver({ recipe, onSaved }: Props) {
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSave() {
    setSaving(true);
    setError(null);
    try {
      const res = await fetch("/api/recipes/save", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(recipe),
      });
      if (!res.ok) {
        const json = await res.json() as { error?: string };
        throw new Error(json.error ?? "Opslaan mislukt");
      }
      setSaved(true);
      onSaved?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Opslaan mislukt");
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <button
        className="btn btn--primary"
        onClick={handleSave}
        disabled={saving || saved}
        aria-label="Recept opslaan"
      >
        {saved ? UI.recipes.savedBtn : saving ? UI.recipes.loadingBtn : UI.recipes.saveBtn}
      </button>
      {error && <p className="error-msg">{error}</p>}
    </>
  );
}
