package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.NetworkHelper
import io.kotest.matchers.shouldBe
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Client routing of [HttpSourceBase]: the SY network layer with and without a bound delegate. */
internal class HttpSourceClientTest {
    private val harness = SourceHarness()
    private val source = BareHttpSource()
    private val other = OkHttpClient()

    @BeforeEach
    fun setUp() = harness.install()

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun clientDerivesFromNetwork() {
        val client = source.client
        (client === harness.server.client) shouldBe false
        client.interceptors shouldBe harness.server.client.interceptors
        (source.client === client) shouldBe false
    }

    @Test
    fun networkClientRebuildsInjected() {
        val network = source.exposedNetwork()
        network.client.interceptors shouldBe harness.server.client.interceptors
        (network.client === harness.server.client) shouldBe false
    }

    @Test
    fun networkClientPrefersDelegate() {
        source.bindDelegate(StubDelegatedSource(delegate = source, networkClient = other))
        (source.exposedNetwork().client === other) shouldBe true
    }

    @Test
    fun networkIgnoresOffDelegate() {
        source.bindDelegate(StubDelegatedSource(delegate = source, networkClient = other))
        harness.delegateSources = false
        source.exposedNetwork().client.interceptors shouldBe harness.server.client.interceptors
    }

    @Test
    fun cloudflareClientUsesDelegate() {
        source.bindDelegate(StubDelegatedSource(delegate = source, networkClient = other))
        val network = source.exposedNetwork()
        (network.readMember(NetworkHelper::class, "cloudflareClient") === other) shouldBe true
    }

    @Test
    fun cloudflareClientFallsBack() {
        val network = source.exposedNetwork()
        val cloudflare = network.readMember(NetworkHelper::class, "cloudflareClient") as OkHttpClient
        cloudflare.interceptors shouldBe harness.server.client.interceptors
    }

    @Test
    fun cookieJarIsInjectedJar() {
        (source.exposedNetwork().cookieJar === harness.cookieJar) shouldBe true
    }

    @Test
    fun delegateWithoutBaseFallsBack() {
        source.bindDelegate(StubDelegatedSource(delegate = source))
        source.client.interceptors shouldBe harness.server.client.interceptors
    }

    @Test
    fun delegateBaseClientWins() {
        source.bindDelegate(StubDelegatedSource(delegate = source, baseHttpClient = other))
        (source.client === other) shouldBe true
    }

    @Test
    fun networkIsBuiltOnce() {
        (source.exposedNetwork() === source.exposedNetwork()) shouldBe true
        source.exposedNetwork().isDebugBuild shouldBe false
    }
}
