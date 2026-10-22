package nl.potentplum.mise.data.settings

/** Kwaliteit (spec §0 A8): the model for quests and photo/page import. Enrichment always uses Haiku. */
enum class Quality { FAST, BALANCED, BEST }

enum class AutoBackupFrequency { OFF, DAILY, WEEKLY }

enum class LibrarySort { RECENTLY_ADDED, ALPHABETICAL, RECENTLY_COOKED }

/** The persistent settings of spec §7.3. The API key is never here (spec §9). */
data class Settings(
    val quality: Quality = Quality.BALANCED,
    val seasonalNotificationsEnabled: Boolean = false,
    val autoBackupTreeUri: String? = null,
    val autoBackupFrequency: AutoBackupFrequency = AutoBackupFrequency.OFF,
    val lastBackupAt: Long? = null,
    /** "2026-10": the month the seasonal notification was last posted for. */
    val lastSeasonalNotifiedMonth: String? = null,
    val onboardingDone: Boolean = false,
    val librarySort: LibrarySort = LibrarySort.RECENTLY_ADDED,
    val clockTimerHintShown: Boolean = false,
    val showQuestCard: Boolean = true,
)
