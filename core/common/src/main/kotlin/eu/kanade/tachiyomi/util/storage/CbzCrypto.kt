package eu.kanade.tachiyomi.util.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import mihon.core.common.archive.ArchiveReader
import tachiyomi.core.common.util.system.ImageUtil
import uy.kohesive.injekt.injectLazy
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.CharBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

private const val PADDING_MIN = 42
private const val PADDING_RANGE = 100
private const val HEADER_BYTES = 128

// SY -->

/** Encrypts, decrypts and Base64-codes passwords before they are stored in shared preferences. */
public object CbzCrypto {
    /** File name of the encrypted database. */
    public const val DATABASE_NAME: String = "tachiyomiEncrypted.db"
    private const val DEFAULT_COVER_NAME = "cover.jpg"
    private val securityPreferences: SecurityPreferences by injectLazy()
    private val keyStore = KeyStore.getInstance(KEYSTORE).apply {
        load(null)
    }

    private val encryptionCipherCbz
        get() = Cipher.getInstance(CRYPTO_SETTINGS).apply {
            init(
                Cipher.ENCRYPT_MODE,
                getKey(ALIAS_CBZ),
            )
        }

    private val encryptionCipherSql
        get() = Cipher.getInstance(CRYPTO_SETTINGS).apply {
            init(
                Cipher.ENCRYPT_MODE,
                getKey(ALIAS_SQL),
            )
        }

    private fun getDecryptCipher(iv: ByteArray, alias: String): Cipher = Cipher.getInstance(CRYPTO_SETTINGS).apply {
        init(
            Cipher.DECRYPT_MODE,
            getKey(alias),
            IvParameterSpec(iv),
        )
    }

    private fun getKey(alias: String): SecretKey {
        val loadedKey = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
        return loadedKey?.secretKey ?: generateKey(alias)
    }

    private fun generateKey(alias: String): SecretKey = KeyGenerator.getInstance(ALGORITHM).apply {
        init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(KEY_SIZE)
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(PADDING)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false)
                .build(),
        )
    }.generateKey()

    private fun encrypt(password: ByteArray, cipher: Cipher): String {
        val outputStream = ByteArrayOutputStream()
        outputStream.use { output ->
            output.write(cipher.iv)
            ByteArrayInputStream(password).use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (input.available() > BUFFER_SIZE) {
                    input.read(buffer)
                    output.write(cipher.update(buffer))
                }
                output.write(cipher.doFinal(input.readBytes()))
            }
        }
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    private fun decrypt(encryptedPassword: String, alias: String): ByteArray {
        val inputStream = Base64.decode(encryptedPassword, Base64.DEFAULT).inputStream()
        return inputStream.use { input ->
            val iv = ByteArray(IV_SIZE)
            input.read(iv)
            val cipher = getDecryptCipher(iv, alias)
            ByteArrayOutputStreamPassword().use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (inputStream.available() > BUFFER_SIZE) {
                    inputStream.read(buffer)
                    output.write(cipher.update(buffer))
                }
                output.write(cipher.doFinal(inputStream.readBytes()))
                output.toByteArray().also {
                    output.clear()
                }
            }
        }
    }

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

    /** True when downloads must be password protected. */
    public fun getPasswordProtectDlPref(): Boolean = securityPreferences.passwordProtectDownloads.get()

    /** Random padding for ComicInfo.xml when downloads are protected, else null. */
    public fun createComicInfoPadding(): String? = if (getPasswordProtectDlPref()) {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        List(SecureRandom().nextInt(PADDING_RANGE) + PADDING_MIN) { charPool.random() }.joinToString("")
    } else {
        null
    }

    /** The configured cipher as the archive library expects it. */
    public fun getPreferredEncryptionAlgo(): ByteArray =
        when (securityPreferences.encryptionType.get()) {
            SecurityPreferences.EncryptionType.AES_256 -> "zip:encryption=aes256".toByteArray()
            SecurityPreferences.EncryptionType.AES_128 -> "zip:encryption=aes128".toByteArray()
            SecurityPreferences.EncryptionType.ZIP_STANDARD -> "zip:encryption=zipcrypt".toByteArray()
        }

    /** True when [stream] is a protected archive holding a cover image. */
    public fun detectCoverImageArchive(stream: InputStream): Boolean {
        val bytes = ByteArray(HEADER_BYTES)
        if (stream.markSupported()) {
            stream.mark(bytes.size)
            stream.read(bytes, 0, bytes.size).also { stream.reset() }
        } else {
            stream.read(bytes, 0, bytes.size)
        }
        return String(bytes).contains(DEFAULT_COVER_NAME, ignoreCase = true)
    }

    /** The first image entry of the archive, or null. */
    public fun ArchiveReader.getCoverStream(): BufferedInputStream? {
        this.getInputStream(DEFAULT_COVER_NAME)?.let { stream ->
            if (ImageUtil.isImage(DEFAULT_COVER_NAME) { stream }) {
                return this.getInputStream(DEFAULT_COVER_NAME)?.buffered()
            }
        }
        return null
    }
}

private const val BUFFER_SIZE = 2048
private const val KEY_SIZE = 256
private const val IV_SIZE = 16

private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
private const val CRYPTO_SETTINGS = "$ALGORITHM/$BLOCK_MODE/$PADDING"

private const val KEYSTORE = "AndroidKeyStore"
private const val ALIAS_CBZ = "cbzPw"
private const val ALIAS_SQL = "sqlPw"

private const val SQL_PASSWORD_LENGTH = 32

private class ByteArrayOutputStreamPassword : ByteArrayOutputStream() {
    fun clear() {
        this.buf.fill('#'.code.toByte())
    }
}
// SY <--
