package eu.kanade.tachiyomi.util.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import uy.kohesive.injekt.injectLazy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/** The AndroidKeyStore half of [CbzCrypto]: key generation, ciphers, and the raw encrypt/decrypt. */
public abstract class CbzCryptoKeys {
    protected val defaultCoverName: String = "cover.jpg"
    protected val securityPreferences: SecurityPreferences by injectLazy()
    protected val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE).apply {
        load(null)
    }

    protected val encryptionCipherCbz: Cipher
        get() = Cipher.getInstance(CRYPTO_SETTINGS).apply {
            init(
                Cipher.ENCRYPT_MODE,
                getKey(ALIAS_CBZ),
            )
        }

    protected val encryptionCipherSql: Cipher
        get() = Cipher.getInstance(CRYPTO_SETTINGS).apply {
            init(
                Cipher.ENCRYPT_MODE,
                getKey(ALIAS_SQL),
            )
        }

    protected fun getDecryptCipher(iv: ByteArray, alias: String): Cipher = Cipher.getInstance(CRYPTO_SETTINGS).apply {
        init(
            Cipher.DECRYPT_MODE,
            getKey(alias),
            IvParameterSpec(iv),
        )
    }

    protected fun getKey(alias: String): SecretKey {
        val loadedKey = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
        return loadedKey?.secretKey ?: generateKey(alias)
    }

    protected fun generateKey(alias: String): SecretKey = KeyGenerator.getInstance(ALGORITHM).apply {
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

    protected fun encrypt(password: ByteArray, cipher: Cipher): String {
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

    protected fun decrypt(encryptedPassword: String, alias: String): ByteArray {
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
}

internal const val BUFFER_SIZE = 2048
internal const val KEY_SIZE = 256
internal const val IV_SIZE = 16

internal const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
internal const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
internal const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
internal const val CRYPTO_SETTINGS = "$ALGORITHM/$BLOCK_MODE/$PADDING"

internal const val KEYSTORE = "AndroidKeyStore"
internal const val ALIAS_CBZ = "cbzPw"
internal const val ALIAS_SQL = "sqlPw"

internal const val SQL_PASSWORD_LENGTH = 32

internal class ByteArrayOutputStreamPassword : ByteArrayOutputStream() {
    fun clear() {
        this.buf.fill('#'.code.toByte())
    }
}
