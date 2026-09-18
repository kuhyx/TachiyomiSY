package eu.kanade.tachiyomi.util.storage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.unmockkAll
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.KeyStore

@RunWith(RobolectricTestRunner::class)
internal class CbzCryptoTest {
    @Before
    fun setUp() {
        CryptoTestEnv.install()
    }

    @After
    fun tearDown() {
        CryptoTestEnv.restore()
        unmockkAll()
    }

    @Test
    fun databaseNameIsStable() {
        CbzCrypto.DATABASE_NAME shouldBe "tachiyomiEncrypted.db"
    }

    @Test
    fun passwordSetFollowsPreference() {
        CbzCrypto.isPasswordSet() shouldBe false
        CryptoTestEnv.prefs.cbzPassword.set("stored")
        CbzCrypto.isPasswordSet() shouldBe true
    }

    @Test
    fun passwordStateFollowsChanges() {
        val scope = inlineScope()
        val state = CbzCrypto.isPasswordSetState(scope)
        state.value shouldBe false
        CryptoTestEnv.prefs.cbzPassword.set("stored")
        state.value shouldBe true
        CryptoTestEnv.prefs.cbzPassword.delete()
        state.value shouldBe false
        scope.cancel()
    }

    @Test
    fun missingCbzPasswordThrows() {
        shouldThrow<IllegalStateException> { CbzCrypto.getDecryptedPasswordCbz() }
        CryptoTestEnv.prefs.cbzPassword.set("   ")
        shouldThrow<IllegalStateException> { CbzCrypto.getDecryptedPasswordCbz() }
    }

    @Test
    fun cbzPasswordRoundTrips() {
        CryptoTestEnv.prefs.cbzPassword.set(CbzCrypto.encryptCbz("hunter2"))
        CbzCrypto.getDecryptedPasswordCbz() shouldBe "hunter2".toByteArray()
    }

    @Test
    fun deleteKeyRotatesArchiveKey() {
        CbzCrypto.encryptCbz("seed")
        val before = storedKey(ALIAS_CBZ)
        CbzCrypto.deleteKeyCbz()
        storedKey(ALIAS_CBZ) shouldNotBe before
    }

    @Test
    fun sqlPasswordIsGeneratedOnce() {
        val first = CbzCrypto.getDecryptedPasswordSql()
        first.size shouldBe SQL_PASSWORD_LENGTH
        first.all { it.toInt().toChar().isLetterOrDigit() } shouldBe true
        CryptoTestEnv.prefs.sqlPassword.get().isNotBlank() shouldBe true
        CbzCrypto.getDecryptedPasswordSql() shouldBe first
    }

    @Test
    fun sqlPasswordRegenerates() {
        val first = CbzCrypto.getDecryptedPasswordSql()
        CryptoTestEnv.prefs.sqlPassword.delete()
        CbzCrypto.getDecryptedPasswordSql() shouldNotBe first
    }
}

/** The raw bytes of the AES key the fake keystore holds under [alias]. */
internal fun storedKey(alias: String): List<Byte> {
    val store = KeyStore.getInstance(KEYSTORE)
    store.load(null)
    val entry = store.getEntry(alias, null) as KeyStore.SecretKeyEntry
    return entry.secretKey.encoded.toList()
}
