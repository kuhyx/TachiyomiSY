package eu.kanade.tachiyomi.util.storage

import eu.kanade.tachiyomi.core.security.SecurityPreferences
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey

/** Opens the protected half of [CbzCryptoKeys] and [CbzCryptoArchives] to the tests. */
internal class TestCbzCrypto : CbzCryptoArchives() {
    val store: KeyStore
        get() = keyStore

    val prefs: SecurityPreferences
        get() = securityPreferences

    val coverName: String
        get() = defaultCoverName

    fun cipherFor(alias: String): Cipher = if (alias == ALIAS_CBZ) encryptionCipherCbz else encryptionCipherSql

    fun keyFor(alias: String): SecretKey = getKey(alias)

    fun newKey(alias: String): SecretKey = generateKey(alias)

    fun encryptWith(password: ByteArray, alias: String): String = encrypt(password, cipherFor(alias))

    fun decryptWith(encrypted: String, alias: String): ByteArray = decrypt(encrypted, alias)

    fun decryptCipher(iv: ByteArray, alias: String): Cipher = getDecryptCipher(iv, alias)
}
