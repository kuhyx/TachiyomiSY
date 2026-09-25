package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.FakeDelegateSource
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.cannedResponse
import eu.kanade.tachiyomi.source.online.invokeDeclared
import eu.kanade.tachiyomi.source.online.rawTag
import eu.kanade.tachiyomi.source.online.sManga
import eu.kanade.tachiyomi.source.online.serveMetadataSource
import exh.metadata.metadata.LanraragiSearchMetadata
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

internal const val ARC_ID: String = "0123456789abcdef0123456789abcdef01234567"
private const val ARCHIVE = """{"arcid":"$ARC_ID","isnew":"false","tags":"date_added:1600000000, timestamp:soon, """ +
    """artist:foo, plain, series:x:y","summary":"Sum","title":"Title","pagecount":3,"filename":"f.zip",""" +
    """"extension":"zip"}"""

@RunWith(RobolectricTestRunner::class)
internal class LanraragiTest {
    private val harness = SourceTestHarness()
    private lateinit var source: Lanraragi

    @Before
    fun setUp() {
        harness.install()
        harness.serveMetadataSource()
        source = Lanraragi(FakeDelegateSource(harness.baseUrl, lang = "all", name = "LANraragi"), harness.application)
    }

    @After
    fun tearDown() = harness.uninstall()

    @Test
    fun identity() {
        source.lang shouldBe "all"
        source.metaClass shouldBe LanraragiSearchMetadata::class
        source.newMetaInstance().javaClass shouldBe LanraragiSearchMetadata::class.java
    }

    @Test
    fun parseArchiveMetadata() {
        val meta = LanraragiSearchMetadata()
        runBlocking { source.parseIntoMetadata(meta, cannedResponse(ARCHIVE)) }
        meta.arcId shouldBe ARC_ID
        meta.title shouldBe "Title"
        meta.summary shouldBe "Sum"
        meta.pageCount shouldBe 3
        meta.filename shouldBe "f.zip"
        meta.extension shouldBe "zip"
        meta.baseUrl shouldBe harness.baseUrl
        meta.tags shouldContainExactly listOf(
            rawTag("date_added", "2020-09-13 12:26", LanraragiSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("timestamp", "soon", LanraragiSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("artist", "foo", LanraragiSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("other", "plain", LanraragiSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("series", "x:y", LanraragiSearchMetadata.TAG_TYPE_DEFAULT),
        )
        val bare = LanraragiSearchMetadata()
        val json = ARCHIVE.replace(Regex(""""tags":"[^"]*""""), """"tags":null""")
            .replace(""""summary":"Sum"""", """"summary":null""")
        runBlocking { source.parseIntoMetadata(bare, cannedResponse(json)) }
        bare.tags.isEmpty() shouldBe true
        bare.summary.shouldBeNull()
    }

    @Test
    fun rxDetailsGoThroughDelegate() {
        harness.enqueue(ARCHIVE)
        val details = source.invokeDeclared(Lanraragi::class, "fetchMangaDetails", listOf(sManga("/reader?id=$ARC_ID")))
        ((details as rx.Observable<*>).toBlocking().first() as SManga).title shouldBe "Title"
        harness.takeRequest().target shouldBe "/reader?id=$ARC_ID"
    }

    @Test
    fun previewsForReaderUrl() {
        harness.enqueue(ARCHIVE)
        val page = runBlocking { source.getPagePreviewList(sManga("/reader?id=$ARC_ID"), emptyList(), 1) }
        harness.takeRequest().target shouldBe "/api/archives/$ARC_ID/metadata"
        page.pagePreviews.map { it.index } shouldContainExactly listOf(1, 2, 3)
        page.pagePreviews[1].imageUrl shouldBe
            "${harness.baseUrl}/api/archives/$ARC_ID/thumbnail?page=2&no_fallback=true"
        page.hasNextPage shouldBe false
        page.pagePreviewPages shouldBe 1
    }

    @Test
    fun previewsForRandomUrl() {
        harness.enqueue("""{"data":[{"arcid":"$ARC_ID"}]}""")
        harness.enqueue(ARCHIVE)
        val page = runBlocking { source.getPagePreviewList(sManga("/api/search/random?filter=x"), emptyList(), 1) }
        harness.takeRequest().target shouldBe "/api/search/random?count=1&filter=x"
        harness.takeRequest().target shouldBe "/api/archives/$ARC_ID/metadata"
        page.pagePreviews.map { it.index } shouldContainExactly listOf(1, 2, 3)
    }

    @Test
    fun previewsFromSavedMetadata() {
        val saved = LanraragiSearchMetadata().apply {
            mangaId = 5L
            arcId = ARC_ID
        }.flatten()
        harness.serveMetadataSource(mangaId = 5L, saved = saved)
        val page = runBlocking { source.getPagePreviewList(sManga("/reader?id=$ARC_ID"), emptyList(), 1) }
        harness.server.requestCount shouldBe 0
        page.pagePreviews.map { it.index } shouldContainExactly listOf(1)
    }

    @Test
    fun randomIdFallbacks() {
        harness.enqueue("""{"data":[{"id":"legacy"}]}""")
        harness.enqueue(ARCHIVE)
        runBlocking { source.getPagePreviewList(sManga("/api/search/random?a=b"), emptyList(), 1) }
        harness.takeRequest()
        harness.takeRequest().target shouldBe "/api/archives/legacy/metadata"
        harness.enqueue("""{"data":[]}""")
        harness.enqueue(ARCHIVE)
        runBlocking { source.getPagePreviewList(sManga("/api/search/random?a=b"), emptyList(), 1) }
        harness.takeRequest()
        harness.takeRequest().target shouldBe "/api/archives//metadata"
        harness.enqueue(ARCHIVE)
        runBlocking { source.getPagePreviewList(sManga("/reader?id=short"), emptyList(), 1) }
        harness.takeRequest().target shouldBe "/api/archives//metadata"
    }

    @Test
    fun minionJobState() {
        harness.enqueue("""{"state":"finished"}""")
        runBlocking { source.minionJobDone(7) } shouldBe true
        harness.takeRequest().target shouldBe "/api/minion/7"
        harness.enqueue("""{"state":"active"}""")
        runBlocking { source.minionJobDone(7) } shouldBe false
        Lanraragi.ThumbnailTask(job = 1, operation = "thumbnail", success = 1).copy(job = 2).job shouldBe 2
        val archive = Lanraragi.Archive(
            arcid = ARC_ID,
            isnew = "true",
            tags = null,
            summary = null,
            title = "t",
            pagecount = 1,
            filename = "f",
            extension = "zip",
        )
        archive.copy(isnew = "false").isnew shouldBe "false"
        archive.hashCode() shouldBe archive.copy().hashCode()
        archive.toString().contains("arcid") shouldBe true
        Lanraragi.TaskProgress("x").copy(state = "y").state shouldBe "y"
    }
}
