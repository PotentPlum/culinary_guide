# Mise — een kooksalmanak

A personal Dutch cooking almanac for Android, built around one daily question: **"Wat kook ik vandaag?"** It ranks your
own recipes (imported from websites, cookbook photos or typed in) by which Dutch-grown vegetables are at their best right
now, offers a one-tap AI "Quest" (a focused cooking exercise generated with Claude), and keeps a cooking journal that
shapes the next suggestions. A technique library supports every step.

All data stays on the phone. The Claude API key you enter is stored encrypted with the Android Keystore.

**Status:** being rebuilt as a native Kotlin/Jetpack Compose app. The previous Next.js + Supabase web app was retired
(last version: commit `cf3ec39`).

| Document | Contents |
|---|---|
| [`spec.md`](spec.md) | Full specification |
| [`todo.md`](todo.md) | Phased task list |
| [`data/README.md`](data/README.md) | Curated content (66 vegetables, 74 techniques, badges, Dutch UI strings), test fixtures, schemas |

```bash
python3 tools/validate_data.py   # validate all content and fixtures
```

Seasonal data reference: [velt.nu](https://www.velt.nu).
