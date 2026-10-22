package nl.potentplum.mise.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

/** Encrypts small secrets; the output is `IV + ciphertext`. */
interface SecretCipher {
    fun encrypt(plaintext: ByteArray): ByteArray

    /** Throws [KeyMissingException] when the key is gone, or the platform's exception when it is invalidated or locked. */
    fun decrypt(blob: ByteArray): ByteArray

    fun deleteKey()
}

class KeyMissingException : java.security.GeneralSecurityException("Keystore key is missing")

/**
 * AES-256-GCM with a non-exportable Android Keystore key (spec §9): StrongBox when the phone has it, otherwise TEE; the
 * key works only while the device is unlocked.
 */
class KeystoreCipher
    @Inject
    constructor() : SecretCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, existingKey() ?: createKey())
            return cipher.iv + cipher.doFinal(plaintext)
        }

        override fun decrypt(blob: ByteArray): ByteArray {
            val key = existingKey() ?: throw KeyMissingException()
            require(blob.size > IV_LENGTH) { "ciphertext too short" }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, blob, 0, IV_LENGTH))
            return cipher.doFinal(blob, IV_LENGTH, blob.size - IV_LENGTH)
        }

        override fun deleteKey() {
            keyStore().deleteEntry(ALIAS)
        }

        private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        private fun existingKey(): SecretKey? = (keyStore().getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey

        private fun createKey(): SecretKey =
            try {
                generateKey(strongBox = true)
            } catch (_: StrongBoxUnavailableException) {
                generateKey(strongBox = false)
            }

        private fun generateKey(strongBox: Boolean): SecretKey {
            val spec =
                KeyGenParameterSpec
                    .Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(KEY_BITS)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .setUnlockedDeviceRequired(true)
                    .setIsStrongBoxBacked(strongBox)
                    .build()
            return KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                .apply { init(spec) }
                .generateKey()
        }

        companion object {
            const val ALIAS = "mise_claude_key_wrap"
            private const val ANDROID_KEYSTORE = "AndroidKeyStore"
            private const val TRANSFORMATION = "AES/GCM/NoPadding"
            private const val KEY_BITS = 256
            private const val IV_LENGTH = 12
            private const val TAG_BITS = 128
        }
    }
