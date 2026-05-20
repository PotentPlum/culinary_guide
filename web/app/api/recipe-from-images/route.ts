import type { NextRequest } from "next/server";
import type { RecipeOutput } from "@/lib/recipeIngestion";

const MAX_IMAGES = 5;
const ALLOWED_MEDIA_TYPES = ["image/jpeg", "image/png", "image/webp"] as const;
type AllowedMediaType = (typeof ALLOWED_MEDIA_TYPES)[number];

const SYSTEM_PROMPT = `Je bent een kookassistent die recepten uit foto's leest.
Extraheer het recept uit de afbeelding(en) en geef ALLEEN een geldig JSON-object terug.
Combineer meerdere foto's als ze onderdelen van hetzelfde recept tonen.
Converteer alle maten naar metrisch systeem (g, ml, °C — geen oz, cups, °F).
Schrijf geen tekst buiten het JSON-object.`;

type FlatRecipe = {
  title?: string;
  description?: string;
  prepTimeMinutes?: number | null;
  cookTimeMinutes?: number | null;
  totalTimeMinutes?: number | null;
  ingredients?: string[];
  steps?: string[];
};

type ImageBlock = { type: "image"; source: { type: "base64"; media_type: AllowedMediaType; data: string } };
type TextBlock = { type: "text"; text: string };

function toRecipeOutput(flat: FlatRecipe): RecipeOutput {
  const steps = (flat.steps ?? []).map((text, i) => ({ stepNumber: i + 1, text }));
  return {
    title: flat.title ?? "Recept",
    description: flat.description ?? "",
    prepTimeMinutes: flat.prepTimeMinutes ?? null,
    cookTimeMinutes: flat.cookTimeMinutes ?? null,
    totalTimeMinutes: flat.totalTimeMinutes ?? null,
    cuisine: [],
    category: [],
    nutrition: { calories: null, protein: null, carbs: null, fat: null },
    ingredientSections: [{ sectionName: "Ingrediënten", ingredients: flat.ingredients ?? [] }],
    instructionSections: [{ sectionName: "Bereiding", steps }],
  };
}

async function extractRecipe(
  apiKey: string,
  images: Array<{ data: string; mediaType: AllowedMediaType }>,
): Promise<RecipeOutput> {
  const imageBlocks: ImageBlock[] = images.map(({ data, mediaType }) => ({
    type: "image",
    source: { type: "base64", media_type: mediaType, data },
  }));

  const textBlock: TextBlock = {
    type: "text",
    text: `Lees ${images.length === 1 ? "dit recept" : "deze receptfoto's"} en geef het recept terug als JSON:
{
  "title": "naam",
  "description": "optionele korte beschrijving",
  "prepTimeMinutes": 20,
  "cookTimeMinutes": 30,
  "totalTimeMinutes": 50,
  "ingredients": ["200g bloem", "3 eieren"],
  "steps": ["Stap 1 tekst", "Stap 2 tekst"]
}

Regels:
- Alle maten in metrisch: oz→g, cups→ml, °F→°C
- Tijden als geheel getal in minuten, of null als niet vermeld
- Ingrediënten vóór stappen
- Alleen JSON, geen extra tekst`,
  };

  const res = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "x-api-key": apiKey,
      "anthropic-version": "2023-06-01",
      "content-type": "application/json",
    },
    body: JSON.stringify({
      model: "claude-sonnet-4-6",
      max_tokens: 2048,
      system: SYSTEM_PROMPT,
      messages: [{ role: "user", content: [...imageBlocks, textBlock] }],
    }),
  });

  if (!res.ok) {
    throw new Error(`Claude API fout: ${res.status}`);
  }

  const data = (await res.json()) as { content: Array<{ type: string; text?: string }> };
  const text = data.content.find((b) => b.type === "text")?.text ?? "";

  const jsonMatch = text.match(/\{[\s\S]*\}/);
  if (!jsonMatch) throw new Error("Geen JSON gevonden in antwoord");

  const flat = JSON.parse(jsonMatch[0]) as FlatRecipe;
  return toRecipeOutput(flat);
}

export async function POST(req: NextRequest) {
  const apiKey = process.env.CLAUDE_API_KEY;
  if (!apiKey) {
    return Response.json({ error: "Claude API-sleutel niet geconfigureerd." }, { status: 500 });
  }

  let images: Array<{ data: string; mediaType: string }>;
  try {
    const body = (await req.json()) as { images?: unknown };
    if (!Array.isArray(body.images)) throw new Error();
    images = body.images as Array<{ data: string; mediaType: string }>;
  } catch {
    return Response.json({ error: "Ongeldig verzoekformaat." }, { status: 400 });
  }

  if (images.length === 0) {
    return Response.json({ error: "Geen afbeeldingen ontvangen." }, { status: 400 });
  }
  if (images.length > MAX_IMAGES) {
    return Response.json({ error: `Maximaal ${MAX_IMAGES} afbeeldingen per keer.` }, { status: 400 });
  }
  for (const img of images) {
    if (!ALLOWED_MEDIA_TYPES.includes(img.mediaType as AllowedMediaType)) {
      return Response.json({ error: "Alleen JPEG, PNG en WebP zijn toegestaan." }, { status: 400 });
    }
  }

  let lastError: unknown;
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      const recipe = await extractRecipe(
        apiKey,
        images as Array<{ data: string; mediaType: AllowedMediaType }>,
      );
      return Response.json(recipe);
    } catch (err) {
      lastError = err;
      if (err instanceof Error && err.message.startsWith("Claude API fout:")) break;
    }
  }

  const msg = lastError instanceof Error ? lastError.message : "Onbekende fout";
  return Response.json({ error: msg }, { status: 502 });
}
