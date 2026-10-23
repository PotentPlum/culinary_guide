package nl.potentplum.mise.claude

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.MiseDatabase
import nl.potentplum.mise.data.db.entity.UsageFeature
import nl.potentplum.mise.security.ClaudeKeyStatus
import nl.potentplum.mise.security.SecretStore
import nl.potentplum.mise.testing.FakeCipher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ClaudeServiceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val db =
        Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                MiseDatabase::class.java,
            ).allowMainThreadQueries()
            .build()

    @After
    fun tearDown() = db.close()

    private class FakeClient(
        var failure: ClaudeError? = null,
    ) : ClaudeClient {
        var lastKey: String? = null

        override suspend fun send(
            key: String,
            request: ClaudeRequest,
        ): ClaudeResponse {
            lastKey = key
            failure?.let { throw it }
            return ClaudeResponse(JsonObject(emptyMap()), Usage(10_000, 2_000), request.model)
        }

        override suspend fun checkKey(key: String) = KeyCheck.VALID
    }

    private fun TestScope.service(client: FakeClient): Pair<ClaudeService, SecretStore> {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "secrets.preferences_pb") }
        val secrets = SecretStore(store, FakeCipher(), StandardTestDispatcher(testScheduler))
        return ClaudeService(client, secrets, db.usageDao(), MiseClock.fixed(LocalDate.now(MiseClock.ZONE))) to secrets
    }

    private val request = ClaudeRequest(ClaudeModels.HAIKU, Effort.LOW, "s", listOf(ContentPart.Text("t")), OutputSchemas.obj(emptyMap()))

    @Test
    fun `without a key nothing is sent`() =
        runTest {
            val client = FakeClient()
            val (service, _) = service(client)
            assertEquals(ClaudeError.NoKey, runCatching { service.send(UsageFeature.ENRICH, request) }.exceptionOrNull())
            assertEquals(null, client.lastKey)
        }

    @Test
    fun `the key is read per call and usage is recorded with its cost`() =
        runTest {
            val client = FakeClient()
            val (service, secrets) = service(client)
            secrets.saveClaudeKey("sk-ant-abc")
            service.send(UsageFeature.ENRICH, request)
            assertEquals("sk-ant-abc", client.lastKey)
            val totals = service.thisMonth().first()
            assertEquals(10_000, totals.inputTokens)
            assertEquals(2_000, totals.costMicroUsd)
        }

    @Test
    fun `a 401 marks the key invalid`() =
        runTest {
            val client = FakeClient(failure = ClaudeError.InvalidKey)
            val (service, secrets) = service(client)
            secrets.saveClaudeKey("sk-ant-abc")
            runCatching { service.send(UsageFeature.QUEST, request) }
            assertEquals(ClaudeKeyStatus.INVALID, secrets.keyState.first().status)
        }
}
