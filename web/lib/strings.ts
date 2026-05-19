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
    challenge:     "Uitdaging",
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

  // Recipes ingestion page
  recipes: {
    pageTitle:      "Recept inladen",
    pageSubtitle:   "Plak een receptlink uit een kookblog of -website. Wij halen de ingrediënten en bereiding op via de gestructureerde gegevens van de pagina.",
    inputPlaceholder: "https://www.kookblog.nl/recept/…",
    inputLabel:     "Receptlink",
    submitBtn:      "Laden",
    loadingBtn:     "Bezig…",
    loadingMsg:     "Recept ophalen…",
    errorTitle:     "Niet gelukt",
    errorFallback:  "Er is een onbekende fout opgetreden.",
    errorNetwork:   "Kon de server niet bereiken. Controleer je verbinding.",
    retryBtn:       "Opnieuw proberen",
    clearBtn:       "Nieuw recept",
    hintText:       "Werkt met recepten die gestructureerde data (Schema.org) gebruiken — de meeste grote kookblogs doen dit automatisch.",
    sectionIngredients: "Ingrediënten",
    sectionInstructions: "Bereiding",
    sectionNutrition: "Voedingswaarden",
    labelPrepTime:  "Voorbereiding",
    labelCookTime:  "Bereiden",
    labelTotalTime: "Totaal",
    labelMinutes:   "min",
    labelCalories:  "Kcal",
    labelProtein:   "Eiwit",
    labelCarbs:     "Koolh.",
    labelFat:       "Vet",
    saveBtn:        "Bewaren",
    savedBtn:       "Bewaard",
    savedSection:   "Opgeslagen recepten",
    savedRemoveLabel: "Verwijderen",
  },

  // Technieken page
  techniques: {
    pageTitle:       "Technieken",
    pageSubtitle:    "Fundamentele bereidingsmethoden voor de Nederlandse keuken",
    categorySnijden: "Snijdtechnieken",
    categoryGaring: "Garingsmethoden",
    categorySmaak: "Smaakgebouw",
    categoryEiwitten: "Eiwitten",
    categorySauzen: "Sauzen & Fonds",
    categoryGroenten: "Groenten voorbereiding",
    categoryGereedschap: "Gereedschap",
    subcategoryKlassieken: "Klassieken",
    subcategoryHoogVuur: "Hoog vuur",
    subcategoryMiddelVuur: "Middel vuur",
    subcategoryLaagVuur: "Laag vuur",
    subcategoryVlees: "Vlees",
    subcategoryGevogelte: "Gevogelte",
    subcategoryVis: "Vis & Zeevruchten",
    subcategoryEieren: "Eieren",
    subcategoryKlassiekeSauzen: "Klassieke sauzen",
    subcategoryEmulsies: "Emulsies",
    subcategoryFonds: "Fonds & Bouillons",
    subcategoryBereidingsmethoden: "Bereidingsmethoden",
    subcategorySmaaklagen: "Smaaklagen",
    subcategoryKookgerei: "Kookgerei",
    levelBasis:      "Basis",
    levelGemiddeld:  "Gemiddeld",
    levelGevorderd:  "Gevorderd",
    heatHoog:        "Hoog vuur",
    heatMiddel:      "Middel vuur",
    heatLaag:        "Laag vuur",
    heatGeen:        "Geen vuur",
    sectionTips:     "Tips",
    sectionRelatedVeg: "Aanbevolen groenten",
    sectionRelatedTechniques: "Verwante technieken",
    sectionCookware: "Benodigde kookgerei",
    searchPlaceholder: "Techniek zoeken…",
    searchAriaLabel: "Technieken zoeken",
    emptyState:      "Geen technieken gevonden. Probeer de zoekopdracht te wissen.",
    closeAriaLabel:  "Sluiten",
    backButton:      "Terug",
  },

  // Challenge page
  challenge: {
    pageTitle:              "Kookuitdaging",
    pageSubtitle:           "Typ een ingrediënt en ontvang een gerichte kookoefening om een specifieke techniek te oefenen.",
    inputPlaceholder:       "Bijv. aardappel, wortel, ui…",
    inputLabel:             "Ingrediënt",
    submitBtn:              "Geef uitdaging",
    loadingBtn:             "Bezig…",
    loadingMsg:             "Uitdaging genereren…",
    tryAnotherBtn:          "Andere uitdaging",
    clearBtn:               "Wissen",
    errorFallback:          "Er is een onbekende fout opgetreden.",
    errorNetwork:           "Kon de server niet bereiken. Controleer je verbinding.",
    retryBtn:               "Opnieuw proberen",
    labelSkill:             "Techniek",
    labelTime:              "Tijd",
    labelMinutes:           "min",
    labelWhy:               "Waarom deze uitdaging?",
    difficultyBeginner:     "Beginner",
    difficultyIntermediate: "Gevorderd",
    difficultyAdvanced:     "Expert",
    errorTitle:             "Niet gelukt",
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
