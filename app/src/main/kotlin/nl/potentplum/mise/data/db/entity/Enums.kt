package nl.potentplum.mise.data.db.entity

/** Stored by name; renaming a constant needs a migration and a backup format change. */
enum class RecipeSourceType { URL, PHOTO, PAGE, MANUAL, QUEST, LEGACY }

enum class EnrichState { NONE, PENDING, DONE, FAILED }

/** Who structured an ingredient line. Claude enrichment replaces NONE and LOCAL, never MANUAL (spec §11.2). */
enum class ParseSource { NONE, LOCAL, CLAUDE, MANUAL }

/** The four keys of the single tag system (spec §8.4). */
enum class TagKey { CUISINE, DISH_TYPE, MAIN_INGREDIENT, TECHNIQUE }

enum class QuestStatus { OPEN, COMPLETED }

enum class IngredientMode { CUSTOM, SEASONAL, SURPRISE }

/** Soort quest (spec §6.11); [CLAUDE_CHOICE] is "Laat Claude kiezen". */
enum class QuestType { MAALTIJD, LUNCH, BIJGERECHT, SAUS, BOUILLON, COMPONENT, CLAUDE_CHOICE }

/** Beschikbare tijd: < 15 / < 30 / < 60 / > 60 minutes. */
enum class TimeLimit { UNDER_15, UNDER_30, UNDER_60, OVER_60 }

enum class AttemptSourceType { RECIPE, QUEST }

enum class UsageFeature { QUEST, PHOTO_IMPORT, PAGE_IMPORT, ENRICH, KEY_CHECK }
