package eu.kanade.tachiyomi.util.storage

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.KeyStore

@RunWith(RobolectricTestRunner::class)
internal class CbzCryptoKeysTest {
    private lateinit var crypto: TestCbzCrypto

    @Before
    fun setUp() {
        CryptoTestEnv.install()
        crypto = TestCbzCrypto()
    }

    @After
    fun tearDown() {
        CryptoTestEnv.restore()
        unmockkAll()
    }

    @Test
    fun generatesKeyWhenMissing() {
        crypto.store.deleteEntry(ALIAS_CBZ)
        val key = crypto.keyFor(ALIAS_CBZ)
        key.algorithm shouldBe ALGORITHM
        key.encoded.size shouldBe KEY_SIZE / Byte.SIZE_BITS
        crypto.store.containsAlias(ALIAS_CBZ) shouldBe true
    }

    @Test
    fun reusesStoredKey() {
        val first = crypto.newKey(ALIAS_SQL)
        crypto.keyFor(ALIAS_SQL).encoded shouldBe first.encoded
        crypto.newKey(ALIAS_SQL).encoded shouldNotBe first.encoded
    }

    @Test
    fun replacesForeignEntry() {
        crypto.store.setEntry(ALIAS_CBZ, NotASecretKeyEntry(), null)
        val key = crypto.keyFor(ALIAS_CBZ)
        val entry = crypto.store.getEntry(ALIAS_CBZ, null).shouldBeInstanceOf<KeyStore.SecretKeyEntry>()
        entry.secretKey.encoded shouldBe key.encoded
    }

    @Test
    fun roundTripsShortPassword() {
        val encrypted = crypto.encryptWith("hunter2".toByteArray(), ALIAS_CBZ)
        encrypted shouldNotBe "hunter2"
        crypto.decryptWith(encrypted, ALIAS_CBZ) shouldBe "hunter2".toByteArray()
    }

    @Test
    fun roundTripsLongPassword() {
        val password = ByteArray(BUFFER_SIZE * 2 + 7) { (it % 251).toByte() }
        val encrypted = crypto.encryptWith(password, ALIAS_SQL)
        crypto.decryptWith(encrypted, ALIAS_SQL) shouldBe password
    }

    @Test
    fun cipherTextDiffersPerCall() {
        val first = crypto.encryptWith("same".toByteArray(), ALIAS_CBZ)
        val second = crypto.encryptWith("same".toByteArray(), ALIAS_CBZ)
        first shouldNotBe second
        crypto.decryptWith(second, ALIAS_CBZ) shouldBe "same".toByteArray()
    }

    @Test
    fun decryptCipherUsesGivenIv() {
        val iv = ByteArray(IV_SIZE) { it.toByte() }
        crypto.decryptCipher(iv, ALIAS_CBZ).iv shouldBe iv
    }

    @Test
    fun exposesDefaults() {
        crypto.coverName shouldBe "cover.jpg"
        crypto.prefs shouldBe CryptoTestEnv.prefs
        CRYPTO_SETTINGS shouldBe "AES/CBC/PKCS7Padding"
        crypto.store.type shouldBe KEYSTORE
    }
}
