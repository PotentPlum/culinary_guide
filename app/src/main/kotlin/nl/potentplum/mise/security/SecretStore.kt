package nl.potentplum.mise.security

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import nl.potentplum.mise.core.util.IoDispatcher
import nl.potentplum.mise.data.settings.SecretsStore
import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.inject.Inject
import javax.inject.Singleton

enum class ClaudeKeyStatus {
    NONE,
    STORED,

    /** The stored key could not be decrypted and was wiped: "Sleutel opnieuw invoeren" (spec §9). */
    NEEDS_REENTRY,
}

data class ClaudeKeyState(
    val status: ClaudeKeyStatus,
    /** Last 4 characters, for the masked display `sk-ant-…abcd`. */
    val hint: String? = null,
) {
    val masked: String? get() = hint?.let { "sk-ant-…$it" }
}

/** The key exists but can't be used right now (e.g. the phone is locked); nothing was wiped. Try again later. */
class SecretUnavailableException(
    cause: Throwable,
) : Exception("Secret temporarily unavailable", cause)

/**
 * The Claude API key, encrypted with [SecretCipher] in `datastore/secrets.preferences_pb`, a file that is excluded from
 * every backup (spec §9). The plaintext never touches disk; read it just before a request and don't keep it.
 */
@Singleton
class SecretStore
    @Inject
    constructor(
        @param:SecretsStore private val dataStore: DataStore<Preferences>,
        private val cipher: SecretCipher,
        @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    ) {
        val keyState: Flow<ClaudeKeyState> =
            dataStore.data
                .map { prefs ->
                    when {
                        prefs[CIPHERTEXT] != null -> ClaudeKeyState(ClaudeKeyStatus.STORED, prefs[HINT])
                        prefs[NEEDS_REENTRY] == true -> ClaudeKeyState(ClaudeKeyStatus.NEEDS_REENTRY)
                        else -> ClaudeKeyState(ClaudeKeyStatus.NONE)
                    }
                }.distinctUntilChanged()

        suspend fun saveClaudeKey(key: String) {
            val trimmed = key.trim()
            require(trimmed.isNotEmpty()) { "empty key" }
            val blob = withContext(dispatcher) { cipher.encrypt(trimmed.toByteArray(Charsets.UTF_8)) }
            dataStore.edit {
                it[CIPHERTEXT] = Base64.getEncoder().encodeToString(blob)
                it[HINT] = trimmed.takeLast(HINT_LENGTH)
                it.remove(NEEDS_REENTRY)
            }
        }

        /**
         * The decrypted key, or null when there is none. An invalidated key, a missing Keystore alias or a corrupt
         * ciphertext wipes the stored key and sets [ClaudeKeyStatus.NEEDS_REENTRY]; it never crashes.
         *
         * @throws SecretUnavailableException when the Keystore refuses for another reason, such as a locked phone.
         */
        suspend fun readClaudeKey(): String? {
            val encoded = dataStore.data.first()[CIPHERTEXT] ?: return null
            return withContext(dispatcher) {
                try {
                    String(cipher.decrypt(Base64.getDecoder().decode(encoded)), Charsets.UTF_8)
                } catch (e: Exception) {
                    if (!e.isUnrecoverable()) {
                        throw if (e is GeneralSecurityException) SecretUnavailableException(e) else e
                    }
                    wipe(needsReentry = true)
                    null
                }
            }
        }

        /** "Sleutel verwijderen": wipes the ciphertext and the Keystore alias. */
        suspend fun deleteClaudeKey() = wipe(needsReentry = false)

        private suspend fun wipe(needsReentry: Boolean) {
            dataStore.edit {
                it.remove(CIPHERTEXT)
                it.remove(HINT)
                if (needsReentry) it[NEEDS_REENTRY] = true else it.remove(NEEDS_REENTRY)
            }
            withContext(dispatcher) { runCatching { cipher.deleteKey() } }
        }

        private fun Exception.isUnrecoverable(): Boolean =
            this is KeyPermanentlyInvalidatedException ||
                this is AEADBadTagException ||
                this is KeyMissingException ||
                this is IllegalArgumentException

        private companion object {
            const val HINT_LENGTH = 4
            val CIPHERTEXT = stringPreferencesKey("claudeKeyCiphertext")
            val HINT = stringPreferencesKey("claudeKeyHint")
            val NEEDS_REENTRY = booleanPreferencesKey("claudeKeyNeedsReentry")
        }
    }
