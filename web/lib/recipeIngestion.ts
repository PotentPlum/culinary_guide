/**
 * recipeIngestion — Schema.org JSON-LD recipe extractor
 *
 * Accepts a URL (or raw HTML string), locates the embedded Schema.org Recipe node,
 * and returns a normalized RecipeOutput object. Handles @graph blocks, nested
 * mainEntity structures, multi-section ingredients/instructions, and ISO 8601 durations.
 */

// ─── Output types ────────────────────────────────────────────────────────────

export interface NutritionInfo {
  calories: string | null;
  protein: string | null;
  carbs: string | null;
  fat: string | null;
}

export interface IngredientSection {
  sectionName: string;
  ingredients: string[];
}

export interface InstructionStep {
  stepNumber: number;
  text: string;
}

export interface InstructionSection {
  sectionName: string;
  steps: InstructionStep[];
}

export interface RecipeOutput {
  title: string;
  description: string;
  prepTimeMinutes: number | null;
  cookTimeMinutes: number | null;
  totalTimeMinutes: number | null;
  cuisine: string | string[];
  category: string | string[];
  nutrition: NutritionInfo;
  ingredientSections: IngredientSection[];
  instructionSections: InstructionSection[];
}

// ─── Error type ──────────────────────────────────────────────────────────────

export class RecipeIngestionError extends Error {
  readonly code: string;

  constructor(code: string, message: string) {
    super(message);
    this.name = "RecipeIngestionError";
    this.code = code;
  }
}

const UNSUPPORTED = "UNSUPPORTED_RECIPE_FORMAT";

// ─── ISO 8601 duration parser ─────────────────────────────────────────────────

/**
 * Converts an ISO 8601 duration string (e.g. "PT1H15M", "PT30M") into total minutes.
 * Returns null for absent, empty, or unparseable values.
 */
export function parseIso8601Duration(value: string | null | undefined): number | null {
  if (!value) return null;

  // Full ISO 8601 pattern: P[nY][nM][nD]T[nH][nM][nS]
  const match = value.match(
    /^P(?:(\d+)Y)?(?:(\d+)M)?(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?)?$/i,
  );
  if (!match) return null;

  const days = parseInt(match[3] ?? "0", 10);
  const hours = parseInt(match[4] ?? "0", 10);
  const minutes = parseInt(match[5] ?? "0", 10);
  const seconds = parseFloat(match[6] ?? "0");

  const total = days * 1440 + hours * 60 + minutes + Math.ceil(seconds / 60);
  return total === 0 ? null : total;
}

// ─── Text utilities ────────────────────────────────────────────────────────────────────────────────

/**
 * Removes HTML tags from a string, replacing block-level elements with spaces
 * so adjacent words don't run together after tag removal.
 * Also decodes common HTML entities.
 */
export function stripHtml(text: string): string {
  return text
    .replace(/<\/(p|li|td|th|div|h[1-6]|section|article)>/gi, " ")
    .replace(/<br\s*\/?>\s*/gi, " ")
    .replace(/<[^>]+>/g, "")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&apos;/g, "'")
    .replace(/&nbsp;/g, " ")
    .replace(/&#(\d+);/g, (_, code) => String.fromCharCode(parseInt(code, 10)))
    .replace(/&#x([0-9a-f]+);/gi, (_, hex) => String.fromCharCode(parseInt(hex, 16)));
}

/**
 * Strips common unicode artifacts and normalises whitespace.
 * ˚ is the ring-above diacritic sometimes emitted instead of the degree sign.
 */
export function cleanText(text: string): string {
  return stripHtml(text)
    .replace(/˚/g, "°") // ring above → degree sign
    .replace(/°/g, "°") // already a degree sign, normalise
    .replace(/’/g, "'") // right single quotation mark
    .replace(/‘/g, "'") // left single quotation mark
    .replace(/“/g, '"') // left double quotation mark
    .replace(/”/g, '"') // right double quotation mark
    .replace(/ /g, " ") // non-breaking space
    .replace(/\s+/g, " ")
    .trim();
}

// ─── Schema.org node traversal ────────────────────────────────────────────────

type JsonValue = string | number | boolean | null | JsonValue[] | JsonObject;
type JsonObject = { [key: string]: JsonValue };

/**
 * Recursively searches any JSON-LD payload for a node whose @type is "Recipe"
 * (or an array containing "Recipe"). Handles:
 *   - Root-level Recipe objects
 *   - @graph arrays (flat or nested)
 *   - mainEntity / mainEntityOfPage wrappers
 *   - Arbitrary nesting via other keys
 */
export function findRecipeNode(value: unknown): JsonObject | null {
  if (value === null || typeof value !== "object") return null;

  if (Array.isArray(value)) {
    for (const item of value) {
      const found = findRecipeNode(item as unknown);
      if (found) return found;
    }
    return null;
  }

  const obj = value as JsonObject;
  const type = obj["@type"];

  if (type === "Recipe" || (Array.isArray(type) && type.includes("Recipe"))) {
    return obj;
  }

  // Prioritise known structural wrapper keys before generic traversal to avoid
  // accidentally matching a deeply-nested non-recipe document's recipe reference.
  const priorityKeys = ["@graph", "mainEntity", "mainEntityOfPage"];
  for (const key of priorityKeys) {
    if (key in obj) {
      const found = findRecipeNode(obj[key]);
      if (found) return found;
    }
  }

  // Generic deep traversal for less predictable structures
  for (const key of Object.keys(obj)) {
    if (priorityKeys.includes(key) || key === "@context") continue;
    const child = obj[key];
    if (child !== null && typeof child === "object") {
      const found = findRecipeNode(child);
      if (found) return found;
    }
  }

  return null;
}

// ─── HTML → JSON-LD extraction ────────────────────────────────────────────────

/**
 * Extracts and parses every application/ld+json <script> block from an HTML string.
 * Silently skips blocks with invalid JSON.
 */
export function extractJsonLdBlocks(html: string): JsonValue[] {
  const blocks: JsonValue[] = [];
  // Matches both single and double-quoted type attributes, with optional whitespace
  const scriptRe =
    /<script[^>]+type\s*=\s*["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/gi;

  let match: RegExpExecArray | null;
  while ((match = scriptRe.exec(html)) !== null) {
    try {
      blocks.push(JSON.parse(match[1]) as JsonValue);
    } catch {
      // Malformed JSON-LD — skip and continue to next block
    }
  }
  return blocks;
}

// ─── Ingredient section parser ────────────────────────────────────────────────

/**
 * Detects implicit section headers inside a flat ingredient array.
 *
 * An item is treated as a header if:
 *   (a) it ends with a colon, e.g. "For the crust:"
 *   (b) it is written entirely in uppercase and contains no digit (quantities),
 *       e.g. "DRESSING", "FOR THE SAUCE"
 */
export function isIngredientHeader(item: string): boolean {
  const trimmed = item.trim();
  if (!trimmed) return false;
  if (trimmed.endsWith(":")) return true;

  const upper = trimmed.toUpperCase();
  if (trimmed === upper && !/\d/.test(trimmed) && trimmed.length > 1) return true;

  return false;
}

/** Strips a trailing colon from a section name. */
function normaliseSectionName(raw: string): string {
  return raw.trim().replace(/:$/, "").trim();
}

export function parseIngredientSections(ingredients: string[]): IngredientSection[] {
  const sections: IngredientSection[] = [];
  let currentName = "Ingredients";
  let currentItems: string[] = [];

  for (const item of ingredients) {
    if (isIngredientHeader(item)) {
      // Flush accumulated items to current section (even if empty, to preserve order)
      if (currentItems.length > 0) {
        sections.push({ sectionName: currentName, ingredients: currentItems });
        currentItems = [];
      } else if (sections.length === 0) {
        // First item is already a header — no leading section to flush
      }
      currentName = normaliseSectionName(item);
    } else {
      currentItems.push(cleanText(item));
    }
  }

  // Flush final section
  sections.push({ sectionName: currentName, ingredients: currentItems });

  return sections;
}

// ─── Instruction section parser ───────────────────────────────────────────────

type RawInstruction = string | JsonObject;

function isHowToSection(item: RawInstruction): boolean {
  return (
    item !== null &&
    typeof item === "object" &&
    !Array.isArray(item) &&
    (item as JsonObject)["@type"] === "HowToSection"
  );
}

function stepsFromItems(items: RawInstruction[]): InstructionStep[] {
  return items.map((step, idx) => ({
    stepNumber: idx + 1,
    text: cleanText(extractStepText(step)),
  }));
}

/**
 * Normalises recipeInstructions which may arrive as:
 *   1. A flat array of plain strings
 *   2. A flat array of HowToStep objects ({ "@type": "HowToStep", "text": "..." })
 *   3. An array of HowToSection objects containing nested itemListElement arrays
 *   4. A mixed array where HowToSection and HowToStep appear at the same level
 */
export function parseInstructionSections(raw: JsonValue | undefined): InstructionSection[] {
  if (raw == null) return [];

  const arr: RawInstruction[] = Array.isArray(raw)
    ? (raw as RawInstruction[])
    : [raw as RawInstruction];

  if (arr.length === 0) return [];

  const hasSections = arr.some(isHowToSection);

  if (hasSections) {
    // Mixed or pure HowToSection array: collect loose steps under the preceding section,
    // or under a default "Instructions" section if they appear before any named section.
    const result: InstructionSection[] = [];
    let currentName = "Instructions";
    let currentItems: RawInstruction[] = [];

    for (const item of arr) {
      if (isHowToSection(item)) {
        // Flush accumulated loose steps before this section
        if (currentItems.length > 0) {
          result.push({ sectionName: currentName, steps: stepsFromItems(currentItems) });
          currentItems = [];
        }
        const s = item as JsonObject;
        const sectionName = cleanText(String(s["name"] ?? "Instructions"));
        const itemList = s["itemListElement"];
        const nestedItems: RawInstruction[] = Array.isArray(itemList)
          ? (itemList as RawInstruction[])
          : [];
        result.push({ sectionName, steps: stepsFromItems(nestedItems) });
        currentName = sectionName;
      } else {
        currentItems.push(item);
      }
    }

    if (currentItems.length > 0) {
      result.push({ sectionName: currentName, steps: stepsFromItems(currentItems) });
    }

    return result;
  }

  // Flat array of strings or HowToStep objects
  return [{ sectionName: "Instructions", steps: stepsFromItems(arr) }];
}

function extractStepText(item: RawInstruction): string {
  if (typeof item === "string") return item;
  if (item !== null && typeof item === "object") {
    const obj = item as JsonObject;
    if (typeof obj["text"] === "string") return obj["text"];
    if (typeof obj["name"] === "string") return obj["name"];
  }
  return String(item);
}

// ─── Nutrition extractor ──────────────────────────────────────────────────────

function extractNutrition(raw: JsonValue | undefined): NutritionInfo {
  const nil: NutritionInfo = { calories: null, protein: null, carbs: null, fat: null };
  if (!raw || typeof raw !== "object" || Array.isArray(raw)) return nil;

  const n = raw as JsonObject;
  const field = (key: string): string | null => {
    const v = n[key];
    if (v == null) return null;
    const s = String(v).trim();
    return s === "" || s === "null" ? null : s;
  };

  return {
    calories: field("calories"),
    protein: field("proteinContent"),
    carbs: field("carbohydrateContent"),
    fat: field("fatContent"),
  };
}

// ─── String coercion helpers ──────────────────────────────────────────────────

function toStringOrArray(v: JsonValue | undefined): string | string[] {
  if (v == null) return "";
  if (Array.isArray(v)) return (v as JsonValue[]).map(String);
  return String(v);
}

// ─── Public API ───────────────────────────────────────────────────────────────

/**
 * Parses a raw HTML string and returns a normalised RecipeOutput.
 * Throws RecipeIngestionError with code UNSUPPORTED_RECIPE_FORMAT when no
 * valid Recipe node is found.
 */
export function ingestRecipeFromHtml(html: string): RecipeOutput {
  const blocks = extractJsonLdBlocks(html);

  let recipe: JsonObject | null = null;
  for (const block of blocks) {
    recipe = findRecipeNode(block);
    if (recipe) break;
  }

  if (!recipe) {
    throw new RecipeIngestionError(
      UNSUPPORTED,
      "No valid Schema.org Recipe node found in any JSON-LD block on this page.",
    );
  }

  return {
    title: cleanText(String(recipe["name"] ?? "")),
    description: cleanText(String(recipe["description"] ?? "")),
    prepTimeMinutes: parseIso8601Duration(recipe["prepTime"] as string | null),
    cookTimeMinutes: parseIso8601Duration(recipe["cookTime"] as string | null),
    totalTimeMinutes: parseIso8601Duration(recipe["totalTime"] as string | null),
    cuisine: toStringOrArray(recipe["recipeCuisine"]),
    category: toStringOrArray(recipe["recipeCategory"]),
    nutrition: extractNutrition(recipe["nutrition"]),
    ingredientSections: parseIngredientSections(
      Array.isArray(recipe["recipeIngredient"])
        ? (recipe["recipeIngredient"] as string[])
        : [],
    ),
    instructionSections: parseInstructionSections(recipe["recipeInstructions"]),
  };
}

/**
 * Fetches the HTML at the given URL and returns a normalised RecipeOutput.
 *
 * Uses a standard desktop browser User-Agent to avoid CDN 403 blocks.
 * Throws RecipeIngestionError with code UNSUPPORTED_RECIPE_FORMAT when no
 * valid Recipe node is found.
 */
const BROWSER_HEADERS: Record<string, string> = {
  "User-Agent":
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
  Accept:
    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
  "Accept-Language": "nl-NL,nl;q=0.9,en-US;q=0.8,en;q=0.7",
  "Accept-Encoding": "gzip, deflate, br",
  "Cache-Control": "max-age=0",
  "Upgrade-Insecure-Requests": "1",
  "Sec-Fetch-Dest": "document",
  "Sec-Fetch-Mode": "navigate",
  "Sec-Fetch-Site": "none",
  "Sec-Fetch-User": "?1",
  "Sec-CH-UA": '"Chromium";v="124", "Google Chrome";v="124", "Not-A.Brand";v="99"',
  "Sec-CH-UA-Mobile": "?0",
  "Sec-CH-UA-Platform": '"Windows"',
};

export async function ingestRecipe(url: string): Promise<RecipeOutput> {
  const response = await fetch(url, {
    headers: BROWSER_HEADERS,
    redirect: "follow",
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status} fetching ${url}: ${response.statusText}`);
  }

  return ingestRecipeFromHtml(await response.text());
}
