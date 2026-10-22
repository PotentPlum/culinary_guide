# Mise — een kooksalmanak

A personal Dutch cooking almanac for Android: which Dutch-grown vegetables are in season right now, how to cook them
(technique library), your own recipe library (imported from websites, cookbook photos or typed in), and AI "Quests"
(focused cooking exercises generated with Claude) with a profile that tracks your progress.

All data stays on the phone. The Claude API key you enter is stored encrypted with the Android Keystore.

**Status:** being rebuilt as a native Kotlin/Jetpack Compose app. The previous Next.js + Supabase web app was retired
(last version: commit `cf3ec39`).

| Document | Contents |
|---|---|
| [`spec.md`](spec.md) | Full specification |
| [`todo.md`](todo.md) | Phased task list |
| [`data/README.md`](data/README.md) | Curated content (66 vegetables, 74 techniques, Dutch UI strings), test fixtures, schemas |

```bash
python3 tools/validate_data.py   # validate all content and fixtures
```

Seasonal data reference: [velt.nu](https://www.velt.nu).
