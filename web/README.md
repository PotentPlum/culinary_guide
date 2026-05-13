# Mise — a cook's almanac

Seasonal vegetable guide for the Netherlands. Shows what's at peak freshness from local Dutch growers, month by month.

## Scope

This app covers **locally grown vegetables in the Netherlands only**.

- Seasonality reflects Dutch field and greenhouse harvest windows — not supermarket import availability.
- "Peak" means a vegetable is at its flavour best from nearby soil, not simply on a shelf.
- Fruits, berries, and nuts are out of scope.
- Imported produce that has no meaningful NL harvest window (avocado, mango, citrus, etc.) is out of scope.

When expanding the dataset, only add vegetables that are genuinely grown and harvested in the Netherlands with a meaningful seasonal window. Source: [velt.nu](https://www.velt.nu) is the reference used for Dutch harvest calendars.

## Data

- `web/lib/vegetables.ts` — typed vegetable data (51 entries). This is the single source of truth. Read the JSDoc at the top of that file before adding entries.
- `web/data/vegetables.json` — original Dutch source data from velt.nu (read-only reference).

## Stack

Next.js (App Router) · TypeScript · Tailwind · Vercel

## Development

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).
