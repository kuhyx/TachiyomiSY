package eu.kanade.domain

import io.mockk.every
import io.mockk.mockk
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.Security
import java.security.cert.Certificate
import java.util.Collections
import java.util.Date
import java.util.Enumeration

private const val PROVIDER_NAME = "FakeAndroidKeyStore"

/**
 * Registers a JVM security provider that answers `KeyStore.getInstance("AndroidKeyStore")` with an empty,
 * in-memory store, so code that opens the Android keystore on construction can load on a plain JVM.
 * The provider is a mock rather than a subclass because the Android SDK stubs and the JDK disagree on
 * which [Provider] constructor is current.
 */
internal fun installFakeAndroidKeyStore() {
    if (Security.getProvider(PROVIDER_NAME) != null) return
    val provider = mockk<Provider>(relaxed = true)
    val service = Provider.Service(
        provider,
        "KeyStore",
        "AndroidKeyStore",
        FakeAndroidKeyStoreSpi::class.java.name,
        null,
        null,
    )
    every { provider.name } returns PROVIDER_NAME
    every { provider.getService("KeyStore", "AndroidKeyStore") } returns service
    every { provider.services } returns setOf(service)
    Security.addProvider(provider)
}

/** A keystore with no entries that accepts every operation. */
internal class FakeAndroidKeyStoreSpi : KeyStoreSpi() {
    override fun engineGetKey(alias: String?, password: CharArray?): Key? = null

    override fun engineGetCertificateChain(alias: String?): Array<Certificate>? = null

    override fun engineGetCertificate(alias: String?): Certificate? = null

    override fun engineGetCreationDate(alias: String?): Date? = null

    override fun engineSetKeyEntry(alias: String?, key: Key?, password: CharArray?, chain: Array<Certificate>?) = Unit

    override fun engineSetKeyEntry(alias: String?, key: ByteArray?, chain: Array<Certificate>?) = Unit

    override fun engineSetCertificateEntry(alias: String?, cert: Certificate?) = Unit

    override fun engineDeleteEntry(alias: String?) = Unit

    override fun engineAliases(): Enumeration<String> = Collections.emptyEnumeration()

    override fun engineContainsAlias(alias: String?): Boolean = false

    override fun engineSize(): Int = 0

    override fun engineIsKeyEntry(alias: String?): Boolean = false

    override fun engineIsCertificateEntry(alias: String?): Boolean = false

    override fun engineGetCertificateAlias(cert: Certificate?): String? = null

    override fun engineStore(stream: OutputStream?, password: CharArray?) = Unit

    override fun engineLoad(stream: InputStream?, password: CharArray?) = Unit
}
