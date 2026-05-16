/**
 * All Dutch UI strings in one place.
 * Import from here instead of hardcoding labels in components.
 * Never render the `name` field from VegItem — use `nl` instead.
 */

export const UI = {
  // Brand
  brandTitle: "Mise",
  brandSub: "een kooksalmanak",

  // Nav items (id matches Page type in page.tsx)
  nav: {
    seasonal:      "Seizoenen",
    calendar:      "Kalender",
    recipes:       "Recepten",
    substitutions: "Vervangers",
    techniques:    "Technieken",
    foraging:      "Plukken",
    soon:          "binnenkort",
  },

  // Footer
  footer: {
    regionLabel: "Regio",
    regionName:  "Nederland",
    build:       "v0.1 · seizoensmodule",
  },

  // Hero / page header
  hero: {
    eyebrowLoc: "📍 Nederland",
    title:      "Wat is er nu",
    titleEm:    "in het seizoen",
    statPeak:      "op hoogtepunt",
    statShoulder:  "beschikbaar",
    statSoon:      "binnenkort",
    statEnding:    "aflopend",
  },

  // Controls
  controls: {
    searchPlaceholder: "Zoeken…",
    searchAriaLabel:   "Groenten zoeken",
    gridAriaLabel:     "Rasterweergave",
    listAriaLabel:     "Lijstweergave",
  },

  // Empty state
  emptyState: "Geen groenten gevonden. Probeer de zoekopdracht te wissen.",

  // Filter pills
  filter: {
    all:      "Alles in seizoen",
    peak:     "Op hoogtepunt",
    shoulder: "Beschikbaar",
    soon:     "Binnenkort",
    ending:   "Aflopend",
  },

  // Status labels (must match VegStatus values)
  status: {
    peak:     "Op hoogtepunt",
    soon:     "Binnenkort",
    ending:   "Aflopend",
    shoulder: "Beschikbaar",
    off:      "Buiten seizoen",
  },

  // Detail panel sections
  detail: {
    close:           "Sluiten",
    sectionSeason:   "Seizoen",
    legendPeak:      "Hoogtepunt",
    legendShoulder:  "Beschikbaar",
    sectionStorage:  "Bewaren",
    sectionPrep:     "Bereiden",
    sectionPairings: "Combineert goed met",
    sectionSubs:     "Als je het niet hebt",
    subsTag:         "Vervangers · binnenkort",
    subsPreviewPre:  "Mogelijke vervangers:",
    subsNote:        "Een volledige vervanger- & combinatieverkenner komt in de volgende versie.",
    sectionRecipes:  "Bereid het",
    recipesTag:      "Recepten · binnenkort",
    recipeTechPre:   "Bereidingstip:",
    recipesNote:     "Gekoppelde recepten verschijnen met de receptenmodule.",
    sectionForaging: "Vind het in de buurt",
    foragingTag:     "Markten & plukken · binnenkort",
    foragingNote:    "Boerenmarktschema's en (voor wilde soorten) veilige plekplekken komen hier.",
    difficultyEasy:   "Makkelijk",
    difficultyMedium: "Gemiddeld",
    difficultyHard:   "Moeilijk",
  },

  // Future peek section
  futurePeek: {
    label: "Binnenkort",
    items: [
      { label: "Recepten op ingrediënt",   note: "We stellen gerechten voor die gebruikmaken van wat er nu piekt." },
      { label: "Vervangers & combinaties", note: "Vind wat er in de plaats komt — en wat er goed bij past." },
      { label: "Plukkaart",                note: "Waar daslook, zuring en cantharellen groeien in Nederland." },
    ],
  },
} as const;
