package nl.potentplum.mise.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import nl.potentplum.mise.core.util.MiseClock
import javax.inject.Inject
import javax.inject.Singleton

/** `settings.preferences_pb` (spec §7.3): settings plus the day and session state. */
@Singleton
class SettingsRepository
    @Inject
    constructor(
        @param:SettingsStore private val dataStore: DataStore<Preferences>,
        private val clock: MiseClock,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        val settings: Flow<Settings> = dataStore.data.map(::toSettings).distinctUntilChanged()

        suspend fun current(): Settings = settings.first()

        suspend fun setQuality(quality: Quality) = edit { it[Keys.QUALITY] = quality.name }

        suspend fun setSeasonalNotificationsEnabled(enabled: Boolean) = edit { it[Keys.SEASONAL_NOTIFICATIONS] = enabled }

        suspend fun setAutoBackup(
            treeUri: String?,
            frequency: AutoBackupFrequency,
        ) = edit {
            if (treeUri == null) it.remove(Keys.AUTO_BACKUP_TREE_URI) else it[Keys.AUTO_BACKUP_TREE_URI] = treeUri
            it[Keys.AUTO_BACKUP_FREQUENCY] = frequency.name
        }

        suspend fun setLastBackupAt(at: Long) = edit { it[Keys.LAST_BACKUP_AT] = at }

        suspend fun setLastSeasonalNotifiedMonth(month: String) = edit { it[Keys.LAST_SEASONAL_NOTIFIED_MONTH] = month }

        suspend fun setOnboardingDone(done: Boolean) = edit { it[Keys.ONBOARDING_DONE] = done }

        suspend fun setLibrarySort(sort: LibrarySort) = edit { it[Keys.LIBRARY_SORT] = sort.name }

        suspend fun setClockTimerHintShown() = edit { it[Keys.CLOCK_TIMER_HINT_SHOWN] = true }

        suspend fun setShowQuestCard(show: Boolean) = edit { it[Keys.SHOW_QUEST_CARD] = show }

        // ── Day and session state ──────────────────────────────────────────────

        /** Today's Vanavond pin; a pin from an earlier day reads as none. */
        val tonight: Flow<TonightPin?> =
            dataStore.data.map { prefs -> decode(prefs, Keys.TONIGHT, TonightPin.serializer())?.takeIf { it.isValidOn(clock.today()) } }

        suspend fun pinTonight(recipeId: String) =
            edit { it[Keys.TONIGHT] = json.encodeToString(TonightPin.serializer(), TonightPin(clock.today().toString(), recipeId)) }

        /** Removes the pin; with [recipeId], only when that recipe is the one pinned. */
        suspend fun unpinTonight(recipeId: String? = null) =
            edit { prefs ->
                val pin = decode(prefs, Keys.TONIGHT, TonightPin.serializer())
                if (recipeId == null || pin == null || pin.recipeId == recipeId) prefs.remove(Keys.TONIGHT)
            }

        /** Recipe ids dismissed with "Niet vandaag" today. */
        val todayDismissed: Flow<Set<String>> =
            dataStore.data.map { prefs ->
                decode(prefs, Keys.TODAY_DISMISSED, TodayDismissed.serializer())
                    ?.takeIf { it.isValidOn(clock.today()) }
                    ?.recipeIds
                    ?.toSet()
                    .orEmpty()
            }

        suspend fun dismissToday(recipeId: String) =
            edit { prefs ->
                val today = clock.today()
                val current = decode(prefs, Keys.TODAY_DISMISSED, TodayDismissed.serializer())?.takeIf { it.isValidOn(today) }
                val ids = (current?.recipeIds.orEmpty() + recipeId).distinct()
                prefs[Keys.TODAY_DISMISSED] = json.encodeToString(TodayDismissed.serializer(), TodayDismissed(today.toString(), ids))
            }

        /** The open cook session, or null when there is none or it is more than 24 hours old. */
        val cookSession: Flow<CookSession?> =
            dataStore.data.map { prefs ->
                decode(prefs, Keys.COOK_SESSION, CookSession.serializer())?.takeUnless { it.isExpired(clock.nowMillis()) }
            }

        /** Saves the session with `updatedAt = now`; called on every change (spec §6.10). */
        suspend fun saveCookSession(session: CookSession) =
            edit {
                it[Keys.COOK_SESSION] = json.encodeToString(CookSession.serializer(), session.copy(updatedAt = clock.nowMillis()))
            }

        /** Clears the session, or only when it belongs to [recipeId] (an attempt was logged for that recipe). */
        suspend fun clearCookSession(recipeId: String? = null) =
            edit { prefs ->
                val session = decode(prefs, Keys.COOK_SESSION, CookSession.serializer())
                if (recipeId == null || session == null || session.recipeId == recipeId) prefs.remove(Keys.COOK_SESSION)
            }

        /** Drops expired day and session state from the file. Reads already ignore it; this only keeps the file tidy. */
        suspend fun pruneExpired() =
            edit { prefs ->
                val today = clock.today()
                if (decode(prefs, Keys.TONIGHT, TonightPin.serializer())?.isValidOn(today) == false) prefs.remove(Keys.TONIGHT)
                if (decode(prefs, Keys.TODAY_DISMISSED, TodayDismissed.serializer())?.isValidOn(today) == false) {
                    prefs.remove(Keys.TODAY_DISMISSED)
                }
                if (decode(prefs, Keys.COOK_SESSION, CookSession.serializer())?.isExpired(clock.nowMillis()) == true) {
                    prefs.remove(Keys.COOK_SESSION)
                }
            }

        private suspend fun edit(block: (MutablePreferences) -> Unit) {
            dataStore.edit(block)
        }

        private fun <T> decode(
            prefs: Preferences,
            key: Preferences.Key<String>,
            serializer: KSerializer<T>,
        ): T? = prefs[key]?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }

        private fun toSettings(prefs: Preferences): Settings {
            val defaults = Settings()
            return Settings(
                quality = enumOrDefault(prefs[Keys.QUALITY], defaults.quality),
                seasonalNotificationsEnabled = prefs[Keys.SEASONAL_NOTIFICATIONS] ?: defaults.seasonalNotificationsEnabled,
                autoBackupTreeUri = prefs[Keys.AUTO_BACKUP_TREE_URI],
                autoBackupFrequency = enumOrDefault(prefs[Keys.AUTO_BACKUP_FREQUENCY], defaults.autoBackupFrequency),
                lastBackupAt = prefs[Keys.LAST_BACKUP_AT],
                lastSeasonalNotifiedMonth = prefs[Keys.LAST_SEASONAL_NOTIFIED_MONTH],
                onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: defaults.onboardingDone,
                librarySort = enumOrDefault(prefs[Keys.LIBRARY_SORT], defaults.librarySort),
                clockTimerHintShown = prefs[Keys.CLOCK_TIMER_HINT_SHOWN] ?: defaults.clockTimerHintShown,
                showQuestCard = prefs[Keys.SHOW_QUEST_CARD] ?: defaults.showQuestCard,
            )
        }

        private inline fun <reified E : Enum<E>> enumOrDefault(
            name: String?,
            default: E,
        ): E = enumValues<E>().firstOrNull { it.name == name } ?: default

        private object Keys {
            val QUALITY = stringPreferencesKey("quality")
            val SEASONAL_NOTIFICATIONS = booleanPreferencesKey("seasonalNotificationsEnabled")
            val AUTO_BACKUP_TREE_URI = stringPreferencesKey("autoBackupTreeUri")
            val AUTO_BACKUP_FREQUENCY = stringPreferencesKey("autoBackupFrequency")
            val LAST_BACKUP_AT = longPreferencesKey("lastBackupAt")
            val LAST_SEASONAL_NOTIFIED_MONTH = stringPreferencesKey("lastSeasonalNotifiedMonth")
            val ONBOARDING_DONE = booleanPreferencesKey("onboardingDone")
            val LIBRARY_SORT = stringPreferencesKey("librarySort")
            val CLOCK_TIMER_HINT_SHOWN = booleanPreferencesKey("clockTimerHintShown")
            val SHOW_QUEST_CARD = booleanPreferencesKey("showQuestCard")
            val TONIGHT = stringPreferencesKey("tonight")
            val TODAY_DISMISSED = stringPreferencesKey("todayDismissed")
            val COOK_SESSION = stringPreferencesKey("cookSession")
        }
    }
