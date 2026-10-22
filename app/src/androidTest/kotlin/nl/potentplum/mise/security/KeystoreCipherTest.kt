package nl.potentplum.mise.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Needs a real Keystore: run on a device or emulator (`./gradlew connectedDebugAndroidTest`). */
@RunWith(AndroidJUnit4::class)
class KeystoreCipherTest {
    private val cipher = KeystoreCipher()

    @After
    fun tearDown() = cipher.deleteKey()

    @Test
    fun encryptsAndDecrypts() {
        val plaintext = "sk-ant-test-key".toByteArray()
        assertArrayEquals(plaintext, cipher.decrypt(cipher.encrypt(plaintext)))
    }

    @Test
    fun usesAFreshIvEveryTime() {
        val plaintext = "same".toByteArray()
        assertFalse(cipher.encrypt(plaintext).contentEquals(cipher.encrypt(plaintext)))
    }

    @Test(expected = KeyMissingException::class)
    fun aDeletedKeyCannotDecrypt() {
        val blob = cipher.encrypt("x".toByteArray())
        cipher.deleteKey()
        cipher.decrypt(blob)
    }
}
