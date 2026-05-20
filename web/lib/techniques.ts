export type TechniqueLevel = 'basis' | 'gemiddeld' | 'gevorderd';
export type TechniqueCategory = 'snijden' | 'garing' | 'smaak' | 'eiwitten' | 'sauzen' | 'groenten' | 'gereedschap';
export type HeatLevel = 'hoog' | 'middel' | 'laag' | 'geen';

export interface Technique {
  id: string;
  nl: string;
  subtitle: string;
  category: TechniqueCategory;
  subcategory?: string;
  parentId?: string; // Points to the foundational concept node
  level: TechniqueLevel;
  heatLevel?: HeatLevel;
  body: string[];
  tips: string[];
  relatedVeg?: string[];
  relatedTechniqueIds?: string[];
  cookware?: string[];
}

export const TECHNIQUES: Technique[] = [
  // ─── METHODEN › Snijden ──────────────────────────────────────────────────
  {
    id: "julienne",
    nl: "Julienne",
    subtitle: "Dunne reepjes snijden",
    category: "snijden",
    subcategory: "Klassieken",
    level: "basis",
    heatLevel: "geen",
    body: [
      "De julienne snijtechniek geeft dunne, gelijkmatige reepjes van zo'n 3 mm breed en 5–6 cm lang. De term wordt gebruikt voor zowel de snijding als de klassieke Franse soep met fijngesneden groenten. Julienne dient als basis voor brunoise, maar ook als garnituur of in roerbakgerechten.",
      "Begin met het maken van plakken van gelijke dikte, stapel deze en snijd ze vervolgens in lange reepjes. Voor ronde groenten zoals wortels geef je ze eerst een platte kant door een dunne plak af te snijden, zodat de groente stabiel op de snijplank ligt.",
    ],
    tips: [
      "Werk met een mandoline voor extra gelijkmatige resultaten bij courgette of komkommer.",
      "Snij groenten met een harde structuur — zoals wortel of koolrabi — wat dunner dan je denkt: de reepjes krimpen bij verhitting.",
      "Julienne van prei of venkel geeft een prachtige basis voor soepen en bouillons.",
      "Houd je vingers gekromd en gebruik de knokkels als gids langs het mes.",
    ],
    relatedVeg: ["Carrot", "Kohlrabi", "Celery", "Fennel", "Courgette"],
    relatedTechniqueIds: ["brunoise", "chiffonade"],
    cookware: ["Koksmes (20–25 cm)", "Snijplank", "Mandoline (optioneel)"],
  },
  {
    id: "brunoise",
    nl: "Brunoise",
    subtitle: "Fijn dobbelsnede van 2–3 mm",
    category: "snijden",
    subcategory: "Klassieken",
    parentId: "julienne",
    level: "basis",
    heatLevel: "geen",
    body: [
      "De brunoise is een van de meest fundamentele snijdingen in de klassieke keuken. Je snijdt groenten eerst in plakken, dan in reepjes (julienne), en tot slot in fijne blokjes van 2 tot 3 millimeter. Deze kleine afmeting zorgt voor een gelijkmatige garing en een elegante presentatie.",
      "Precisie is essentieel bij de brunoise. Gebruik een scherp mes ...",
    ],
    tips: [
      "Gebruik een scherp koksmes — een bot mes verbrijzelt de groente in plaats van te snijden.",
      "Koel de groente licht voor het snijden: koude wortels en selderij zijn steviger en makkelijker te verwerken.",
      "Bewaar de snijresten voor fond of soep — verspil nooit goede groente.",
      "Oefen eerst met wortel: die heeft de juiste stijfheid voor beginners.",
    ],
    relatedVeg: ["Carrot", "Celeriac", "Celery", "Beetroot"],
    relatedTechniqueIds: ["julienne"],
    cookware: ["Koksmes (20–25 cm)", "Snijplank"],
  },
  {
    id: "batonnet",
    nl: "Bâtonnet",
    subtitle: "Dikke staafjes snijden van 6x6 mm",
    category: "snijden",
    subcategory: "Klassieken",
    level: "basis",
    heatLevel: "geen",
    body: [
      "Bâtonnet betekent letterlijk 'stokje' en vormt de opstap naar grovere snijwijzen of grotere blokjes (macedoine). De afmetingen zijn standaard 6 mm bij 6 mm breed en ongeveer 5 tot 6 cm lang. Het is de klassieke snijwijze voor authentieke Franse frites en stevige groentegarnituren.",
      "Net als bij julienne snijd je eerst de zijkanten van de groente recht af om een stabiel blok te vormen, waarna je gelijkmatige plakken en vervolgens staven snijdt."
    ],
    tips: [
      "Zorg dat het product stabiel ligt; snijd altijd eerst een dun plakje van de onderkant af.",
      "Gebruik restjes die overblijven bij het recht snijden direct in soepen of bouillons.",
      "Bâtonnet-snijwerk leent zich uitstekend voor groenten die geroosterd of gefrituurd gaan worden."
    ],
    relatedVeg: ["Carrot", "Parsnip", "Celeriac", "Potato"],
    relatedTechniqueIds: ["julienne", "brunoise"],
    cookware: ["Koksmes (20–25 cm)", "Snijplank"]
  },
  {
    id: "snipperen",
    nl: "Snipperen",
    subtitle: "Fijnsnijden van uien en sjalotten",
    category: "snijden",
    subcategory: "Klassieken",
    level: "basis",
    heatLevel: "geen",
    body: [
      "Snipperen is een specifieke snijtechniek die wordt gebruikt voor uien en sjalotten om gelijkmatige, fijne stukjes te krijgen. Door de natuurlijke gelaagdheid van de ui te gebruiken, kan men met een paar strategische sneden snel en efficiënt te werk gaan.",
      "Laat bij het snipperen altijd de wortelzijde intact; dit houdt de ui bij elkaar tijdens het snijden. Maak horizontale en verticale inkepingen tot vlak voor de wortel, en snijd de ui vervolgens dwars in fijne stukjes."
    ],
    tips: [
      "Houd het koksmes scherp om de cellen van de ui te snijden in plaats van te pletten, wat tranende ogen vermindert.",
      "Hoe fijner de ui gesnipperd is, hoe sneller hij smelt en karameliseert in de pan.",
      "Sjalotten snipper je nog fijner dan uien vanwege hun sterkere, verfijndere smaak."
    ],
    relatedVeg: ["Yellow onion", "Shallot"],
    relatedTechniqueIds: ["brunoise"],
    cookware: ["Koksmes (20–25 cm)", "Snijplank"]
  },
  {
    id: "chiffonade",
    nl: "Chiffonade",
    subtitle: "Rolsnede voor bladgroenten",
    category: "snijden",
    subcategory: "Klassieken",
    level: "basis",
    heatLevel: "geen",
    body: [
      "Bij de chiffonade rol je bladgroenten strak op tot een sigaarvorm en snijdt ze vervolgens in dunne linten van 1–3 mm...",
    ],
    tips: [
      "Droog de bladeren goed voor het snijden — vochtige bladeren plakken samen en snijden minder schoon.",
      "Snij basilicum altijd op het allerlaatste moment: het oxideeringsproces gaat razendsnel na het snijden.",
    ],
    relatedVeg: ["Spinach", "Sorrel", "Chard", "Butterhead lettuce"],
    relatedTechniqueIds: ["julienne"],
    cookware: ["Koksmes (20–25 cm)", "Snijplank"],
  },
  {
    id: "tournee",
    nl: "Tourné",
    subtitle: "Gedraaid snijden voor presentatie",
    category: "snijden",
    subcategory: "Klassieken",
    level: "gevorderd",
    heatLevel: "geen",
    body: [
      "Bij de tourné-snijding geef je wortelgroenten een klassieke, zevenzijdige footballvorm met afgeronde uiteinden...",
    ],
    tips: [
      "Gebruik altijd een scherp tourné-mesje — probeer niet met een gewoon koksmes, dat geeft te weinig controle.",
    ],
    relatedVeg: ["Carrot", "Parsnip", "Turnip", "New potato"],
    relatedTechniqueIds: ["brunoise"],
    cookware: ["Tourné-mesje", "Snijplank"],
  },

  // ─── METHODEN › Hoog vuur ────────────────────────────────────────────────
  {
    id: "sauteren",
    nl: "Sauteren",
    subtitle: "Snel roerbakken op hoog vuur",
    category: "garing",
    subcategory: "Hoog vuur",
    level: "basis",
    heatLevel: "hoog",
    body: [
      "Sauteren betekent letterlijk 'laten springen' (van het Franse sauter): kleine stukken voedsel worden in weinig vet op hoog vuur snel gaar gemaakt...",
    ],
    tips: [
      "Verwarm de pan altijd eerst goed voor je het vet toevoegt — zo voorkom je aanplakken.",
    ],
    relatedVeg: ["Courgette", "Bell pepper", "Green beans", "Spinach"],
    relatedTechniqueIds: ["groenten-roerbakken", "schroeien"],
    cookware: ["Sauteerpan of wok", "Houten spatel of tang"],
  },
  {
    id: "schroeien",
    nl: "Schroeien",
    subtitle: "Aanbraden voor korst",
    category: "garing",
    subcategory: "Hoog vuur",
    level: "gemiddeld",
    heatLevel: "hoog",
    body: [
      "Schroeien (of aanbraden) is het snel bakken van een oppervlak op zeer hoog vuur om een goudbruine, aromatische korst te vormen...",
    ],
    tips: [
      "De pan moet echt goed heet zijn voordat je het product inlegt — een druppel water moet direct sissen en verdampen.",
    ],
    relatedTechniqueIds: ["sauteren", "vlees-schroeien", "vis-pansearen"],
    cookware: ["Gietijzeren pan of RVS koekenpan", "Tang"],
  },
  {
    id: "gratineren",
    nl: "Gratineren",
    subtitle: "Goudbruin afwerken onder grill",
    category: "garing",
    subcategory: "Hoog vuur",
    level: "basis",
    heatLevel: "hoog",
    body: [
      "Gratineren is het goudbruin en krokant afwerken van een gerecht onder de grill of in een hete oven...",
    ],
    tips: [
      "Zet de ovenschaal dicht bij het grillvlak voor een snellere, knapperigere korst.",
    ],
    relatedVeg: ["Cauliflower", "Fennel", "Chicory", "Endive"],
    relatedTechniqueIds: ["bechamel"],
    cookware: ["Gratineerschaal (ovenvast)", "Grillstand van oven"],
  },

  // ─── METHODEN › Middel vuur ──────────────────────────────────────────────
  {
    id: "smoren",
    nl: "Smoren",
    subtitle: "Langzaam garen in vocht",
    category: "garing",
    subcategory: "Middel vuur",
    level: "gemiddeld",
    heatLevel: "middel",
    body: [
      "Smoren is een techniek waarbij voedsel eerst wordt aangebraden op hoog vuur en vervolgens langzaam gaart in een kleine hoeveelheid vocht...",
    ],
    tips: [
      "Bruin het vlees of de groenten goed aan voordat je vloeistof toevoegt — dat anbraden-laagje is de basis van de smaak.",
    ],
    relatedVeg: ["Celeriac", "Carrot", "Parsnip", "Fennel", "Leek"],
    relatedTechniqueIds: ["sudderen", "vlees-smoren", "groenten-smoren"],
    cookware: ["Braadpan met deksel", "Koekenpan voor aanbraden"],
  },
  {
    id: "sudderen",
    nl: "Sudderen",
    subtitle: "Zacht laten trekken",
    category: "garing",
    subcategory: "Middel vuur",
    level: "basis",
    heatLevel: "middel",
    body: [
      "Sudderen is garen in vloeistof net onder het kookpunt, bij 85–95°C. Er stijgen af en toe kleine belletjes op, maar de vloeistof kookt nooit heftig...",
    ],
    tips: [
      "Zet het vuur zo laag dat er slechts zelden een belletje opstijgt — actief borrelen is al te heet.",
    ],
    relatedVeg: ["Carrot", "Celeriac", "Leek", "Jerusalem artichoke"],
    relatedTechniqueIds: ["smoren", "kippenfond", "groentebouillon"],
    cookware: ["Soeppan of steelpan", "Schuimspaan"],
  },
  {
    id: "koken",
    nl: "Koken",
    subtitle: "Garen in kolkend water op 100°C",
    category: "garing",
    subcategory: "Middel vuur",
    level: "basis",
    heatLevel: "middel",
    body: [
      "Koken is een van de meest basale kooktechnieken waarbij ingrediënten volledig worden ondergedompeld in een vloeistof (meestal water of bouillon) die het kookpunt van 100°C heeft bereikt. Grote actieve bellen verplaatsen het voedsel voortdurend.",
      "Deze methode is ideaal voor het bereiden van zetmeelproducten zoals pasta, aardappelen en rijst, of het snel garen van taaiere peulvruchten."
    ],
    tips: [
      "Breng het water altijd eerst op een rollend kookpunt voordat je ingrediënten toevoegt.",
      "Zout het water royaal wanneer je pasta of aardappelen kookt; dit trekt diep in de kern van het product.",
      "Houd de pan onafgedekt als je groene groenten kookt om vluchtige zuren te laten ontsnappen, zodat ze hun kleur niet verliezen."
    ],
    relatedVeg: ["Potato", "Green beans"],
    relatedTechniqueIds: ["blancheren", "sudderen"],
    cookware: ["Grote kookpan"]
  },
  {
    id: "panroosteren",
    nl: "Panroosteren",
    subtitle: "Droog braden in de pan",
    category: "garing",
    subcategory: "Middel vuur",
    level: "basis",
    heatLevel: "middel",
    body: [
      "Bij panroosteren bak je ingrediënten in een droge pan — zonder olie of vet — op middelhoog vuur...",
    ],
    tips: [
      "Verwarm de pan voor op middelhoog vuur voor je de specerijen toevoegt.",
    ],
    relatedVeg: ["Fennel", "Carrot"],
    relatedTechniqueIds: ["groenten-roasten"],
    cookware: ["Koekenpan (bij voorkeur gietijzer)"],
  },

  // ─── METHODEN › Laag vuur ────────────────────────────────────────────────
  {
    id: "langzaam-garen",
    nl: "Langzaam garen",
    subtitle: "Slow cooking in oven of pan",
    category: "garing",
    subcategory: "Laag vuur",
    level: "gemiddeld",
    heatLevel: "laag",
    body: [
      "Langzaam garen is het koken van voedsel bij lage temperatures (60–120°C) gedurende langere tijd...",
    ],
    tips: [
      "Bak vlees of groenten altijd eerst aan op hoog vuur voor smaak — daarna pas de oven in op lage temperatuur.",
    ],
    relatedVeg: ["Celeriac", "Parsnip", "Swede", "Carrot", "Beetroot"],
    relatedTechniqueIds: ["smoren", "confit"],
    cookware: ["Braadpan met deksel (cocotte)", "Kernthermometer"],
  },
  {
    id: "confit",
    nl: "Confit",
    subtitle: "Garen in vet op lage temperatuur",
    category: "garing",
    subcategory: "Laag vuur",
    level: "gevorderd",
    heatLevel: "laag",
    body: [
      "Bij confit gaart voedsel volledig ondergedompeld in vet op een lage, constante temperatuur (60–90°C)...",
    ],
    tips: [
      "Gebruik milde olijfolie of kippenschmaltz voor groenten-confit — de smaak van het vet determines the output.",
    ],
    relatedVeg: ["Fennel", "New potato", "Leek", "Yellow onion"],
    relatedTechniqueIds: ["langzaam-garen", "sous-vide"],
    cookware: ["Zware pan of braadpan", "Kookthermometer", "Schuimspaan"],
  },
  {
    id: "sous-vide",
    nl: "Sous-vide",
    subtitle: "Vacuüm garen in waterbad",
    category: "garing",
    subcategory: "Laag vuur",
    level: "gevorderd",
    heatLevel: "laag",
    body: [
      "Sous-vide (Frans voor 'onder vacuüm') is een techniek waarbij voedsel vacuüm wordt verpakt en dan in een waterbad...",
    ],
    tips: [
      "Gebruik een vacuümzakje van goede kwaliteit en zorg dat er geen lucht achterblijft...",
    ],
    relatedVeg: ["Carrot", "Beetroot", "New potato", "Fennel"],
    relatedTechniqueIds: ["confit", "langzaam-garen"],
    cookware: ["Sous-vide circulator", "Vacuümzakjes", "Vacuümpomp"],
  },

  // ─── METHODEN › Smaaklagen ───────────────────────────────────────────────
  {
    id: "zout-timing",
    nl: "Zout toevoegen",
    subtitle: "Wanneer en hoe zout toevoegen",
    category: "smaak",
    subcategory: "Smaaklagen",
    level: "basis",
    heatLevel: "geen",
    body: [
      "Zout is het meest krachtige smaakmaker in de keuken, maar het moment van toevoegen bepaalt het effect...",
    ],
    tips: [
      "Proef altijd meerdere keren tijdens het koken — niet eenmalig aan het einde.",
    ],
    relatedTechniqueIds: ["marineren", "fond-deglaceren", "pekelen"],
    cookware: ["Zoutvat"],
  },
  {
    id: "pekelen",
    nl: "Pekelen",
    subtitle: "Hydrateren en kruiden met zoutwater",
    category: "smaak",
    subcategory: "Smaaklagen",
    parentId: "zout-timing",
    level: "basis",
    heatLevel: "geen",
    body: [
      "Pekelen is het weken van ingrediënten (vooral vlees of gevogelte) in een zoutwateroplossing (nat pekelen) of het direct inzouten (droog pekelen). Via osmose en diffusie dringt het zout diep door in de cellen, wat de eiwitstructuur verandert en zorgt dat het product tot 30% meer vocht vasthoudt tijdens de garing.",
      "Nat pekelen is ideaal voor mager vlees dat snel uitdroogt, zoals kipfilet of varkenshaas. Droog pekelen werkt uitstekend voor biefstuk en eendenborst, omdat het tevens het oppervlak uitdroogt, wat leidt tot een superieure korstvorming bij het bakken."
    ],
    tips: [
      "Houd voor een standaard natte pekel een verhouding aan van 50 tot 60 gram zout per liter water.",
      "Voeg aromaten zoals laurier, peperkorrels en tijm toe aan de pekel, en kook deze kort om de smaken te activeren voordat je de pekel volledig laat afkoelen.",
      "Spoel het product na nat pekelen altijd af met koud water en dep het grondig droog."
    ],
    relatedTechniqueIds: ["zout-timing", "marineren"],
    cookware: ["Grote kom of container", "Koelkast"]
  },
  {
    id: "fond-deglaceren",
    nl: "Fond deglaceren",
    subtitle: "Aanbaksels losmaken",
    category: "smaak",
    subcategory: "Smaaklagen",
    level: "gemiddeld",
    heatLevel: "middel",
    body: [
      "Deglaceren is het losmaken van de aanbaksels (fond) die aan de bodem van de pan plakken na het braden...",
    ],
    tips: [
      "Gebruik een koude of kamertemperatuur vloeistof bij het deglaceren.",
    ],
    relatedTechniqueIds: ["smoren", "vlees-schroeien", "sauteren"],
    cookware: ["RVS of gietijzeren koekenpan", "Houten spatel"],
  },
  {
    id: "emulgeren",
    nl: "Emulgeren",
    subtitle: "Vet en water verbinden",
    category: "smaak",
    subcategory: "Smaaklagen",
    level: "gemiddeld",
    heatLevel: "geen",
    body: [
      "Een emulsie is een stabiel mengsel van twee vloeistoffen die normaal niet mengen, zoals olie en water...",
    ],
    tips: [
      "Alle ingrediënten op kamertemperatuur versnelt de emulgatie.",
    ],
    relatedTechniqueIds: ["hollandaise", "mayonaise", "vinaigrette", "beurre-blanc", "monteren"],
    cookware: ["Garde", "Staafmixer", "Kom"],
  },
  {
    id: "marineren",
    nl: "Marineren",
    subtitle: "Kruiden en intrekken",
    category: "smaak",
    subcategory: "Smaaklagen",
    level: "basis",
    heatLevel: "geen",
    body: [
      "Marineren is het weken van vlees, vis of groenten in een gekruide vloeistof (marinade) voor het bereiden...",
    ],
    tips: [
      "Marineer altijd in de koelkast, nooit op kamertemperatuur.",
    ],
    relatedVeg: ["Courgette", "Aubergine", "Bell pepper", "Beetroot"],
    relatedTechniqueIds: ["groenten-rauw", "zout-timing"],
    cookware: ["Kom of ziplock-zak", "Koelkast"],
  },

  // ─── METHODEN › Kookgerei ────────────────────────────────────────────────
  {
    id: "koekenpan",
    nl: "Koekenpan",
    subtitle: "Gietijzer, rvs of anti-aanbak",
    category: "gereedschap",
    subcategory: "Kookgerei",
    level: "basis",
    heatLevel: "geen",
    body: [
      "De koekenpan is het meest veelzijdige stuk kookgerei in de keuken...",
    ],
    tips: [
      "Investeer in één goede pan per type: een gietijzeren koekenpan van 26 cm dekt de meeste bereidingen.",
    ],
    relatedTechniqueIds: ["schroeien", "sauteren", "panroosteren"],
    cookware: ["Gietijzeren koekenpan", "RVS koekenpan", "Anti-aanbak koekenpan"],
  },
  {
    id: "braadpan",
    nl: "Braadpan",
    subtitle: "Gebruik van de cocotte of Dutch oven",
    category: "gereedschap",
    subcategory: "Kookgerei",
    level: "basis",
    heatLevel: "geen",
    body: [
      "De braadpan (ook cocotte of Dutch oven) is de meest veelzijdige pan voor smoren, langzaam garen en soepen...",
    ],
    tips: [
      "Begin altijd op middelhoog vuur — een braadpan heeft tijd nodig om gelijkmatig op te warmen.",
    ],
    relatedTechniqueIds: ["smoren", "langzaam-garen", "kippenfond"],
    cookware: ["Braadpan / cocotte (4–6 L)", "Ovenhandschoenen"],
  },
  {
    id: "steelpan",
    nl: "Steelpan",
    subtitle: "Sauzen, fondsen en eieren",
    category: "gereedschap",
    subcategory: "Kookgerei",
    level: "basis",
    heatLevel: "geen",
    body: [
      "De steelpan is de werkpaard van sauzen, bouillons en smeltbewerkingen...",
    ],
    tips: [
      "Gebruik een kleinere steelpan dan je denkt nodig te hebben.",
    ],
    relatedTechniqueIds: ["bechamel", "hollandaise", "beurre-blanc"],
    cookware: ["Steelpan RVS (16–20 cm)", "Garde", "Houten lepel"],
  },

  // ─── EIWITTEN › Vlees ────────────────────────────────────────────────────
  {
    id: "vlees-schroeien",
    nl: "Vlees schroeien",
    subtitle: "Maillard-reactie en korstvorming",
    category: "eiwitten",
    subcategory: "Vlees",
    parentId: "schroeien",
    level: "gemiddeld",
    heatLevel: "hoog",
    body: [
      "Het schroeien van vlees is het aanbraden op zeer hoog vuur om een goudbruine, smaakvolle korst te vormen via de Maillard-reactie...",
    ],
    tips: [
      "Dep vlees always droog met keukenpapier voor het bakken.",
    ],
    relatedTechniqueIds: ["schroeien", "vlees-braden", "fond-deglaceren"],
    cookware: ["Gietijzeren koekenpan of RVS pan", "Tang", "Lepel voor basten"],
  },
  {
    id: "vlees-smoren",
    nl: "Vlees smoren",
    subtitle: "Taaie stukken zacht maken",
    category: "eiwitten",
    subcategory: "Vlees",
    parentId: "smoren",
    level: "gemiddeld",
    heatLevel: "middel",
    body: [
      "Vlees smoren transformeert goedkopere, taai-vezelige stukken vlees tot boterzacht, smaakintensief gerecht...",
    ],
    tips: [
      "Kies vlees met veel bindweefsel: sukade, schouder, rib en kuit worden het zachtst.",
    ],
    relatedVeg: ["Carrot", "Celeriac", "Parsnip"],
    relatedTechniqueIds: ["smoren", "vlees-schroeien", "fond-deglaceren"],
    cookware: ["Braadpan met deksel (cocotte)", "Tang", "Kernthermometer"],
  },
  {
    id: "vlees-braden",
    nl: "Vlees braden",
    subtitle: "Oven- en panbraden",
    category: "eiwitten",
    subcategory: "Vlees",
    level: "gemiddeld",
    heatLevel: "middel",
    body: [
      "Braden in de oven is de methode voor grotere stukken vlees die te groot zijn voor alleen de pan...",
    ],
    tips: [
      "Breng vlees op kamertemperatuur voor het braden — koud vlees gaart ongelijkmatig.",
    ],
    relatedTechniqueIds: ["vlees-schroeien", "vlees-smoren"],
    cookware: ["Braadslee met rooster", "Kernthermometer", "Aluminiumfolie"],
  },
  {
    id: "vlees-snijden",
    nl: "Vlees snijden",
    subtitle: "Draad snijden en carving",
    category: "eiwitten",
    subcategory: "Vlees",
    level: "basis",
    heatLevel: "geen",
    body: [
      "Vlees snijden dwars op de draad (haaks op de spiervezels) is de meest invloedrijke techniek voor een mals eindresultaat...",
    ],
    tips: [
      "Een scherp, lang vleesmes geeft schonere sneden en vereist minder kracht.",
    ],
    relatedTechniqueIds: ["vlees-braden", "vlees-smoren"],
    cookware: ["Lang vleesmes (25–30 cm)", "Vleesnaald of -vork", "Snijplank"],
  },

  // ─── EIWITTEN › Gevogelte ─────────────────────────────────────────────────
  {
    id: "gevogelte-braden",
    nl: "Gevogelte braden",
    subtitle: "Gelijkmatig garen en knapperige huid",
    category: "eiwitten",
    subcategory: "Gevogelte",
    level: "gemiddeld",
    heatLevel: "middel",
    body: [
      "Een perfect gebraden kip of eend begint met een droge huid. Dep het gevogelte goed droog...",
    ],
    tips: [
      "Leg aromaten (citroen, tijm, knoflook) in de holte voor extra smaak.",
    ],
    relatedTechniqueIds: ["vlees-braden", "gevogelte-ontleden"],
    cookware: ["Braadslee met rooster", "Kernthermometer", "Braadpenseel"],
  },
  {
    id: "gevogelte-pocheren",
    nl: "Gevogelte pocheren",
    subtitle: "Sappig en zacht",
    category: "eiwitten",
    subcategory: "Gevogelte",
    parentId: "sudderen",
    level: "gemiddeld",
    heatLevel: "laag",
    body: [
      "Pocheren van gevogelte in hete bouillon of court-bouillon geeft een van de sappigste, zachtste resultaten...",
    ],
    tips: [
      "Begin altijd met voldoende koud water en voeg de kip toe voor het vuur aan gaat.",
    ],
    relatedVeg: ["Carrot", "Celery", "Leek"],
    relatedTechniqueIds: ["sudderen", "kippenfond"],
    cookware: ["Grote soeppan", "Schuimspaan", "Kookthermometer"],
  },
  {
    id: "gevogelte-ontleden",
    nl: "Gevogelte ontleden",
    subtitle: "Uitbenen og portioneren",
    category: "eiwitten",
    subcategory: "Gevogelte",
    level: "gevorderd",
    heatLevel: "geen",
    body: [
      "Het ontleden van gevogelte — een hele kip in acht stukken verdelen — is een fundamentele keukenbekwaamheid...",
    ],
    tips: [
      "Gebruik een scherp mesje (uitbeenmesje) voor de precieze werkzaamheden.",
    ],
    relatedTechniqueIds: ["gevogelte-braden", "kippenfond"],
    cookware: ["Koksmes (20–25 cm)", "Uitbeenmesje", "Snijplank"],
  },

  // ─── EIWITTEN › Vis & Zeevruchten ────────────────────────────────────────
  {
    id: "vis-stomen",
    nl: "Vis stomen",
    subtitle: "Delicaat garen met stoom",
    category: "eiwitten",
    subcategory: "Vis & Zeevruchten",
    level: "basis",
    heatLevel: "middel",
    body: [
      "Stomen is de meest delicate methode voor het garen van vis...",
    ],
    tips: [
      "Zorg dat het water al kookt voordat je de vis erin plaatst.",
    ],
    relatedVeg: ["Fennel", "Spring onion", "Wild garlic"],
    relatedTechniqueIds: ["vis-pocheren"],
    cookware: ["Stoommandje of bamboestomer", "Soeppan of wok"],
  },
  {
    id: "vis-pansearen",
    nl: "Vis pansearen",
    subtitle: "Knapperige huid in de pan",
    category: "eiwitten",
    subcategory: "Vis & Zeevruchten",
    parentId: "schroeien",
    level: "gemiddeld",
    heatLevel: "hoog",
    body: [
      "Pansearen van vis — de huid krokant bakken in een hete pan — is een van de meest bevredigende technieken...",
    ],
    tips: [
      "Dep de vissenhuid kurkdroog — zelfs kleine hoeveelheden vocht verhinderen een knapperige huid.",
    ],
    relatedTechniqueIds: ["schroeien", "vis-pocheren"],
    cookware: ["RVS of gietijzeren koekenpan", "Spatel", "Tang"],
  },
  {
    id: "vis-pocheren",
    nl: "Vis pocheren",
    subtitle: "In bouillon of court-bouillon",
    category: "eiwitten",
    subcategory: "Vis & Zeevruchten",
    parentId: "sudderen",
    level: "basis",
    heatLevel: "laag",
    body: [
      "Court-bouillon is de klassieke vloeistof voor het pocheren van vis...",
    ],
    tips: [
      "Bereid de court-bouillon twintig minuten van tevoren zodat de smaken kunnen intrekken.",
    ],
    relatedTechniqueIds: ["vis-stomen", "vis-pansearen", "groentebouillon"],
    cookware: ["Brede lage pan", "Kookthermometer"],
  },
  {
    id: "zeevruchten-bereiden",
    nl: "Zeevruchten bereiden",
    subtitle: "Mosselen, garnalen en inktvis",
    category: "eiwitten",
    subcategory: "Vis & Zeevruchten",
    level: "gemiddeld",
    heatLevel: "middel",
    body: [
      "Zeevruchten zijn de meest precieze categorie qua garing...",
    ],
    tips: [
      "Gooi mosselen weg die voor het koken al open zijn en niet sluiten bij aantippen.",
    ],
    relatedTechniqueIds: ["vis-stomen", "sauteren"],
    cookware: ["Grote pan met deksel", "Tang", "Schuimspaan"],
  },

  // ─── EIWITTEN › Eieren ────────────────────────────────────────────────────
  {
    id: "eieren-pocheren",
    nl: "Eieren pocheren",
    subtitle: "Perfect zacht gepocheerd ei",
    category: "eiwitten",
    subcategory: "Eieren",
    parentId: "sudderen",
    level: "gemiddeld",
    heatLevel: "laag",
    body: [
      "Een gepocheerd ei heeft een gestold eiwit en een vloeibare, romige dooier...",
    ],
    tips: [
      "Gebruik zo vers mogelijke eieren — de kwaliteit van het ei bepaalt het resultaat.",
    ],
    relatedTechniqueIds: ["eieren-emulsies"],
    cookware: ["Diepe pan", "Schuimspaan", "Klein kommetje"],
  },
  {
    id: "eieren-roerei",
    nl: "Roerei",
    subtitle: "Luchtig versus romig roerei",
    category: "eiwitten",
    subcategory: "Eieren",
    level: "basis",
    heatLevel: "laag",
    body: [
      "Er zijn twee stijlen roerei: het Engelse luchtige roerei en het Franse romige roerei...",
    ],
    tips: [
      "Voeg zout toe vóór het kloppen en laat de eieren minimaal vijftien minuten rusten.",
    ],
    relatedTechniqueIds: ["eieren-omelet"],
    cookware: ["Kleine koekenpan of steelpan", "Spatel of houten lepel"],
  },
  {
    id: "eieren-omelet",
    nl: "Omelet",
    subtitle: "Franse omelettechniek",
    category: "eiwitten",
    subcategory: "Eieren",
    level: "gemiddeld",
    heatLevel: "middel",
    body: [
      "De Franse omelet is een klassieke techniek die behendigheid vraagt...",
    ],
    tips: [
      "Gebruik een kleine pan: 3 eieren in een 18 cm pan geeft de perfecte dikte.",
    ],
    relatedTechniqueIds: ["eieren-roerei"],
    cookware: ["Kleine koekenpan (18–20 cm, anti-aanbak)", "Vork", "Houten spatel"],
  },
  {
    id: "eieren-emulsies",
    nl: "Eieremulsies",
    subtitle: "Hollandaise en mayonaise",
    category: "eiwitten",
    subcategory: "Eieren",
    parentId: "emulgeren",
    level: "gevorderd",
    heatLevel: "laag",
    body: [
      "Emulsies op basis van eigeel — hollandaise en mayonaise — zijn twee van de meest fundamentele sauzen...",
    ],
    tips: [
      "Voeg het vet altijd in een dun straaltje toe, nooit in één keer.",
    ],
    relatedTechniqueIds: ["emulgeren", "hollandaise", "mayonaise"],
    cookware: ["Garde", "Bain-marie", "Staafmixer"],
  },

  // ─── GROENTEN › Bereidingsmethoden ───────────────────────────────────────
  {
    id: "blancheren",
    nl: "Blancheren",
    subtitle: "Kort koken, dan afkoelen in ijswater",
    category: "groenten",
    subcategory: "Bereidingsmethoden",
    level: "basis",
    heatLevel: "hoog",
    body: [
      "Blancheren is het kort onderdompelen van groenten in kokend gezouten water, gevolgd door direct afkoelen in ijswater...",
    ],
    tips: [
      "Gebruik royaal gezouten kookwater — pasta-zout-niveau — voor de beste smaak en kleur.",
    ],
    relatedVeg: ["Green beans", "Spinach", "Broad bean", "Broccoli", "Cauliflower"],
    relatedTechniqueIds: ["groenten-roerbakken", "groenten-rauw"],
    cookware: ["Grote kookpan", "Zeef of spatel", "Kom met ijswater"],
  },
  {
    id: "groenten-roasten",
    nl: "Groenten roosteren",
    subtitle: "Karamelisering in de oven",
    category: "groenten",
    subcategory: "Bereidingsmethoden",
    level: "basis",
    heatLevel: "hoog",
    body: [
      "Roosteren in de oven transformeert groenten door karamelisatie en de Maillard-reactie...",
    ],
    tips: [
      "Droog groenten goed voor het roosteren: vochtige groenten stomen in de oven.",
    ],
    relatedVeg: ["Cauliflower", "Beetroot", "Carrot", "Kohlrabi", "Aubergine", "Pumpkin / squash"],
    relatedTechniqueIds: ["sauteren", "groenten-smoren"],
    cookware: ["Bakplaat (goed groot)", "Kom voor mengen met olie"],
  },
  {
    id: "groenten-smoren",
    nl: "Groenten smoren",
    subtitle: "Zacht en diep van smaak",
    category: "groenten",
    subcategory: "Bereidingsmethoden",
    parentId: "smoren",
    level: "gemiddeld",
    heatLevel: "middel",
    body: [
      "Groenten smoren is ze langzaam laten garen in een kleine hoeveelheid vloeistof in een afgedekte pan...",
    ],
    tips: [
      "Voeg een klontje koude boter toe aan het ende voor een glanzende, rijke saus.",
    ],
    relatedVeg: ["Chicory", "Endive", "Fennel", "Celery", "Leek"],
    relatedTechniqueIds: ["smoren", "groenten-roasten"],
    cookware: ["Diepe pan met deksel", "Houten lepel"],
  },
  {
    id: "groenten-roerbakken",
    nl: "Groenten roerbakken",
    subtitle: "Snel op hoog vuur",
    category: "groenten",
    subcategory: "Bereidingsmethoden",
    parentId: "sauteren",
    level: "basis",
    heatLevel: "hoog",
    body: [
      "Roerbakken is de snelste methode voor groenten: hoog vuur, weinig vet, constante beweging...",
    ],
    tips: [
      "Verwarm de wok of pan tot het rookpunt voor je olie toevoegt.",
    ],
    relatedVeg: ["Bell pepper", "Courgette", "Green beans", "Sugar snap / mangetout", "Chard", "Spinach"],
    relatedTechniqueIds: ["sauteren", "blancheren"],
    cookware: ["Wok of grote koekenpan", "Wokspatel of tang"],
  },
  {
    id: "groenten-rauw",
    nl: "Rauw bereiden",
    subtitle: "Snijden, marineren en fermenteren",
    category: "groenten",
    subcategory: "Bereidingsmethoden",
    level: "basis",
    heatLevel: "geen",
    body: [
      "Rauwe bereidingen zijn in opkomst: van carpaccio van rode biet tot gefermenteerde koolraap...",
    ],
    tips: [
      "Gebruik mandoline voor flinterdunne schijfjes die snel marineren.",
    ],
    relatedVeg: ["Beetroot", "Kohlrabi", "Radish", "Cucumber", "Carrot"],
    relatedTechniqueIds: ["marineren", "julienne", "chiffonade"],
    cookware: ["Mandoline", "Kom", "Inmaakpot voor fermentatie"],
  },

  // ─── SAUZEN › Klassieke sauzen ───────────────────────────────────────────
  {
    id: "roux",
    nl: "Roux",
    subtitle: "Basis bindmiddel van bloem en vet",
    category: "sauzen",
    subcategory: "Klassieke sauzen",
    level: "basis",
    heatLevel: "laag",
    body: [
      "Een roux is het klassieke bindmiddel voor soepen en sauzen, bestaande uit gelijke delen vet (meestal boter) en bloem. Door de bloem in het vet te garen, zwellen de zetmeelkorrels op zonder te klonteren, waardoor ze vloeistof effectief kunnen binden wanneer deze wordt toegevoegd.",
      "Afhankelijk van de kooktijd verandert een roux van kleur en smaak: van een blanke roux (korte garing, maximale binding) naar een blonde roux tot een bruine roux (lange garing, nootachtige smaak, minder bindkracht)."
    ],
    tips: [
      "Weeg bloem en boter nauwkeurig af — een verhouding van 1:1 is essentieel.",
      "Laat de roux minstens twee minuten garen om de rauwe bloemsmaak volledig te neutraliseren.",
      "Voeg koude vloeistof toe aan een hete roux, of hete vloeistof aan een afgekoelde roux om klontjes te vermijden."
    ],
    relatedTechniqueIds: ["bechamel", "veloute"],
    cookware: ["Steelpan", "Garde"]
  },
  {
    id: "bechamel",
    nl: "Béchamel",
    subtitle: "Basis witte saus",
    category: "sauzen",
    subcategory: "Klassieke sauzen",
    parentId: "roux",
    level: "basis",
    heatLevel: "laag",
    body: [
      "Béchamel is één van de vijf moedersauzen van de klassieke Franse keuken en de basis voor talloze gerechten...",
    ],
    tips: [
      "Roer altijd met een garde voor een klontvrije saus — een houten lepel laat soms klontjes staan.",
    ],
    relatedVeg: ["Cauliflower", "Fennel", "Endive"],
    relatedTechniqueIds: ["gratineren", "emulgeren"],
    cookware: ["Steelpan", "Garde", "Houten lepel"],
  },
  {
    id: "veloute",
    nl: "Velouté",
    subtitle: "Fond-gebonden saus",
    category: "sauzen",
    subcategory: "Klassieke sauzen",
    parentId: "roux",
    level: "gemiddeld",
    heatLevel: "middel",
    body: [
      "Velouté is een moedersaus van witte fond (kip, vis of groente) gebonden met een lichte roux...",
    ],
    tips: [
      "Gebruik alleen heldere, goed gemaakte fond voor een goede velouté.",
    ],
    relatedTechniqueIds: ["kippenfond", "groentebouillon", "bechamel"],
    cookware: ["Steelpan", "Garde", "Fijne zeef"],
  },
  {
    id: "tomatensaus",
    nl: "Tomatensaus",
    subtitle: "Langzaam ingekookt",
    category: "sauzen",
    subcategory: "Klassieke sauzen",
    level: "basis",
    heatLevel: "laag",
    body: [
      "Een goede tomatensaus vraagt geduld: de tomaten moeten lang en zacht koken om hun zuurheid te verliezen...",
    ],
    tips: [
      "Roer de saus regelmatig och houd het vuur laag.",
    ],
    relatedVeg: ["Tomato", "Celery", "Carrot"],
    relatedTechniqueIds: ["sudderen", "sauteren"],
    cookware: ["Brede pan of braadpan", "Houten lepel"],
  },
  {
    id: "hollandaise",
    nl: "Hollandaise",
    subtitle: "Warme emulsiesaus",
    category: "sauzen",
    subcategory: "Klassieke sauzen",
    parentId: "emulgeren",
    level: "gevorderd",
    heatLevel: "laag",
    body: [
      "Hollandaise is een warme emulsiesaus van eidooiers en geklaarde boter, met een citroensap...",
    ],
    tips: [
      "De bain-marie mag nooit het kookpunt bereiken: de dooiers mogen niet stollen.",
    ],
    relatedVeg: ["White asparagus", "Green asparagus"],
    relatedTechniqueIds: ["emulgeren", "eieren-emulsies"],
    cookware: ["Garde", "Bain-marie (kom op pan)", "Steelpan"],
  },

  // ─── SAUZEN › Emulsies ───────────────────────────────────────────────────
  {
    id: "vinaigrette",
    nl: "Vinaigrette",
    subtitle: "Stabiele dressing",
    category: "sauzen",
    subcategory: "Emulsies",
    parentId: "emulgeren",
    level: "basis",
    heatLevel: "geen",
    body: [
      "Een vinaigrette is de eenvoudigste emulsie in de keuken: azijn en olie verbonden met een emulgator...",
    ],
    tips: [
      "Gebruik kwaliteitsazijn: witte wijnazijn, sherryazijn of champagneazijn geven de beste resultaten.",
    ],
    relatedVeg: ["Butterhead lettuce", "Rocket", "Lamb's lettuce", "Beetroot"],
    relatedTechniqueIds: ["emulgeren", "marineren"],
    cookware: ["Kom", "Garde", "Potje met deksel voor bewaren"],
  },
  {
    id: "mayonaise",
    nl: "Mayonaise",
    subtitle: "Koude emulsie",
    category: "sauzen",
    subcategory: "Emulsies",
    parentId: "emulgeren",
    level: "gemiddeld",
    heatLevel: "geen",
    body: [
      "Mayonaise is een koude emulsie van eidooier, mosterd, olie en zuur...",
    ],
    tips: [
      "Als de mayonaise breekt, voeg een druppel water toe en klop dan snel.",
    ],
    relatedTechniqueIds: ["emulgeren", "eieren-emulsies", "vinaigrette"],
    cookware: ["Garde of staafmixer", "Kom of maatbeker"],
  },
  {
    id: "beurre-blanc",
    nl: "Beurre blanc",
    subtitle: "Botersaus op wijnbasis",
    category: "sauzen",
    subcategory: "Emulsies",
    parentId: "emulgeren",
    level: "gevorderd",
    heatLevel: "laag",
    body: [
      "Beurre blanc is een klassieke Franse botersaus gemaakt van een reductie van witte wijn, wijnazijn en sjalotjes...",
    ],
    tips: [
      "Gebruik koude boter in kleine stukjes — dit is essentieel voor de emulsie.",
    ],
    relatedVeg: ["White asparagus", "Green asparagus", "Leek"],
    relatedTechniqueIds: ["emulgeren", "fond-deglaceren"],
    cookware: ["Kleine steelpan", "Garde", "Zeef"],
  },
  {
    id: "monteren",
    nl: "Monteren",
    subtitle: "Saus binden met koude boter",
    category: "sauzen",
    subcategory: "Emulsies",
    parentId: "emulgeren",
    level: "gemiddeld",
    heatLevel: "laag",
    body: [
      "Monteren (of monter au beurre) is het glanzend en licht binden van een warme saus door er op het allerlaatste moment ijskoude klontjes boter door te kloppen of te zwenken. De saus mag hierna absoluut niet meer koken, anders smelt het vet volledig en breekt de binding.",
      "De melkvreemden en eiwitten in de koude boter werken als een natuurlijke emulgator die de vetdruppeltjes in de saus verdeelt. Dit geeft de saus een zijdezachte textuur, een rijke smaak en een prachtige glans."
    ],
    tips: [
      "Zorg dat de boter echt ijskoud is en in kleine blokjes is gesneden.",
      "Haal de pan volledig van het vuur voordat je begint met monteren.",
      "Serveer de saus onmiddellijk na het monteren om kwaliteitsverlies te voorkomen."
    ],
    relatedTechniqueIds: ["beurre-blanc", "emulgeren"],
    cookware: ["Steelpan", "Garde of panhandvat"]
  },

  // ─── SAUZEN › Fonds & Bouillons ──────────────────────────────────────────
  {
    id: "kippenfond",
    nl: "Kippenfond",
    subtitle: "Helder kippenbouillon",
    category: "sauzen",
    subcategory: "Fonds & Bouillons",
    parentId: "sudderen",
    level: "basis",
    heatLevel: "laag",
    body: [
      "Kippenfond is de basis van de klassieke keuken: soepen, sauzen, risotto en smoorgerechten...",
    ],
    tips: [
      "Gebruik nooit deksel op de fond: de stoom geeft troebele resultaten.",
    ],
    relatedVeg: ["Carrot", "Celery", "Leek", "Yellow onion"],
    relatedTechniqueIds: ["sudderen", "veloute", "smoren"],
    cookware: ["Grote soeppan (6+ L)", "Schuimspaan", "Fijne zeef", "Kaasdoek"],
  },
  {
    id: "groentebouillon",
    nl: "Groentebouillon",
    subtitle: "Lichte groentebasis",
    category: "sauzen",
    subcategory: "Fonds & Bouillons",
    parentId: "sudderen",
    level: "basis",
    heatLevel: "laag",
    body: [
      "Groentebouillon is lichter en sneller klaar dan vleesfond: vijfenveertig minuten sudderen geeft al een aromatische basis...",
    ],
    tips: [
      "Voeg een stuk kombu-zeewier toe voor umami-diepte.",
    ],
    relatedVeg: ["Carrot", "Celery", "Leek", "Fennel", "Yellow onion"],
    relatedTechniqueIds: ["sudderen", "veloute"],
    cookware: ["Soeppan (4–6 L)", "Schuimspaan", "Fijne zeef"],
  },
  {
    id: "bruine-fond",
    nl: "Bruine fond",
    subtitle: "Geroosterde botten",
    category: "sauzen",
    subcategory: "Fonds & Bouillons",
    level: "gevorderd",
    heatLevel: "hoog",
    body: [
      "Bruine fond is de rijkste, meest complexe basis in de klassieke keuken. De botten worden eerst geroosterd...",
    ],
    tips: [
      "Rooster de botten tot ze echt diep bruin zijn — dit geeft de fond zijn kleur.",
    ],
    relatedVeg: ["Carrot", "Celery", "Yellow onion"],
    relatedTechniqueIds: ["smoren", "fond-deglaceren"],
    cookware: ["Braadslede voor de oven", "Grote soeppan (8+ L)", "Schuimspaan", "Fijne zeef"],
  },
];