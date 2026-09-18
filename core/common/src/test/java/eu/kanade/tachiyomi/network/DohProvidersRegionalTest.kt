package eu.kanade.tachiyomi.network

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

internal class DohProvidersRegionalTest {
    @Test
    fun regionalProvidersUseEndpoints() {
        val expected = mapOf<String, OkHttpClient.Builder.() -> OkHttpClient.Builder>(
            "dns.alidns.com" to { dohAliDNS() },
            "doh.pub" to { dohDNSPod() },
            "doh.360.cn" to { doh360() },
            "dns.twnic.tw" to { dohQuad101() },
            "dns.njal.la" to { dohNajalla() },
            "free.shecan.ir" to { dohShecan() },
        )
        expected.forEach { (host, configure) ->
            withClue(host) { dohHost(configure) shouldBe host }
        }
    }
}
