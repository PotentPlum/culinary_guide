package nl.potentplum.mise.testing

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import nl.potentplum.mise.claude.ClaudeClient
import nl.potentplum.mise.claude.ClaudeError
import nl.potentplum.mise.claude.ClaudeModule
import nl.potentplum.mise.claude.ClaudeRequest
import nl.potentplum.mise.claude.ClaudeResponse
import nl.potentplum.mise.claude.KeyCheck
import nl.potentplum.mise.claude.Usage
import nl.potentplum.mise.claude.enrich.EnrichmentModule
import nl.potentplum.mise.claude.enrich.EnrichmentScheduler
import nl.potentplum.mise.security.SecretCipher
import nl.potentplum.mise.security.SecurityModule

/** Recorded replies for UI tests; never calls the network. */
object FakeClaudeClient : ClaudeClient {
    var reply: JsonObject = JsonObject(emptyMap())
    var failure: ClaudeError? = null
    val requests = mutableListOf<ClaudeRequest>()

    fun reset() {
        reply = JsonObject(emptyMap())
        failure = null
        requests.clear()
    }

    override suspend fun send(
        key: String,
        request: ClaudeRequest,
    ): ClaudeResponse {
        requests += request
        failure?.let { throw it }
        return ClaudeResponse(reply, Usage(3_000, 800), request.model)
    }

    override suspend fun checkKey(key: String) = KeyCheck.VALID

    fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject
}

/** Enrichment requests, recorded instead of handed to WorkManager. */
object FakeEnrichmentScheduler : EnrichmentScheduler {
    val scheduled = mutableListOf<String>()

    override fun schedule(recipeId: String) {
        scheduled += recipeId
    }
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ClaudeModule::class])
object FakeClaudeModule {
    @Provides
    fun claudeClient(): ClaudeClient = FakeClaudeClient
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [EnrichmentModule::class])
object FakeEnrichmentModule {
    @Provides
    fun scheduler(): EnrichmentScheduler = FakeEnrichmentScheduler
}

/** Robolectric has no AndroidKeyStore. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SecurityModule::class])
object FakeSecurityModule {
    @Provides
    fun cipher(): SecretCipher = FakeCipher()
}
