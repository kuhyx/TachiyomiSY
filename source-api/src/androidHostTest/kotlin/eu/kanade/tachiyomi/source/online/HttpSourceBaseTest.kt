package eu.kanade.tachiyomi.source.online

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.security.MessageDigest

/** Identity members of [HttpSourceBase]: id generation, headers, naming and the delegate hook. */
internal class HttpSourceBaseTest {
    private val harness = SourceHarness()
    private val source = BareHttpSource()

    @BeforeEach
    fun setUp() = harness.install()

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun idIsMd5OfNameLangVersion() {
        source.id shouldBe expectedId("bare source/en/1")
        source.id shouldBe source.id
    }

    @Test
    fun generateIdClearsSignBit() {
        val id = source.exposedGenerateId("Other Name", "de", 3)
        id shouldBe expectedId("other name/de/3")
        (id >= 0L) shouldBe true
    }

    @Test
    fun generateIdDependsOnEveryInput() {
        val base = source.exposedGenerateId("A", "en", 1)
        source.exposedGenerateId("B", "en", 1) shouldBe expectedId("b/en/1")
        (source.exposedGenerateId("A", "fr", 1) == base) shouldBe false
        (source.exposedGenerateId("A", "en", 2) == base) shouldBe false
    }

    @Test
    fun versionIdDefaultsToOne() {
        source.versionId shouldBe 1
    }

    @Test
    fun headersCarryTrimmedUserAgent() {
        source.headers["User-Agent"] shouldBe USER_AGENT
        source.headers.size shouldBe 1
    }

    @Test
    fun headersBuilderSeedsUserAgent() {
        source.exposedHeadersBuilder().build()["User-Agent"] shouldBe USER_AGENT
    }

    @Test
    fun toStringUppercasesLang() {
        source.toString() shouldBe "Bare Source (EN)"
        BareHttpSource(name = "Other", lang = "pt-BR").toString() shouldBe "Other (PT-BR)"
    }

    @Test
    fun homeUrlDefaultsToBaseUrl() {
        source.getHomeUrl() shouldBe "https://bare.example"
    }

    @Test
    fun boundDelegateRoutesClient() {
        val delegate = StubDelegatedSource(delegate = source, baseHttpClient = harness.server.client)
        source.bindDelegate(delegate)
        (source.client === harness.server.client) shouldBe true
    }

    @Test
    fun boundDelegateIgnoredIfOff() {
        source.bindDelegate(StubDelegatedSource(delegate = source, baseHttpClient = harness.server.client))
        harness.delegateSources = false
        (source.client === harness.server.client) shouldBe false
        source.client.interceptors shouldBe harness.server.client.interceptors
    }

    private fun expectedId(key: String): Long {
        val digest = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        return ByteBuffer.wrap(digest).long and Long.MAX_VALUE
    }
}
