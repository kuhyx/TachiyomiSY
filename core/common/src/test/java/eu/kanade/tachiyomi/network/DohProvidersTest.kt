package eu.kanade.tachiyomi.network

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

internal class DohProvidersTest {
    @Test
    fun preferenceValuesAreDistinct() {
        val values = listOf(
            PREF_DOH_CLOUDFLARE, PREF_DOH_GOOGLE, PREF_DOH_ADGUARD, PREF_DOH_QUAD9, PREF_DOH_ALIDNS, PREF_DOH_DNSPOD,
            PREF_DOH_360, PREF_DOH_QUAD101, PREF_DOH_MULLVAD, PREF_DOH_CONTROLD, PREF_DOH_NJALLA, PREF_DOH_SHECAN,
        )
        values shouldBe (1..12).toList()
    }

    @Test
    fun globalProvidersUseEndpoints() {
        val expected = mapOf<String, OkHttpClient.Builder.() -> OkHttpClient.Builder>(
            "cloudflare-dns.com" to { dohCloudflare() },
            "dns.google" to { dohGoogle() },
            "dns-unfiltered.adguard.com" to { dohAdGuard() },
            "dns.quad9.net" to { dohQuad9() },
            "dns.mullvad.net" to { dohMullvad() },
            "freedns.controld.com" to { dohControlD() },
        )
        expected.forEach { (host, configure) ->
            withClue(host) { dohHost(configure) shouldBe host }
        }
    }

    @Test
    fun resolverKeepsTheBuilderClient() {
        // The resolver's own client is built from the builder as configured so far; the bootstrap
        // hosts replace only its DNS.
        val client = OkHttpClient.Builder().connectTimeout(1234L, TimeUnit.MILLISECONDS).dohCloudflare().build()
        val dns = client.dns.shouldBeInstanceOf<DnsOverHttps>()
        dns.url.encodedPath shouldBe "/dns-query"
        dns.client.connectTimeoutMillis shouldBe 1234
    }
}
