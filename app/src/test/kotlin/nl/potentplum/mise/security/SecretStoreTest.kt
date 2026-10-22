package nl.potentplum.mise.security

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException

/** Robolectric, so the platform's Keystore exceptions can be constructed; the cipher itself is a fake. */
@RunWith(AndroidJUnit4::class)
class SecretStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val key = "sk-ant-api03-abcDEF_123-wxyz"
    private val cipher = FakeCipher()

    private fun TestScope.store(): Pair<SecretStore, File> {
        val file = File(folder.root, "secrets.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        return SecretStore(dataStore, cipher, StandardTestDispatcher(testScheduler)) to file
    }

    @Test
    fun `a saved key reads back and is masked`() =
        runTest {
            val (store, _) = store()
            assertEquals(ClaudeKeyStatus.NONE, store.keyState.first().status)

            store.saveClaudeKey("  $key\n")
            val state = store.keyState.first()
            assertEquals(ClaudeKeyStatus.STORED, state.status)
            assertEquals("sk-ant-…wxyz", state.masked)
            assertEquals(key, store.readClaudeKey())
        }

    @Test
    fun `the plaintext key never reaches the file`() =
        runTest {
            val (store, file) = store()
            store.saveClaudeKey(key)
            store.keyState.first()
            val bytes = file.readBytes().toString(Charsets.ISO_8859_1)
            assertFalse(bytes.contains(key))
            assertFalse(bytes.contains("api03"))
        }

    @Test
    fun `deleting wipes the ciphertext and the keystore alias`() =
        runTest {
            val (store, _) = store()
            store.saveClaudeKey(key)
            store.deleteClaudeKey()
            assertEquals(ClaudeKeyStatus.NONE, store.keyState.first().status)
            assertNull(store.readClaudeKey())
            assertTrue(cipher.deleted)
        }

    @Test
    fun `an invalidated keystore key asks for the key again`() = assertNeedsReentry(KeyPermanentlyInvalidatedException())

    @Test
    fun `a bad gcm tag asks for the key again`() = assertNeedsReentry(AEADBadTagException())

    @Test
    fun `a missing alias asks for the key again`() = assertNeedsReentry(KeyMissingException())

    @Test
    fun `a corrupt ciphertext asks for the key again`() = assertNeedsReentry(IllegalArgumentException("bad base64"))

    private fun assertNeedsReentry(failure: Exception) =
        runTest {
            val (store, _) = store()
            store.saveClaudeKey(key)
            cipher.decryptFailure = failure

            assertNull(store.readClaudeKey())
            assertEquals(ClaudeKeyStatus.NEEDS_REENTRY, store.keyState.first().status)
            assertTrue(cipher.deleted)

            cipher.decryptFailure = null
            store.saveClaudeKey(key)
            assertEquals(ClaudeKeyStatus.STORED, store.keyState.first().status)
            assertEquals(key, store.readClaudeKey())
        }

    @Test
    fun `a temporarily unavailable keystore keeps the key`() =
        runTest {
            val (store, _) = store()
            store.saveClaudeKey(key)
            cipher.decryptFailure = GeneralSecurityException("device locked")

            val thrown = runCatching { store.readClaudeKey() }.exceptionOrNull()
            assertTrue(thrown is SecretUnavailableException)
            assertEquals(ClaudeKeyStatus.STORED, store.keyState.first().status)
            assertFalse(cipher.deleted)
        }

    /** Reversible, deliberately not real crypto: XOR with a fixed byte behind a 12-byte "IV". */
    private class FakeCipher : SecretCipher {
        var decryptFailure: Exception? = null
        var deleted = false

        override fun encrypt(plaintext: ByteArray): ByteArray {
            deleted = false
            return ByteArray(12) + plaintext.map { (it.toInt() xor 0x5A).toByte() }
        }

        override fun decrypt(blob: ByteArray): ByteArray {
            decryptFailure?.let { throw it }
            return blob.drop(12).map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
        }

        override fun deleteKey() {
            deleted = true
        }
    }
}
