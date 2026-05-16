# Culinary Guide — Project Instructions

## Stack
- Next.js (App Router) + TypeScript + Tailwind, deployed to Vercel
- No backend yet — all data is static in `web/lib/vegetables.ts`
- All work lives in `web/`

## Data scope
Locally grown vegetables in the Netherlands only. Seasonality = Dutch field/greenhouse harvest windows (not supermarket imports). Source: velt.nu.
Do NOT add: imported year-round produce (avocado, citrus, mango), fruits, berries, or nuts.

## Language
All user-visible text must be in **Dutch**. Code, variable names, comments, and filenames stay in English.
- Source of truth for all Dutch UI strings: `web/lib/strings.ts` — import from there, do not hardcode
- `VegItem.name` = internal English identifier, **never rendered**; display `VegItem.nl` instead
- See `web/AGENTS.md` for the full language ruleset

## Design system
"Mise — een kooksalmanak". Editorial cookbook aesthetic:
- Fonts: Instrument Serif italic + Instrument Sans body
- Colors: cream `#F4EFE6`, leaf-green `#3F5E2A`, terracotta accent
- Layout via plain CSS classes in `web/app/globals.css` — no Tailwind utilities for layout
- Tailwind is installed but intentionally unused for layout; do not introduce Tailwind utility classes

## Key data files
- `web/lib/vegetables.ts` — `VegItem` type, `VEG_DATA[]`, `statusFor(veg, month)` helper, `MONTHS`/`MONTHS_LONG`
- `web/lib/strings.ts` — all Dutch UI strings; update here when adding new labels
- `web/data/vegetables.json` — original Dutch source data (reference only, not imported)

## Validation
- `npm run validate` — checks all 66 VegItem entries for hex colors, valid months (1–12), required fields, allowed family values
- `npm run build` runs validate first; a failing validate blocks the build
- Allowed `family` values: `"Wortel"`, `"Blad"`, `"Brassica"`, `"Allium"`, `"Peulvrucht"`, `"Stengel"`, `"Pompoen"`, `"Knolgewas"`, `"Vruchtgroente"`, `"Paddenstoel"`, `"Kruid"`

## Component contracts

| Component | File | Props summary | Responsibility |
|-----------|------|---------------|----------------|
| `App` | `app/page.tsx` | — (root) | App shell, all state (page, filter, query, view, density, openVeg), month computed from `new Date()` |
| `VegCard` | `components/VegCard.tsx` | `veg, month, density?, onOpen, listMode?` | Grid card or list row; calls `onOpen(veg)` on click |
| `DetailPanel` | `components/DetailPanel.tsx` | `veg\|null, month, onClose` | Slide-in drawer; ESC closes; blocks body scroll while open |
| `FilterPills` | `components/FilterPills.tsx` | `value, onChange, counts` | Tab-style season filter; exports `Filter` and `Counts` types |
| `CalendarPage` | `components/CalendarPage.tsx` | `month, onOpen` | Full-year 12-month calendar heatmap |
| `PeakBar` | `components/PeakBar.tsx` | `veg, month, compact?` | 12-month peak/shoulder availability strip with tooltips |
| `StatusDot` | `components/StatusDot.tsx` | `status, withLabel?` | Colored dot + optional Dutch status label |
| `VegToken` | `components/VegToken.tsx` | `veg, size` | Geometric SVG circle illustration placeholder |
| `NavIcon` | `components/NavIcon.tsx` | `name` | Monoline SVG icon switcher (9 icons) |
| `FuturePeek` | `components/FuturePeek.tsx` | — | Teaser cards for upcoming modules (static, strings from `lib/strings.ts`) |

## Known intentional gaps
- `density` state exists in `App` but has no toggle UI yet — it's initialized to `"comfortable"` and passed through. Adding the toggle button is a planned future task, not a bug.
- `VegItem.shoulder` is optional; not all vegetables have shoulder months.
- Month detection uses local client timezone (`new Date().getMonth()`). This is intentional — the app is NL-only.

## Future modules (not yet built)
Recepten, Vervangers, Technieken, Plukken — ghost nav items in the sidebar, defined in `NAV_ITEMS` in `page.tsx`.
