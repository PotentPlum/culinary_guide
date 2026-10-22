package nl.potentplum.mise.data.settings

import kotlinx.serialization.Serializable
import java.time.LocalDate

// Day and session state (spec §7.3). Stored as JSON in the settings file, never backed up, and only valid for a day
// (or 24 hours for a cook session); the expiry rules live here so they are testable without DataStore.

/** The Vanavond pin (spec §6.2). [date] is an ISO date in Europe/Amsterdam. */
@Serializable
data class TonightPin(
    val date: String,
    val recipeId: String,
) {
    fun isValidOn(today: LocalDate): Boolean = date == today.toString()
}

/** Suggestions dismissed with "Niet vandaag". */
@Serializable
data class TodayDismissed(
    val date: String,
    val recipeIds: List<String>,
) {
    fun isValidOn(today: LocalDate): Boolean = date == today.toString()
}

@Serializable
data class CookTimer(
    val stepIndex: Int,
    val lengthSeconds: Int,
    val startedAt: Long,
)

/** The one open cook session (spec §6.10). [stepIndex] is 0-based. */
@Serializable
data class CookSession(
    val recipeId: String,
    val stepIndex: Int,
    val servings: Int? = null,
    val checkedIngredientIds: List<String> = emptyList(),
    val timers: List<CookTimer> = emptyList(),
    val updatedAt: Long,
) {
    fun isExpired(nowMillis: Long): Boolean = nowMillis - updatedAt > MAX_AGE_MILLIS

    companion object {
        const val MAX_AGE_MILLIS: Long = 24 * 60 * 60 * 1000L
    }
}
