package nl.potentplum.mise.claude

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.potentplum.mise.core.util.MiseClock
import nl.potentplum.mise.data.db.dao.UsageDao
import nl.potentplum.mise.data.db.dao.UsageTotals
import nl.potentplum.mise.data.db.entity.ClaudeUsageEntity
import nl.potentplum.mise.data.db.entity.UsageFeature
import nl.potentplum.mise.data.repo.newId
import nl.potentplum.mise.security.ClaudeKeyStatus
import nl.potentplum.mise.security.SecretStore
import nl.potentplum.mise.security.SecretUnavailableException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every Claude call goes through here: the key is decrypted just before the request and not kept (spec §9), a 401
 * marks it invalid, and each call's usage is recorded with its estimated cost (spec §8.1).
 */
@Singleton
class ClaudeService
    @Inject
    constructor(
        private val client: ClaudeClient,
        private val secrets: SecretStore,
        private val usage: UsageDao,
        private val clock: MiseClock,
    ) {
        /** True while a usable key is stored. */
        val hasKey: Flow<Boolean> = secrets.keyState.map { it.status == ClaudeKeyStatus.STORED }

        suspend fun send(
            feature: UsageFeature,
            request: ClaudeRequest,
        ): ClaudeResponse {
            val key =
                try {
                    secrets.readClaudeKey()
                } catch (_: SecretUnavailableException) {
                    null
                } ?: throw ClaudeError.NoKey
            val response =
                try {
                    client.send(key, request)
                } catch (e: ClaudeError.InvalidKey) {
                    secrets.markInvalid()
                    throw e
                }
            record(feature, response.model, response.usage)
            return response
        }

        /** The live check before saving a key (spec §9). */
        suspend fun checkKey(key: String): KeyCheck {
            val result = client.checkKey(key)
            if (result != KeyCheck.UNREACHABLE) record(UsageFeature.KEY_CHECK, "", Usage(0, 0))
            return result
        }

        /** Tokens and estimated cost since the first of this month (Instellingen, spec §6.15). */
        fun thisMonth(): Flow<UsageTotals> {
            val start =
                LocalDate
                    .now(MiseClock.ZONE)
                    .withDayOfMonth(1)
                    .atStartOfDay(MiseClock.ZONE)
                    .toInstant()
                    .toEpochMilli()
            return usage.observeTotalsSince(start)
        }

        private suspend fun record(
            feature: UsageFeature,
            model: String,
            tokens: Usage,
        ) {
            usage.insert(
                ClaudeUsageEntity(
                    id = newId(),
                    at = clock.nowMillis(),
                    feature = feature,
                    model = model,
                    inputTokens = tokens.inputTokens,
                    outputTokens = tokens.outputTokens,
                    cacheReadTokens = tokens.cacheReadTokens,
                    costMicroUsd = if (model.isEmpty()) 0 else Pricing.costMicroUsd(model, tokens),
                ),
            )
        }
    }
