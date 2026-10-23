package nl.potentplum.mise.testing

import nl.potentplum.mise.security.SecretCipher

/** Reversible, deliberately not real crypto: XOR with a fixed byte behind a 12-byte "IV". */
class FakeCipher : SecretCipher {
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
