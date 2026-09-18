package eu.kanade.tachiyomi.util.storage

import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.spec.AlgorithmParameterSpec
import java.util.Collections
import java.util.Date
import java.util.Enumeration
import javax.crypto.KeyGeneratorSpi
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Registers an in-memory JCA provider named like the AndroidKeyStore, so that
 * `KeyStore.getInstance("AndroidKeyStore")` and `KeyGenerator.init(KeyGenParameterSpec)`
 * work on the JVM. Idempotent: an already registered provider is kept (its entries too)
 * and only moved back to the front of the provider list.
 */
internal fun installFakeAndroidKeyStore() {
    val provider = Security.getProvider(KEYSTORE) ?: FakeAndroidKeyStoreProvider()
    Security.removeProvider(KEYSTORE)
    Security.insertProviderAt(provider, 1)
}

/** The entries every [FakeKeyStoreSpi] and [FakeAesKeyGeneratorSpi] of this class loader share. */
internal object FakeKeyStoreBackend {
    val entries: MutableMap<String, KeyStore.Entry> = mutableMapOf()
}

/** A [KeyStore.Entry] that is not a [KeyStore.SecretKeyEntry], to exercise the `as?` miss. */
internal class NotASecretKeyEntry : KeyStore.Entry

internal class FakeAndroidKeyStoreProvider : Provider(KEYSTORE, 1.0, "In-memory AndroidKeyStore for unit tests") {
    init {
        putService(
            FakeService(provider = this, type = "KeyStore", algorithm = KEYSTORE, factory = ::FakeKeyStoreSpi),
        )
        putService(
            FakeService(
                provider = this,
                type = "KeyGenerator",
                algorithm = ALGORITHM,
                factory = ::FakeAesKeyGeneratorSpi,
            ),
        )
    }

    private companion object {
        private const val serialVersionUID = 1L
    }
}

private class FakeService(
    provider: Provider,
    type: String,
    algorithm: String,
    private val factory: () -> Any,
) : Provider.Service(provider, type, algorithm, "fake", null, null) {
    override fun newInstance(constructorParameter: Any?): Any = factory()
}

internal class FakeKeyStoreSpi : KeyStoreSpi() {
    override fun engineGetKey(alias: String, password: CharArray?): Key? =
        (FakeKeyStoreBackend.entries[alias] as? KeyStore.SecretKeyEntry)?.secretKey

    override fun engineGetCertificateChain(alias: String): Array<Certificate>? = null

    override fun engineGetCertificate(alias: String): Certificate? = null

    override fun engineGetCreationDate(alias: String): Date? = null

    override fun engineSetKeyEntry(alias: String, key: Key, password: CharArray?, chain: Array<Certificate>?) {
        FakeKeyStoreBackend.entries[alias] = KeyStore.SecretKeyEntry(key as SecretKey)
    }

    override fun engineSetKeyEntry(alias: String, key: ByteArray, chain: Array<Certificate>?) {
        FakeKeyStoreBackend.entries[alias] = KeyStore.SecretKeyEntry(SecretKeySpec(key, ALGORITHM))
    }

    override fun engineSetCertificateEntry(alias: String, cert: Certificate) {
        FakeKeyStoreBackend.entries[alias] = KeyStore.TrustedCertificateEntry(cert)
    }

    override fun engineDeleteEntry(alias: String) {
        FakeKeyStoreBackend.entries.remove(alias)
    }

    override fun engineAliases(): Enumeration<String> = Collections.enumeration(FakeKeyStoreBackend.entries.keys)

    override fun engineContainsAlias(alias: String): Boolean = FakeKeyStoreBackend.entries.containsKey(alias)

    override fun engineSize(): Int = FakeKeyStoreBackend.entries.size

    override fun engineIsKeyEntry(alias: String): Boolean =
        FakeKeyStoreBackend.entries[alias] is KeyStore.SecretKeyEntry

    override fun engineIsCertificateEntry(alias: String): Boolean =
        FakeKeyStoreBackend.entries[alias] is KeyStore.TrustedCertificateEntry

    override fun engineGetCertificateAlias(cert: Certificate): String? = null

    override fun engineStore(stream: OutputStream?, password: CharArray?) {
        // Nothing to persist: the backend lives in memory.
    }

    override fun engineLoad(stream: InputStream?, password: CharArray?) {
        // Nothing to load: the backend lives in memory.
    }

    override fun engineGetEntry(alias: String, protParam: KeyStore.ProtectionParameter?): KeyStore.Entry? =
        FakeKeyStoreBackend.entries[alias]

    override fun engineSetEntry(alias: String, entry: KeyStore.Entry, protParam: KeyStore.ProtectionParameter?) {
        FakeKeyStoreBackend.entries[alias] = entry
    }
}

/**
 * Generates AES keys and, like the real AndroidKeyStore, stores them under the alias of the
 * `KeyGenParameterSpec`. The spec is read reflectively so the class works whichever class
 * loader (Robolectric sandbox) created it.
 */
internal class FakeAesKeyGeneratorSpi : KeyGeneratorSpi() {
    private var alias: String? = null
    private var keySize: Int = KEY_SIZE
    private var random: SecureRandom = SecureRandom()

    override fun engineInit(random: SecureRandom) {
        this.random = random
    }

    override fun engineInit(params: AlgorithmParameterSpec, random: SecureRandom) {
        alias = params.javaClass.getMethod("getKeystoreAlias").invoke(params) as String
        keySize = params.javaClass.getMethod("getKeySize").invoke(params) as Int
        this.random = random
    }

    override fun engineInit(keysize: Int, random: SecureRandom) {
        keySize = keysize
        this.random = random
    }

    override fun engineGenerateKey(): SecretKey {
        val bytes = ByteArray(keySize / Byte.SIZE_BITS)
        random.nextBytes(bytes)
        val key = SecretKeySpec(bytes, ALGORITHM)
        alias?.let { FakeKeyStoreBackend.entries[it] = KeyStore.SecretKeyEntry(key) }
        return key
    }
}
