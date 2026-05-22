import type { NextRequest } from "next/server";
import type { ChallengeResponse, ChallengeParams } from "@/lib/types";
import { supabase } from "@/lib/supabase";
import { getFacet } from "@/lib/facets";

const DISH_TYPES   = getFacet("dishType").options.map((o) => o.value).join(", ");
const CUISINES     = getFacet("cuisine").options.map((o) => o.value).join(", ");
const MAIN_INGR    = getFacet("mainIngredient").options.map((o) => o.value).join(", ");
const PREP_TIMES   = getFacet("prepTime").options.map((o) => o.value).join(", ");

const TIME_LABEL: Record<NonNullable<ChallengeParams["timeLimit"]>, string> = {
  "<15": "minder dan 15 minuten",
  "<30": "minder dan 30 minuten",
  "<60": "minder dan 60 minuten",
  ">60": "meer dan 60 minuten",
};

const SYSTEM_PROMPT = `Je bent een kookcoach voor thuiskoks. Wanneer je een ingrediënt en optionele parameters krijgt, genereer je een gerichte kookuitdaging die resulteert in een concreet, klaargemaakt gerecht.

Geef ALLEEN een JSON-object terug met deze exacte velden:
{
  "challenge": "Een duidelijke, motiverende uitdagingsomschrijving (2–3 zinnen, in het Nederlands)",
  "skill": "De specifieke culinaire techniek die geoefend wordt (3–5 woorden, in het Nederlands)",
  "difficulty": "Beginner" of "Intermediate" of "Advanced",
  "timeEstimate": getal (minuten als geheel getal),
  "why": "Korte uitleg waarom deze techniek belangrijk is (1–2 zinnen, in het Nederlands)",
  "outputType": "Type gerecht — VERPLICHT een van: ${DISH_TYPES}",
  "tags": {
    "cuisine": ["optioneel — een of meer van: ${CUISINES}"],
    "dishType": ["VERPLICHT — zelfde waarde als outputType"],
    "mainIngredient": ["een of meer van: ${MAIN_INGR}"],
    "prepTime": ["VERPLICHT — een van: ${PREP_TIMES}, gebaseerd op timeEstimate"]
  },
  "recipe": {
    "title": "Naam van het gerecht (in het Nederlands)",
    "servings": getal (aantal personen als geheel getal),
    "ingredients": [
      { "amount": "hoeveelheid als string", "unit": "eenheid (bijv. g, el, tl, stuks — of lege string)", "name": "ingrediëntnaam in het Nederlands" }
    ],
    "steps": [
      { "step": 1, "instruction": "Duidelijke bereidingsstap in het Nederlands" }
    ],
    "tips": ["Optionele praktische tip in het Nederlands"]
  }
}

Regels:
- "difficulty" moet exact één van deze Engelse waarden zijn: "Beginner", "Intermediate", "Advanced"
- "outputType" moet exact één van de opgegeven gerechttypes zijn
- "tags.dishType" moet dezelfde waarde hebben als "outputType"
- "tags.prepTime" moet één waarde bevatten gebaseerd op timeEstimate: <15 (tot 15 min), <30 (15–30 min), <60 (30–60 min), >60 (meer dan 60 min)
- Alle andere tekstvelden moeten in het Nederlands zijn
- Het recept moet volledig en bruikbaar zijn — geen vage stappen
- Schrijf geen tekst buiten het JSON-object
- Als een parameter is meegegeven (techniek, keuken, tijd, eenvoudig), houd je daar strikt rekening mee`;

type ClaudeTextBlock = { type: "text"; text: string };
type ClaudeContent = ClaudeTextBlock[];

function buildUserMessage(ingredient: string, params: ChallengeParams): string {
  const ingredientLine =
    params.quantity && params.unit
      ? `Ingrediënt: ${params.quantity} ${params.unit} ${ingredient}`
      : `Ingrediënt: ${ingredient}`;
  const lines = [ingredientLine];

  if (params.technique) {
    lines.push(`Techniek: ${params.technique}`);
  }
  if (params.cuisine) {
    lines.push(`Keuken: ${params.cuisine}`);
  }
  if (params.timeLimit) {
    lines.push(`Beschikbare tijd: ${TIME_LABEL[params.timeLimit]}`);
  }
  if (params.simpleAndSmall) {
    lines.push(
      "Stijl: Klein en eenvoudig — gebruik weinig ingrediënten, bij voorkeur alleen wat iemand doorgaans in huis heeft (pantry-ingrediënten)"
    );
  }

  return lines.join("\n");
}

async function callClaude(
  apiKey: string,
  ingredient: string,
  params: ChallengeParams
): Promise<ChallengeResponse> {
  const res = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "x-api-key": apiKey,
      "anthropic-version": "2023-06-01",
      "content-type": "application/json",
    },
    body: JSON.stringify({
      model: "claude-sonnet-4-6",
      max_tokens: 2000,
      temperature: 0.7,
      system: SYSTEM_PROMPT,
      messages: [
        { role: "user", content: buildUserMessage(ingredient, params) },
      ],
    }),
  });

  if (!res.ok) {
    throw new Error(`Claude API fout: ${res.status}`);
  }

  const data = (await res.json()) as { content: ClaudeContent };
  const text = data.content.find((b) => b.type === "text")?.text ?? "";

  const jsonMatch = text.match(/\{[\s\S]*\}/);
  if (!jsonMatch) throw new Error("Geen JSON gevonden in antwoord");

  const parsed = JSON.parse(jsonMatch[0]) as Partial<ChallengeResponse>;

  const validDishTypes = getFacet("dishType").options.map((o) => o.value);
  if (
    typeof parsed.challenge !== "string" ||
    typeof parsed.skill !== "string" ||
    !["Beginner", "Intermediate", "Advanced"].includes(parsed.difficulty ?? "") ||
    typeof parsed.timeEstimate !== "number" ||
    typeof parsed.why !== "string" ||
    typeof parsed.outputType !== "string" ||
    !validDishTypes.includes(parsed.outputType) ||
    !parsed.recipe ||
    typeof parsed.recipe.title !== "string" ||
    !Array.isArray(parsed.recipe.ingredients) ||
    !Array.isArray(parsed.recipe.steps)
  ) {
    throw new Error("Ongeldig antwoordformaat van Claude");
  }

  return parsed as ChallengeResponse;
}

export async function POST(req: NextRequest) {
  const apiKey = process.env.CLAUDE_API_KEY;
  if (!apiKey) {
    return Response.json({ error: "Claude API-sleutel niet geconfigureerd." }, { status: 500 });
  }

  let ingredient: string;
  let params: ChallengeParams;
  try {
    const body = (await req.json()) as { ingredient?: unknown; params?: unknown };
    ingredient = typeof body.ingredient === "string" ? body.ingredient.trim() : "";
    params = (typeof body.params === "object" && body.params !== null ? body.params : {}) as ChallengeParams;
  } catch {
    return Response.json({ error: "Ongeldig verzoekformaat." }, { status: 400 });
  }

  if (!ingredient) {
    return Response.json({ error: "Geen ingrediënt opgegeven." }, { status: 400 });
  }

  let lastError: unknown;
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      const result = await callClaude(apiKey, ingredient, params);

      await supabase.from("challenges").insert({
        ingredient,
        challenge_text: result.challenge,
        skill: result.skill,
        difficulty: result.difficulty,
        time_estimate: result.timeEstimate,
        why: result.why,
        output_type: result.outputType,
        recipe: { ...result.recipe, tags: result.tags },
        params: Object.keys(params).length ? params : null,
      });

      return Response.json(result);
    } catch (err) {
      lastError = err;
    }
  }

  const msg = lastError instanceof Error ? lastError.message : "Onbekende fout";
  return Response.json({ error: msg }, { status: 502 });
}
