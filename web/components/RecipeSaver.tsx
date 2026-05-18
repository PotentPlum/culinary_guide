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

// Stub: wire up to POST /api/recipes/save to persist the recipe
export default function RecipeSaver({ recipe, onSaved }: Props) {
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  async function handleSave() {
    setSaving(true);
    // TODO: call POST /api/recipes/save with recipe data
    // const res = await fetch("/api/recipes/save", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify(recipe) });
    // if (res.ok) { setSaved(true); onSaved?.(); }
    setSaving(false);
    setSaved(true);
    onSaved?.();
  }

  return (
    <button
      className="btn btn--primary"
      onClick={handleSave}
      disabled={saving || saved}
      aria-label="Recept opslaan"
    >
      {saved ? UI.recipes.savedBtn : saving ? UI.recipes.loadingBtn : UI.recipes.saveBtn}
    </button>
  );
}
