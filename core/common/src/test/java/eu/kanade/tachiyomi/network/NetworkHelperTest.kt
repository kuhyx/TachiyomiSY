package eu.kanade.tachiyomi.network

import android.app.Application
import android.webkit.CookieManager
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor
import eu.kanade.tachiyomi.network.interceptor.UserAgentInterceptor
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import okhttp3.Dns
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executor

internal class NetworkHelperTest {
    private val tempDir: File = Files.createTempDirectory("network-helper").toFile()
    private val context: Application = mockk {
        every { cacheDir } returns tempDir
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(CookieManager::class)
        every { CookieManager.getInstance() } returns mockk()
        mockkStatic(ContextCompat::class)
        every { ContextCompat.getMainExecutor(any()) } returns Executor { it.run() }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        tempDir.deleteRecursively()
    }

    @Test
    fun clientUsesConfiguredConstants() {
        val helper = helper()
        val client = helper.client
        client.connectTimeoutMillis shouldBe 30_000
        client.readTimeoutMillis shouldBe 30_000
        client.callTimeoutMillis shouldBe 120_000
        val cache = checkNotNull(client.cache)
        cache.maxSize() shouldBe 5L * 1024 * 1024
        cache.directory shouldBe File(tempDir, "network_cache")
        client.cookieJar shouldBeSameInstanceAs helper.cookieJar
        client.interceptors.map { it::class } shouldContainExactly listOf(
            UncaughtExceptionInterceptor::class,
            UserAgentInterceptor::class,
            CloudflareInterceptor::class,
        )
        client.networkInterceptors shouldBe emptyList()
        client.dns shouldBe Dns.SYSTEM
    }

    @Test
    fun debugBuildLogsHeaders() {
        val client = helper(debug = true).client
        val logger = client.networkInterceptors.single().shouldBeInstanceOf<HttpLoggingInterceptor>()
        logger.level shouldBe HttpLoggingInterceptor.Level.HEADERS
    }

    @Test
    fun dohPreferenceSelectsResolver() {
        val hosts = mapOf(
            PREF_DOH_CLOUDFLARE to "cloudflare-dns.com",
            PREF_DOH_GOOGLE to "dns.google",
            PREF_DOH_ADGUARD to "dns-unfiltered.adguard.com",
            PREF_DOH_QUAD9 to "dns.quad9.net",
            PREF_DOH_ALIDNS to "dns.alidns.com",
            PREF_DOH_DNSPOD to "doh.pub",
            PREF_DOH_360 to "doh.360.cn",
            PREF_DOH_QUAD101 to "dns.twnic.tw",
            PREF_DOH_MULLVAD to "dns.mullvad.net",
            PREF_DOH_CONTROLD to "freedns.controld.com",
            PREF_DOH_NJALLA to "dns.njal.la",
            PREF_DOH_SHECAN to "free.shecan.ir",
        )
        hosts.forEach { (provider, host) ->
            withClue(host) {
                val dns = helper(doh = provider).client.dns
                dns.shouldBeInstanceOf<DnsOverHttps>().url.host shouldBe host
            }
        }
    }

    @Test
    fun unknownDohKeepsSystemDns() {
        helper(doh = 99).client.dns shouldBe Dns.SYSTEM
    }

    @Test
    fun userAgentIsTrimmed() {
        helper(userAgent = "  agent/1  ").defaultUserAgentProvider() shouldBe "agent/1"
    }

    @Test
    fun cloudflareClientIsMainClient() {
        val helper = helper()
        // Deprecated member: read through Java reflection so no deprecation warning is compiled.
        val legacy = NetworkHelper::class.java.getMethod("getCloudflareClient").invoke(helper)
        legacy shouldBeSameInstanceAs helper.client
        helper.isDebugBuild shouldBe false
    }

    private fun helper(doh: Int = -1, debug: Boolean = false, userAgent: String = "agent/1"): NetworkHelper {
        val store = InMemoryPreferenceStore(
            sequenceOf(
                InMemoryPreferenceStore.InMemoryPreference("doh_provider", doh, -1),
                InMemoryPreferenceStore.InMemoryPreference("default_user_agent", userAgent, ""),
            ),
        )
        return NetworkHelper(context, NetworkPreferences(store), debug)
    }
}
