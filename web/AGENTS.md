<!-- BEGIN:nextjs-agent-rules -->
# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices.
<!-- END:nextjs-agent-rules -->

<!-- BEGIN:language-rules -->
# Language: Dutch only

All user-visible content in this web app must be in Dutch. This is a hard requirement.

- Every label, heading, placeholder, aria-label, tooltip, button text, status message, and data field rendered to the user must be in Dutch.
- Vegetable data in `web/lib/vegetables.ts` — including `blurb`, `storage`, `prep`, `pairings`, `substitutions`, `technique`, `family`, and recipe `why` fields — must be written in Dutch.
- Month names (`MONTHS`, `MONTHS_LONG`) must use Dutch abbreviations and full names.
- The `name` field on `VegItem` is the internal English identifier used for search matching and sorting only — it is never rendered to users. Display only the `nl` field.
- Code, variable names, comments, filenames, and anything not visible to website visitors may remain in English.
<!-- END:language-rules -->
