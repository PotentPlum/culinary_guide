import type { Discipline } from './profileTypes';

export const DISCIPLINE_MAP: Record<string, Discipline> = {
  // ── snijden → mechanical ──────────────────────────────────────────────────
  julienne:              'mechanical',
  brunoise:              'mechanical',
  batonnet:              'mechanical',
  snipperen:             'mechanical',
  chiffonade:            'mechanical',
  tournee:               'mechanical',
  rondelle:              'mechanical',
  hakken:                'mechanical',
  paysanne:              'mechanical',

  // ── eiwitten: cutting/prep → mechanical ──────────────────────────────────
  'vlees-snijden':       'mechanical',
  'gevogelte-ontleden':  'mechanical',

  // ── garing → thermal ─────────────────────────────────────────────────────
  sauteren:              'thermal',
  schroeien:             'thermal',
  gratineren:            'thermal',
  smoren:                'thermal',
  sudderen:              'thermal',
  koken:                 'thermal',
  panroosteren:          'thermal',
  'langzaam-garen':      'thermal',
  confit:                'thermal',
  'sous-vide':           'thermal',
  stomen:                'thermal',
  grillen:               'thermal',
  frituren:              'thermal',
  'en-papillote':        'thermal',

  // ── eiwitten (heat techniques) → thermal ─────────────────────────────────
  'vlees-schroeien':     'thermal',
  'vlees-smoren':        'thermal',
  'vlees-braden':        'thermal',
  'gevogelte-braden':    'thermal',
  'gevogelte-pocheren':  'thermal',
  'vis-stomen':          'thermal',
  'vis-pansearen':       'thermal',
  'vis-pocheren':        'thermal',
  'zeevruchten-bereiden':'thermal',
  'eieren-pocheren':     'thermal',
  'eieren-roerei':       'thermal',
  'eieren-omelet':       'thermal',
  'eieren-emulsies':     'thermal',
  arroseren:             'thermal',

  // ── groenten (heat-based) → thermal ──────────────────────────────────────
  blancheren:            'thermal',
  'groenten-roasten':    'thermal',
  'groenten-roerbakken': 'thermal',
  'groenten-smoren':     'thermal',

  // ── smaak → flavor ───────────────────────────────────────────────────────
  'zout-timing':         'flavor',
  pekelen:               'flavor',
  'fond-deglaceren':     'flavor',
  emulgeren:             'flavor',
  marineren:             'flavor',
  soffritto:             'flavor',
  zuurbalans:            'flavor',
  'kruiden-gebruik':     'flavor',

  // ── groenten (raw/flavor) → flavor ───────────────────────────────────────
  'groenten-rauw':       'flavor',

  // ── sauzen → flavor ──────────────────────────────────────────────────────
  roux:                  'flavor',
  bechamel:              'flavor',
  veloute:               'flavor',
  tomatensaus:           'flavor',
  hollandaise:           'flavor',
  vinaigrette:           'flavor',
  mayonaise:             'flavor',
  'beurre-blanc':        'flavor',
  monteren:              'flavor',
  kippenfond:            'flavor',
  groentebouillon:       'flavor',
  'bruine-fond':         'flavor',
  reductie:              'flavor',
  jus:                   'flavor',
  espagnole:             'flavor',
  gastrique:             'flavor',

  // ── bewaren → flavor (preservation relates to flavor) ────────────────────
  fermenteren:           'flavor',
  inmaken:               'flavor',

  // ── gereedschap → material ───────────────────────────────────────────────
  koekenpan:             'material',
  braadpan:              'material',
  steelpan:              'material',
  messenonderhoud:       'material',
};

export function getDiscipline(techniqueId: string): Discipline | null {
  return DISCIPLINE_MAP[techniqueId] ?? null;
}

export function getTechniquesByDiscipline(discipline: Discipline): string[] {
  return Object.entries(DISCIPLINE_MAP)
    .filter(([, d]) => d === discipline)
    .map(([id]) => id);
}
