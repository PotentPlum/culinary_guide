package nl.potentplum.mise.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.testing.MutableClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Duration
import java.time.LocalDateTime

class SettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    /** 22 October 2026, 23:30 in Amsterdam: half an hour before midnight. */
    private val clock = MutableClock.at(LocalDateTime.of(2026, 10, 22, 23, 30))

    private fun TestScope.repository(): SettingsRepository {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "settings.preferences_pb") }
        return SettingsRepository(store, MiseClock(clock))
    }

    @Test
    fun `defaults follow the spec`() =
        runTest {
            val settings = repository().current()
            assertEquals(Quality.BALANCED, settings.quality)
            assertTrue(settings.showQuestCard)
            assertFalse(settings.onboardingDone)
            assertFalse(settings.seasonalNotificationsEnabled)
            assertEquals(LibrarySort.RECENTLY_ADDED, settings.librarySort)
            assertEquals(AutoBackupFrequency.OFF, settings.autoBackupFrequency)
        }

    @Test
    fun `settings are stored`() =
        runTest {
            val repo = repository()
            repo.setQuality(Quality.BEST)
            repo.setShowQuestCard(false)
            repo.setOnboardingDone(true)
            repo.setLibrarySort(LibrarySort.ALPHABETICAL)
            repo.setAutoBackup("content://tree/backups", AutoBackupFrequency.WEEKLY)
            repo.setClockTimerHintShown()
            val settings = repo.current()
            assertEquals(Quality.BEST, settings.quality)
            assertFalse(settings.showQuestCard)
            assertTrue(settings.onboardingDone)
            assertEquals(LibrarySort.ALPHABETICAL, settings.librarySort)
            assertEquals("content://tree/backups", settings.autoBackupTreeUri)
            assertEquals(AutoBackupFrequency.WEEKLY, settings.autoBackupFrequency)
            assertTrue(settings.clockTimerHintShown)
        }

    @Test
    fun `the vanavond pin expires at midnight`() =
        runTest {
            val repo = repository()
            repo.pinTonight("r1")
            assertEquals("r1", repo.tonight.first()?.recipeId)

            clock.advance(Duration.ofMinutes(29))
            assertEquals("r1", repo.tonight.first()?.recipeId)

            clock.advance(Duration.ofMinutes(2)) // 00:01 on the 23rd
            assertNull(repo.tonight.first())
        }

    @Test
    fun `unpinning removes the pin`() =
        runTest {
            val repo = repository()
            repo.pinTonight("r1")
            repo.unpinTonight()
            assertNull(repo.tonight.first())
        }

    @Test
    fun `niet vandaag collects ids for one day and starts over after midnight`() =
        runTest {
            val repo = repository()
            repo.dismissToday("r1")
            repo.dismissToday("r2")
            repo.dismissToday("r1")
            assertEquals(setOf("r1", "r2"), repo.todayDismissed.first())

            clock.advance(Duration.ofHours(1)) // 00:30 on the 23rd
            assertEquals(emptySet<String>(), repo.todayDismissed.first())

            repo.dismissToday("r3")
            assertEquals(setOf("r3"), repo.todayDismissed.first())
        }

    @Test
    fun `a cook session survives a day boundary but not 24 hours`() =
        runTest {
            val repo = repository()
            repo.saveCookSession(CookSession(recipeId = "r1", stepIndex = 2, servings = 4, updatedAt = 0))
            val saved = repo.cookSession.first()!!
            assertEquals(2, saved.stepIndex)
            assertEquals(clock.millis(), saved.updatedAt)

            clock.advance(Duration.ofHours(23).plusMinutes(59)) // past midnight, still within 24 hours
            assertEquals(2, repo.cookSession.first()?.stepIndex)

            clock.advance(Duration.ofMinutes(1).plusMillis(1))
            assertNull(repo.cookSession.first())
        }

    @Test
    fun `every save restarts the 24 hours`() =
        runTest {
            val repo = repository()
            repo.saveCookSession(CookSession(recipeId = "r1", stepIndex = 0, updatedAt = 0))
            clock.advance(Duration.ofHours(20))
            repo.saveCookSession(repo.cookSession.first()!!.copy(stepIndex = 1))
            clock.advance(Duration.ofHours(20))
            assertEquals(1, repo.cookSession.first()?.stepIndex)
        }

    @Test
    fun `logging an attempt clears only that recipe's session`() =
        runTest {
            val repo = repository()
            repo.saveCookSession(CookSession(recipeId = "r1", stepIndex = 3, updatedAt = 0))
            repo.clearCookSession(recipeId = "other")
            assertEquals("r1", repo.cookSession.first()?.recipeId)
            repo.clearCookSession(recipeId = "r1")
            assertNull(repo.cookSession.first())
        }

    @Test
    fun `pruning drops expired state and keeps current state`() =
        runTest {
            val repo = repository()
            repo.pinTonight("r1")
            repo.dismissToday("r2")
            repo.saveCookSession(CookSession(recipeId = "r3", stepIndex = 1, updatedAt = 0))
            repo.pruneExpired()
            assertEquals("r1", repo.tonight.first()?.recipeId)

            clock.advance(Duration.ofDays(2))
            repo.pruneExpired()
            clock.instant = clock.instant.minus(Duration.ofDays(2)) // even back on the same day, the pruned state is gone
            assertNull(repo.tonight.first())
            assertEquals(emptySet<String>(), repo.todayDismissed.first())
            assertNull(repo.cookSession.first())
        }
}
