package eu.kanade.tachiyomi.util.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.nio.CharBuffer
import java.security.SecureRandom

internal const val PADDING_MIN = 42
internal const val PADDING_RANGE = 100
internal const val HEADER_BYTES = 128

// SY -->

/** Encrypts, decrypts and Base64-codes passwords before they are stored in shared preferences. */
public object CbzCrypto : CbzCryptoArchives() {
    /** File name of the encrypted database. */
    public const val DATABASE_NAME: String = "tachiyomiEncrypted.db"

    /** Removes the archive password key from the keystore. */
    public fun deleteKeyCbz() {
        keyStore.deleteEntry(ALIAS_CBZ)
        generateKey(ALIAS_CBZ)
    }

    /** [password] encrypted with the archive key, Base64 encoded. */
    public fun encryptCbz(password: String): String = encrypt(password.toByteArray(), encryptionCipherCbz)

    /** The archive password, decrypted. */
    public fun getDecryptedPasswordCbz(): ByteArray {
        val encryptedPassword = securityPreferences.cbzPassword.get()
        if (encryptedPassword.isBlank()) error("This archive is encrypted please set a password")

        return decrypt(encryptedPassword, ALIAS_CBZ)
    }

    private fun generateAndEncryptSqlPw() {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val passwordArray = CharArray(SQL_PASSWORD_LENGTH)
        for (i in 0..<SQL_PASSWORD_LENGTH) {
            passwordArray[i] = charPool[SecureRandom().nextInt(charPool.size)]
        }
        val passwordBuffer = Charsets.UTF_8.encode(CharBuffer.wrap(passwordArray))
        val passwordBytes = ByteArray(passwordBuffer.limit())
        passwordBuffer.get(passwordBytes)
        securityPreferences.sqlPassword.set(encrypt(passwordBytes, encryptionCipherSql))
            .also {
                passwordArray.fill('#')
                passwordBuffer.array().fill('#'.code.toByte())
                passwordBytes.fill('#'.code.toByte())
            }
    }

    /** The database password, decrypted; generated on first use. */
    public fun getDecryptedPasswordSql(): ByteArray {
        if (securityPreferences.sqlPassword.get().isBlank()) generateAndEncryptSqlPw()
        return decrypt(securityPreferences.sqlPassword.get(), ALIAS_SQL)
    }

    /** True when an archive password is stored. */
    public fun isPasswordSet(): Boolean = securityPreferences.cbzPassword.get().isNotEmpty()

    /** [isPasswordSet] as a state flow in [scope]. */
    public fun isPasswordSetState(scope: CoroutineScope): StateFlow<Boolean> = securityPreferences.cbzPassword.changes()
        .map { it.isNotEmpty() }
        .stateIn(scope, SharingStarted.Eagerly, false)
}
// SY <--
