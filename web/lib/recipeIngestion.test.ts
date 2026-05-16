/**
 * Unit tests for recipeIngestion.ts
 *
 * Coverage:
 *   - ISO 8601 duration parsing
 *   - JSON-LD extraction from HTML
 *   - Deep traversal: @graph, mainEntity, arbitrary nesting
 *   - Ingredient section detection (colon headers, uppercase headers)
 *   - Instruction normalisation: plain strings, HowToStep, HowToSection
 *   - Full end-to-end ingestRecipeFromHtml across fixture layouts
 *   - Error path: missing recipe node, malformed JSON-LD
 */

import { describe, expect, it } from "vitest";
import {
  RecipeIngestionError,
  cleanText,
  extractJsonLdBlocks,
  findRecipeNode,
  ingestRecipeFromHtml,
  isIngredientHeader,
  parseIngredientSections,
  parseInstructionSections,
  parseIso8601Duration,
} from "./recipeIngestion";

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Wraps a JSON-LD object into a minimal HTML page string. */
function htmlWithJsonLd(payload: unknown): string {
  return `<!DOCTYPE html><html><head>
<script type="application/ld+json">${JSON.stringify(payload)}</script>
</head><body></body></html>`;
}

/** Minimal valid Recipe node for composing test fixtures. */
const BASE_RECIPE = {
  "@type": "Recipe",
  name: "Test Recipe",
  description: "A test recipe.",
  prepTime: "PT10M",
  cookTime: "PT20M",
  totalTime: "PT30M",
  recipeCuisine: "Dutch",
  recipeCategory: "Dinner",
  nutrition: {
    "@type": "NutritionInformation",
    calories: "350 kcal",
    proteinContent: "12g",
    carbohydrateContent: "45g",
    fatContent: "8g",
  },
  recipeIngredient: ["200g flour", "2 eggs", "100ml milk"],
  recipeInstructions: [
    { "@type": "HowToStep", text: "Mix flour and eggs." },
    { "@type": "HowToStep", text: "Add milk slowly." },
  ],
};

// ─── parseIso8601Duration ────────────────────────────────────────────────────

describe("parseIso8601Duration", () => {
  it("parses minutes only — PT20M", () => {
    expect(parseIso8601Duration("PT20M")).toBe(20);
  });

  it("parses hours and minutes — PT1H15M", () => {
    expect(parseIso8601Duration("PT1H15M")).toBe(75);
  });

  it("parses hours only — PT2H", () => {
    expect(parseIso8601Duration("PT2H")).toBe(120);
  });

  it("parses days — P1DT30M", () => {
    expect(parseIso8601Duration("P1DT30M")).toBe(1470); // 1440 + 30
  });

  it("rounds seconds up to next minute — PT1M30S", () => {
    expect(parseIso8601Duration("PT1M30S")).toBe(2);
  });

  it("returns null for empty string", () => {
    expect(parseIso8601Duration("")).toBeNull();
  });

  it("returns null for null input", () => {
    expect(parseIso8601Duration(null)).toBeNull();
  });

  it("returns null for undefined input", () => {
    expect(parseIso8601Duration(undefined)).toBeNull();
  });

  it("returns null for non-ISO strings", () => {
    expect(parseIso8601Duration("30 minutes")).toBeNull();
    expect(parseIso8601Duration("20")).toBeNull();
  });

  it("returns null for a valid but zero-value duration", () => {
    expect(parseIso8601Duration("PT0M")).toBeNull();
  });
});

// ─── cleanText ────────────────────────────────────────────────────────────────

describe("cleanText", () => {
  it("converts ring-above to degree sign", () => {
    expect(cleanText("180˚C")).toBe("180°C");
  });

  it("normalises curly quotes", () => {
    expect(cleanText("‘hello’")).toBe("'hello'");
    expect(cleanText("“hello”")).toBe('"hello"');
  });

  it("collapses internal whitespace", () => {
    expect(cleanText("mix   well   then  stir")).toBe("mix well then stir");
  });

  it("trims leading and trailing whitespace", () => {
    expect(cleanText("  hello  ")).toBe("hello");
  });
});

// ─── extractJsonLdBlocks ──────────────────────────────────────────────────────

describe("extractJsonLdBlocks", () => {
  it("extracts a single block", () => {
    const payload = { "@type": "WebPage" };
    const html = htmlWithJsonLd(payload);
    const blocks = extractJsonLdBlocks(html);
    expect(blocks).toHaveLength(1);
    expect(blocks[0]).toEqual(payload);
  });

  it("extracts multiple blocks from one page", () => {
    const html = `<html><head>
      <script type="application/ld+json">{"@type":"WebPage"}</script>
      <script type="application/ld+json">{"@type":"Recipe","name":"Soup"}</script>
    </head></html>`;
    const blocks = extractJsonLdBlocks(html);
    expect(blocks).toHaveLength(2);
  });

  it("silently skips malformed JSON blocks", () => {
    const html = `<html><head>
      <script type="application/ld+json">{ BROKEN JSON }</script>
      <script type="application/ld+json">{"@type":"Recipe","name":"Cake"}</script>
    </head></html>`;
    const blocks = extractJsonLdBlocks(html);
    expect(blocks).toHaveLength(1);
    expect((blocks[0] as Record<string, unknown>)["name"]).toBe("Cake");
  });

  it("returns empty array when no ld+json blocks present", () => {
    expect(extractJsonLdBlocks("<html><body>No structured data</body></html>")).toEqual([]);
  });

  it("handles single-quoted type attribute", () => {
    const html = `<script type='application/ld+json'>{"@type":"Recipe","name":"X"}</script>`;
    const blocks = extractJsonLdBlocks(html);
    expect(blocks).toHaveLength(1);
  });
});

// ─── findRecipeNode ───────────────────────────────────────────────────────────

describe("findRecipeNode — root object", () => {
  it("finds a root-level Recipe node", () => {
    const node = { "@type": "Recipe", name: "Pancakes" };
    expect(findRecipeNode(node)).toEqual(node);
  });

  it("handles @type as an array containing Recipe", () => {
    const node = { "@type": ["Thing", "Recipe"], name: "Soup" };
    expect(findRecipeNode(node)).toEqual(node);
  });

  it("returns null for non-recipe objects", () => {
    expect(findRecipeNode({ "@type": "WebPage", name: "Home" })).toBeNull();
  });

  it("returns null for primitives", () => {
    expect(findRecipeNode(null)).toBeNull();
    expect(findRecipeNode("string" as unknown as null)).toBeNull();
    expect(findRecipeNode(42 as unknown as null)).toBeNull();
  });
});

describe("findRecipeNode — @graph layout", () => {
  it("finds Recipe inside a @graph array", () => {
    const payload = {
      "@context": "https://schema.org",
      "@graph": [
        { "@type": "WebSite", name: "Food Blog" },
        { "@type": "WebPage", name: "About" },
        { "@type": "Recipe", name: "Apple Tart" },
      ],
    };
    const result = findRecipeNode(payload);
    expect(result).not.toBeNull();
    expect(result!["name"]).toBe("Apple Tart");
  });

  it("finds Recipe nested inside a @graph item", () => {
    const payload = {
      "@graph": [
        {
          "@type": "WebPage",
          mainEntity: { "@type": "Recipe", name: "Nested Pie" },
        },
      ],
    };
    const result = findRecipeNode(payload);
    expect(result!["name"]).toBe("Nested Pie");
  });

  it("returns null when @graph contains no Recipe", () => {
    const payload = {
      "@graph": [
        { "@type": "WebSite" },
        { "@type": "BreadcrumbList" },
      ],
    };
    expect(findRecipeNode(payload)).toBeNull();
  });
});

describe("findRecipeNode — mainEntity wrapper", () => {
  it("finds Recipe inside mainEntity", () => {
    const payload = {
      "@type": "WebPage",
      mainEntity: { "@type": "Recipe", name: "Stroopwafel" },
    };
    expect(findRecipeNode(payload)!["name"]).toBe("Stroopwafel");
  });

  it("finds Recipe inside mainEntityOfPage", () => {
    const payload = {
      mainEntityOfPage: { "@type": "Recipe", name: "Erwtensoep" },
    };
    expect(findRecipeNode(payload)!["name"]).toBe("Erwtensoep");
  });
});

describe("findRecipeNode — flat array at root", () => {
  it("finds Recipe in a root-level array", () => {
    const payload = [
      { "@type": "Organization" },
      { "@type": "Recipe", name: "Appelmoes" },
    ];
    expect(findRecipeNode(payload)!["name"]).toBe("Appelmoes");
  });
});

// ─── isIngredientHeader ───────────────────────────────────────────────────────

describe("isIngredientHeader", () => {
  it("detects colon-terminated strings", () => {
    expect(isIngredientHeader("For the crust:")).toBe(true);
    expect(isIngredientHeader("Sauce:")).toBe(true);
  });

  it("detects all-uppercase strings without digits", () => {
    expect(isIngredientHeader("DRESSING")).toBe(true);
    expect(isIngredientHeader("FOR THE SAUCE")).toBe(true);
  });

  it("does not flag mixed-case ingredient lines", () => {
    expect(isIngredientHeader("200g plain flour")).toBe(false);
    expect(isIngredientHeader("2 large eggs")).toBe(false);
  });

  it("does not flag uppercase strings with digits", () => {
    expect(isIngredientHeader("2 CUPS FLOUR")).toBe(false);
  });

  it("does not flag single-character strings", () => {
    expect(isIngredientHeader("A")).toBe(false);
  });

  it("returns false for empty strings", () => {
    expect(isIngredientHeader("")).toBe(false);
    expect(isIngredientHeader("   ")).toBe(false);
  });
});

// ─── parseIngredientSections ──────────────────────────────────────────────────

describe("parseIngredientSections — no headers", () => {
  it("groups all items under default 'Ingredients' section", () => {
    const result = parseIngredientSections(["200g flour", "2 eggs", "100ml milk"]);
    expect(result).toHaveLength(1);
    expect(result[0].sectionName).toBe("Ingredients");
    expect(result[0].ingredients).toEqual(["200g flour", "2 eggs", "100ml milk"]);
  });

  it("returns a single empty section for an empty array", () => {
    const result = parseIngredientSections([]);
    expect(result).toHaveLength(1);
    expect(result[0].ingredients).toEqual([]);
  });
});

describe("parseIngredientSections — colon headers", () => {
  it("splits ingredients at colon-terminated dividers", () => {
    const items = [
      "For the crust:",
      "200g butter",
      "300g flour",
      "For the filling:",
      "4 apples",
      "2 tbsp sugar",
    ];
    const result = parseIngredientSections(items);
    expect(result).toHaveLength(2);
    expect(result[0].sectionName).toBe("For the crust");
    expect(result[0].ingredients).toEqual(["200g butter", "300g flour"]);
    expect(result[1].sectionName).toBe("For the filling");
    expect(result[1].ingredients).toEqual(["4 apples", "2 tbsp sugar"]);
  });

  it("handles list that begins immediately with a header", () => {
    const items = ["For the dough:", "500g flour", "7g yeast"];
    const result = parseIngredientSections(items);
    expect(result).toHaveLength(1);
    expect(result[0].sectionName).toBe("For the dough");
    expect(result[0].ingredients).toHaveLength(2);
  });
});

describe("parseIngredientSections — uppercase headers", () => {
  it("splits ingredients at all-uppercase dividers", () => {
    const items = ["DRESSING", "3 tbsp olive oil", "1 tbsp vinegar", "SALAD", "100g lettuce"];
    const result = parseIngredientSections(items);
    expect(result).toHaveLength(2);
    expect(result[0].sectionName).toBe("DRESSING");
    expect(result[1].sectionName).toBe("SALAD");
  });
});

describe("parseIngredientSections — mixed headers", () => {
  it("handles a mix of colon and uppercase headers in one list", () => {
    const items = [
      "For the base:",
      "200g biscuits",
      "TOPPING",
      "300g cream cheese",
      "100g sugar",
    ];
    const result = parseIngredientSections(items);
    expect(result).toHaveLength(2);
    expect(result[0].sectionName).toBe("For the base");
    expect(result[1].sectionName).toBe("TOPPING");
  });

  it("cleans unicode artifacts in ingredient text", () => {
    const items = ["200g butter", "Bake at 180˚C"];
    const result = parseIngredientSections(items);
    expect(result[0].ingredients[1]).toBe("Bake at 180°C");
  });
});

// ─── parseInstructionSections ─────────────────────────────────────────────────

describe("parseInstructionSections — flat string array", () => {
  it("wraps plain strings in a single Instructions section", () => {
    const raw = ["Mix the ingredients.", "Pour into a pan.", "Bake 30 minutes."];
    const result = parseInstructionSections(raw);
    expect(result).toHaveLength(1);
    expect(result[0].sectionName).toBe("Instructions");
    expect(result[0].steps).toHaveLength(3);
    expect(result[0].steps[0]).toEqual({ stepNumber: 1, text: "Mix the ingredients." });
    expect(result[0].steps[2].stepNumber).toBe(3);
  });
});

describe("parseInstructionSections — HowToStep objects", () => {
  it("extracts .text from HowToStep objects into a single section", () => {
    const raw = [
      { "@type": "HowToStep", text: "Preheat oven to 200°C." },
      { "@type": "HowToStep", text: "Grease a baking tin." },
      { "@type": "HowToStep", text: "Bake for 25 minutes." },
    ];
    const result = parseInstructionSections(raw);
    expect(result).toHaveLength(1);
    expect(result[0].sectionName).toBe("Instructions");
    expect(result[0].steps[1].text).toBe("Grease a baking tin.");
  });

  it("assigns sequential step numbers starting at 1", () => {
    const raw = [
      { "@type": "HowToStep", text: "Step A" },
      { "@type": "HowToStep", text: "Step B" },
    ];
    const result = parseInstructionSections(raw);
    expect(result[0].steps.map((s) => s.stepNumber)).toEqual([1, 2]);
  });
});

describe("parseInstructionSections — HowToSection objects", () => {
  it("creates one InstructionSection per HowToSection", () => {
    const raw = [
      {
        "@type": "HowToSection",
        name: "Make the dough",
        itemListElement: [
          { "@type": "HowToStep", text: "Combine flour and yeast." },
          { "@type": "HowToStep", text: "Knead for 10 minutes." },
        ],
      },
      {
        "@type": "HowToSection",
        name: "Bake",
        itemListElement: [
          { "@type": "HowToStep", text: "Preheat oven to 220˚C." },
          { "@type": "HowToStep", text: "Bake for 30 minutes." },
        ],
      },
    ];
    const result = parseInstructionSections(raw);
    expect(result).toHaveLength(2);
    expect(result[0].sectionName).toBe("Make the dough");
    expect(result[0].steps).toHaveLength(2);
    expect(result[0].steps[0].text).toBe("Combine flour and yeast.");
    expect(result[1].sectionName).toBe("Bake");
    expect(result[1].steps[0].text).toBe("Preheat oven to 220°C."); // ˚ → °
  });

  it("resets stepNumber to 1 for each section", () => {
    const raw = [
      {
        "@type": "HowToSection",
        name: "Prep",
        itemListElement: [
          { "@type": "HowToStep", text: "Chop vegetables." },
          { "@type": "HowToStep", text: "Boil water." },
        ],
      },
      {
        "@type": "HowToSection",
        name: "Cook",
        itemListElement: [{ "@type": "HowToStep", text: "Add vegetables." }],
      },
    ];
    const result = parseInstructionSections(raw);
    expect(result[0].steps.map((s) => s.stepNumber)).toEqual([1, 2]);
    expect(result[1].steps.map((s) => s.stepNumber)).toEqual([1]);
  });
});

describe("parseInstructionSections — edge cases", () => {
  it("returns empty array for null/undefined", () => {
    expect(parseInstructionSections(undefined)).toEqual([]);
    expect(parseInstructionSections(null)).toEqual([]);
  });

  it("wraps a single string (non-array) in a section", () => {
    const result = parseInstructionSections("Mix everything together.");
    expect(result).toHaveLength(1);
    expect(result[0].steps[0].text).toBe("Mix everything together.");
  });
});

// ─── ingestRecipeFromHtml — end-to-end fixtures ────────────────────────────────

describe("ingestRecipeFromHtml — root-level recipe", () => {
  it("parses a straightforward root-level Recipe", () => {
    const html = htmlWithJsonLd({ "@context": "https://schema.org", ...BASE_RECIPE });
    const result = ingestRecipeFromHtml(html);

    expect(result.title).toBe("Test Recipe");
    expect(result.description).toBe("A test recipe.");
    expect(result.prepTimeMinutes).toBe(10);
    expect(result.cookTimeMinutes).toBe(20);
    expect(result.totalTimeMinutes).toBe(30);
    expect(result.cuisine).toBe("Dutch");
    expect(result.category).toBe("Dinner");
  });

  it("normalises nutrition fields", () => {
    const html = htmlWithJsonLd({ "@context": "https://schema.org", ...BASE_RECIPE });
    const { nutrition } = ingestRecipeFromHtml(html);
    expect(nutrition.calories).toBe("350 kcal");
    expect(nutrition.protein).toBe("12g");
    expect(nutrition.carbs).toBe("45g");
    expect(nutrition.fat).toBe("8g");
  });

  it("returns null nutrition fields when absent", () => {
    const recipe = { ...BASE_RECIPE };
    delete (recipe as Partial<typeof BASE_RECIPE>).nutrition;
    const html = htmlWithJsonLd(recipe);
    const { nutrition } = ingestRecipeFromHtml(html);
    expect(nutrition.calories).toBeNull();
    expect(nutrition.protein).toBeNull();
  });

  it("parses ingredients into default section", () => {
    const html = htmlWithJsonLd({ ...BASE_RECIPE });
    const { ingredientSections } = ingestRecipeFromHtml(html);
    expect(ingredientSections).toHaveLength(1);
    expect(ingredientSections[0].sectionName).toBe("Ingredients");
    expect(ingredientSections[0].ingredients).toEqual(["200g flour", "2 eggs", "100ml milk"]);
  });
});

describe("ingestRecipeFromHtml — @graph layout", () => {
  it("finds Recipe inside a @graph block alongside other node types", () => {
    const payload = {
      "@context": "https://schema.org",
      "@graph": [
        { "@type": "WebSite", url: "https://example.com" },
        { "@type": "BreadcrumbList" },
        { ...BASE_RECIPE },
      ],
    };
    const result = ingestRecipeFromHtml(htmlWithJsonLd(payload));
    expect(result.title).toBe("Test Recipe");
    expect(result.prepTimeMinutes).toBe(10);
  });

  it("finds Recipe nested inside a @graph item's mainEntity", () => {
    const payload = {
      "@graph": [
        { "@type": "WebSite" },
        {
          "@type": "WebPage",
          mainEntity: { ...BASE_RECIPE },
        },
      ],
    };
    const result = ingestRecipeFromHtml(htmlWithJsonLd(payload));
    expect(result.title).toBe("Test Recipe");
  });

  it("handles multiple JSON-LD script tags — uses the one with Recipe", () => {
    const html = `<!DOCTYPE html><html><head>
      <script type="application/ld+json">{"@type":"WebSite","name":"Food Blog"}</script>
      <script type="application/ld+json">${JSON.stringify({ ...BASE_RECIPE })}</script>
    </head><body></body></html>`;
    const result = ingestRecipeFromHtml(html);
    expect(result.title).toBe("Test Recipe");
  });
});

describe("ingestRecipeFromHtml — instruction layouts", () => {
  it("handles flat string instructions", () => {
    const recipe = {
      ...BASE_RECIPE,
      recipeInstructions: ["Mix.", "Bake.", "Serve."],
    };
    const { instructionSections } = ingestRecipeFromHtml(htmlWithJsonLd(recipe));
    expect(instructionSections).toHaveLength(1);
    expect(instructionSections[0].sectionName).toBe("Instructions");
    expect(instructionSections[0].steps).toHaveLength(3);
    expect(instructionSections[0].steps[0]).toEqual({ stepNumber: 1, text: "Mix." });
  });

  it("handles HowToStep array instructions", () => {
    const recipe = {
      ...BASE_RECIPE,
      recipeInstructions: [
        { "@type": "HowToStep", text: "Preheat oven." },
        { "@type": "HowToStep", text: "Mix batter." },
        { "@type": "HowToStep", text: "Pour and bake." },
      ],
    };
    const { instructionSections } = ingestRecipeFromHtml(htmlWithJsonLd(recipe));
    expect(instructionSections[0].steps[1].text).toBe("Mix batter.");
  });

  it("handles HowToSection structured instructions", () => {
    const recipe = {
      ...BASE_RECIPE,
      recipeInstructions: [
        {
          "@type": "HowToSection",
          name: "Prepare dough",
          itemListElement: [
            { "@type": "HowToStep", text: "Mix flour and water." },
            { "@type": "HowToStep", text: "Rest 1 hour." },
          ],
        },
        {
          "@type": "HowToSection",
          name: "Bake",
          itemListElement: [{ "@type": "HowToStep", text: "Bake at 200˚C for 20 minutes." }],
        },
      ],
    };
    const { instructionSections } = ingestRecipeFromHtml(htmlWithJsonLd(recipe));
    expect(instructionSections).toHaveLength(2);
    expect(instructionSections[0].sectionName).toBe("Prepare dough");
    expect(instructionSections[0].steps).toHaveLength(2);
    expect(instructionSections[1].sectionName).toBe("Bake");
    expect(instructionSections[1].steps[0].text).toBe("Bake at 200°C for 20 minutes.");
  });
});

describe("ingestRecipeFromHtml — multi-section ingredients", () => {
  it("splits ingredients at colon headers", () => {
    const recipe = {
      ...BASE_RECIPE,
      recipeIngredient: [
        "For the pastry:",
        "250g flour",
        "125g cold butter",
        "For the filling:",
        "500g apples",
        "2 tbsp brown sugar",
        "1 tsp cinnamon",
      ],
    };
    const { ingredientSections } = ingestRecipeFromHtml(htmlWithJsonLd(recipe));
    expect(ingredientSections).toHaveLength(2);
    expect(ingredientSections[0].sectionName).toBe("For the pastry");
    expect(ingredientSections[0].ingredients).toHaveLength(2);
    expect(ingredientSections[1].sectionName).toBe("For the filling");
    expect(ingredientSections[1].ingredients).toHaveLength(3);
  });

  it("splits ingredients at uppercase headers", () => {
    const recipe = {
      ...BASE_RECIPE,
      recipeIngredient: [
        "BASE",
        "200g digestive biscuits",
        "100g melted butter",
        "FILLING",
        "400g cream cheese",
        "150g sugar",
      ],
    };
    const { ingredientSections } = ingestRecipeFromHtml(htmlWithJsonLd(recipe));
    expect(ingredientSections).toHaveLength(2);
    expect(ingredientSections[0].sectionName).toBe("BASE");
    expect(ingredientSections[1].sectionName).toBe("FILLING");
  });
});

describe("ingestRecipeFromHtml — cuisine and category arrays", () => {
  it("returns arrays when recipeCuisine/recipeCategory are arrays", () => {
    const recipe = {
      ...BASE_RECIPE,
      recipeCuisine: ["Dutch", "Belgian"],
      recipeCategory: ["Dessert", "Cake"],
    };
    const result = ingestRecipeFromHtml(htmlWithJsonLd(recipe));
    expect(result.cuisine).toEqual(["Dutch", "Belgian"]);
    expect(result.category).toEqual(["Dessert", "Cake"]);
  });
});

describe("ingestRecipeFromHtml — time fields", () => {
  it("sets time fields to null when absent from node", () => {
    const { prepTime: _p, cookTime: _c, totalTime: _t, ...noTimes } = BASE_RECIPE as Record<
      string,
      unknown
    >;
    const result = ingestRecipeFromHtml(htmlWithJsonLd(noTimes));
    expect(result.prepTimeMinutes).toBeNull();
    expect(result.cookTimeMinutes).toBeNull();
    expect(result.totalTimeMinutes).toBeNull();
  });

  it("correctly converts complex durations", () => {
    const recipe = {
      ...BASE_RECIPE,
      prepTime: "PT45M",
      cookTime: "PT1H30M",
      totalTime: "PT2H15M",
    };
    const result = ingestRecipeFromHtml(htmlWithJsonLd(recipe));
    expect(result.prepTimeMinutes).toBe(45);
    expect(result.cookTimeMinutes).toBe(90);
    expect(result.totalTimeMinutes).toBe(135);
  });
});

// ─── Error handling ───────────────────────────────────────────────────────────

describe("ingestRecipeFromHtml — error handling", () => {
  it("throws RecipeIngestionError with code UNSUPPORTED_RECIPE_FORMAT when no recipe found", () => {
    const html = htmlWithJsonLd({ "@type": "WebPage", name: "About Us" });
    expect(() => ingestRecipeFromHtml(html)).toThrowError(RecipeIngestionError);
    try {
      ingestRecipeFromHtml(html);
    } catch (err) {
      expect((err as RecipeIngestionError).code).toBe("UNSUPPORTED_RECIPE_FORMAT");
    }
  });

  it("throws UNSUPPORTED_RECIPE_FORMAT when page has no JSON-LD at all", () => {
    const html = "<html><body><h1>Hello world</h1></body></html>";
    expect(() => ingestRecipeFromHtml(html)).toThrowError(RecipeIngestionError);
  });

  it("throws UNSUPPORTED_RECIPE_FORMAT when all JSON-LD blocks are malformed", () => {
    const html = `<script type="application/ld+json">{ INVALID }</script>`;
    expect(() => ingestRecipeFromHtml(html)).toThrowError(RecipeIngestionError);
  });

  it("skips a malformed JSON-LD block and finds Recipe in a later valid block", () => {
    const html = `<html><head>
      <script type="application/ld+json">{ BROKEN }</script>
      <script type="application/ld+json">${JSON.stringify({ ...BASE_RECIPE })}</script>
    </head></html>`;
    const result = ingestRecipeFromHtml(html);
    expect(result.title).toBe("Test Recipe");
  });

  it("RecipeIngestionError has correct name property", () => {
    try {
      ingestRecipeFromHtml("<html></html>");
    } catch (err) {
      expect((err as RecipeIngestionError).name).toBe("RecipeIngestionError");
    }
  });
});
