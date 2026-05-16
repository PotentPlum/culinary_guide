import type { NextRequest } from "next/server";
import { ingestRecipeFromHtml, RecipeIngestionError } from "@/lib/recipeIngestion";

const BROWSER_UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
  "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

export async function POST(req: NextRequest) {
  let url: string;
  try {
    const body = (await req.json()) as { url?: unknown };
    url = typeof body.url === "string" ? body.url.trim() : "";
  } catch {
    return Response.json({ error: "Ongeldig verzoekformaat." }, { status: 400 });
  }

  if (!url) {
    return Response.json({ error: "Geen URL opgegeven." }, { status: 400 });
  }

  let parsed: URL;
  try {
    parsed = new URL(url);
  } catch {
    return Response.json({ error: "Ongeldige URL." }, { status: 400 });
  }

  if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
    return Response.json(
      { error: "Alleen http(s) URLs zijn toegestaan." },
      { status: 400 },
    );
  }

  let html: string;
  try {
    const upstream = await fetch(url, {
      headers: {
        "User-Agent": BROWSER_UA,
        Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "nl-NL,nl;q=0.9,en-US;q=0.8,en;q=0.7",
      },
    });

    if (!upstream.ok) {
      return Response.json(
        { error: `De pagina kon niet worden opgehaald (HTTP ${upstream.status}).` },
        { status: 502 },
      );
    }

    html = await upstream.text();
  } catch {
    return Response.json(
      { error: "De URL kon niet worden bereikt. Controleer de link en probeer opnieuw." },
      { status: 502 },
    );
  }

  try {
    const recipe = ingestRecipeFromHtml(html);
    return Response.json(recipe);
  } catch (err) {
    if (err instanceof RecipeIngestionError && err.code === "UNSUPPORTED_RECIPE_FORMAT") {
      return Response.json(
        {
          error:
            "Deze pagina bevat geen herkenbaar recept in gestructureerde data (Schema.org). " +
            "Probeer een andere receptpagina.",
        },
        { status: 422 },
      );
    }
    return Response.json(
      { error: "Onverwachte fout bij het verwerken van het recept." },
      { status: 500 },
    );
  }
}
