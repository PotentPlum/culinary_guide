package nl.potentplum.mise.claude.enrich

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import nl.potentplum.mise.claude.ClaudeError
import nl.potentplum.mise.data.db.entity.EnrichState
import nl.potentplum.mise.platform.BackgroundNotifications
import nl.potentplum.mise.platform.NotificationChannels
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class EnrichmentWorkerTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var fixture: EnrichmentFixture? = null

    @After
    fun tearDown() {
        fixture?.db?.close()
    }

    private suspend fun TestScope.setUp(): EnrichmentFixture =
        EnrichmentFixture(folder.root, backgroundScope, StandardTestDispatcher(testScheduler)).also {
            fixture = it
            it.secrets.saveClaudeKey("sk-ant-test")
            it.recipes.save(EnrichmentFixture.recipe)
            it.recipes.setEnrichState("r1", EnrichState.PENDING)
            NotificationChannels.create(context)
            shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        }

    private fun worker(
        f: EnrichmentFixture,
        attempt: Int = 0,
    ): EnrichmentWorker {
        val factory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = EnrichmentWorker(appContext, workerParameters, f.enricher, f.recipes, BackgroundNotifications(appContext))
            }
        return TestListenableWorkerBuilder<EnrichmentWorker>(context)
            .setInputData(workDataOf(EnrichmentWorker.KEY_RECIPE_ID to "r1"))
            .setRunAttemptCount(attempt)
            .setWorkerFactory(factory)
            .build()
    }

    private val notifications get() = shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications

    @Test
    fun `success saves the enrichment`() =
        runTest {
            val f = setUp()
            assertEquals(ListenableWorker.Result.success(), worker(f).doWork())
            assertEquals(EnrichState.DONE, f.recipes.get("r1")!!.enrichState)
            assertTrue(notifications.isEmpty())
        }

    @Test
    fun `a network error is retried and stays PENDING until the last attempt`() =
        runTest {
            val f = setUp()
            f.client.failure = ClaudeError.Network
            assertEquals(ListenableWorker.Result.retry(), worker(f).doWork())
            assertEquals(EnrichState.PENDING, f.recipes.get("r1")!!.enrichState)

            assertEquals(ListenableWorker.Result.failure(), worker(f, attempt = EnrichmentWorker.MAX_RETRIES).doWork())
            assertEquals(EnrichState.FAILED, f.recipes.get("r1")!!.enrichState)
            val notification = notifications.single()
            assertEquals(NotificationChannels.BACKGROUND, notification.channelId)
            assertTrue(notification.extras.getString("android.text")!!.contains("Pompoensoep"))
        }

    @Test
    fun `a refusal fails at once, keeps the recipe and notifies`() =
        runTest {
            val f = setUp()
            f.client.failure = ClaudeError.Refused(null)
            assertEquals(ListenableWorker.Result.failure(), worker(f).doWork())
            assertEquals(EnrichmentFixture.recipe.ingredients, f.recipes.get("r1")!!.ingredients)
            assertEquals(EnrichState.FAILED, f.recipes.get("r1")!!.enrichState)
            assertEquals(1, notifications.size)
        }

    @Test
    fun `a removed key resets the state without a notification`() =
        runTest {
            val f = setUp()
            f.secrets.deleteClaudeKey()
            assertEquals(ListenableWorker.Result.failure(), worker(f).doWork())
            assertEquals(EnrichState.NONE, f.recipes.get("r1")!!.enrichState)
            assertTrue(notifications.isEmpty())
        }
}
