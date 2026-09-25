package eu.kanade.tachiyomi.source.online.all

import android.net.Uri
import eu.kanade.tachiyomi.source.PagePreviewInfo
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.online.FakeDelegateSource
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.cannedResponse
import eu.kanade.tachiyomi.source.online.fixture
import eu.kanade.tachiyomi.source.online.invokeDeclared
import eu.kanade.tachiyomi.source.online.rawTag
import eu.kanade.tachiyomi.source.online.sManga
import eu.kanade.tachiyomi.source.online.serveMetadataSource
import exh.metadata.metadata.NHentaiSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import okhttp3.CacheControl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val CONFIG = """{"image_servers":["https://i9.nhentai.net"],"thumb_servers":["https://t9.nhentai.net"]}"""

@RunWith(RobolectricTestRunner::class)
internal class NHentaiTest {
    private val harness = SourceTestHarness()
    private lateinit var source: NHentai

    @Before
    fun setUp() {
        harness.install()
        harness.serveMetadataSource()
        source = NHentai(FakeDelegateSource(harness.baseUrl, lang = "all", name = "NHentai"), harness.application)
    }

    @After
    fun tearDown() = harness.uninstall()

    private fun full() = fixture("eu/kanade/tachiyomi/source/online/all/nh_gallery_full.json")

    @Test
    fun identity() {
        source.lang shouldBe "all"
        source.metaClass shouldBe NHentaiSearchMetadata::class
        source.newMetaInstance().javaClass shouldBe NHentaiSearchMetadata::class.java
        source.matchingHosts shouldContainExactly listOf("nhentai.net")
        NHentai.otherId shouldBe 7_309_872_737_163_460_316L
        source.thumbServer.shouldBeNull()
    }

    @Test
    fun parseFullMetadata() {
        harness.enqueue(CONFIG)
        val meta = NHentaiSearchMetadata()
        runBlocking { source.parseIntoMetadata(meta, cannedResponse(full())) }
        harness.takeRequest().target shouldBe "/api/v2/config"
        meta.nhId shouldBe 177_013L
        meta.uploadDate shouldBe 1_471_743_100L
        meta.favoritesCount shouldBe 42L
        meta.mediaId shouldBe "987286"
        meta.japaneseTitle shouldBe "日本語"
        meta.shortTitle shouldBe "Pretty"
        meta.englishTitle shouldBe "English Title"
        meta.preferredTitle shouldBe NHentaiSearchMetadata.TITLE_TYPE_ENGLISH
        meta.coverImageUrl shouldBe "https://t9.nhentai.net/galleries/987286/cover.jpg"
        meta.pageImagePreviewUrls shouldContainExactly listOf("galleries/987286/1t.jpg")
        meta.scanlator shouldBe "Scan"
        meta.tags shouldContainExactly listOf(
            rawTag("tag", "solo", NHentaiSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("category", "doujinshi", RaisedSearchMetadata.TAG_TYPE_VIRTUAL),
        )
        source.thumbServer shouldBe "https://t9.nhentai.net"
    }

    @Test
    fun parseMinimalWithFallbackConfig() {
        harness.enqueue("", code = 500)
        val prefs = harness.application.getSharedPreferences("source_${source.id}", 0)
        prefs.edit().putString("Display manga title as:", "short").commit()
        val meta = NHentaiSearchMetadata()
        val minimal = fixture("eu/kanade/tachiyomi/source/online/all/nh_gallery_minimal.json")
        runBlocking { source.parseIntoMetadata(meta, cannedResponse(minimal)) }
        source.nhConfig?.imageServers shouldBe (1..4).map { "https://i$it.nhentai.net" }
        meta.nhId shouldBe 1L
        meta.uploadDate.shouldBeNull()
        meta.japaneseTitle.shouldBeNull()
        meta.preferredTitle shouldBe NHentaiSearchMetadata.TITLE_TYPE_SHORT
        meta.coverImageUrl?.endsWith("/galleries/1/thumb.jpg") shouldBe true
        meta.scanlator.shouldBeNull()
        meta.tags.isEmpty() shouldBe true
        runBlocking { source.parseIntoMetadata(meta, cannedResponse("""{"id":2}""")) }
        meta.coverImageUrl.shouldBeNull()
        harness.server.requestCount shouldBe 1
    }

    @Test
    fun rxDetailsGoThroughDelegate() {
        harness.enqueue(full())
        harness.enqueue(CONFIG)
        val details = source.invokeDeclared(NHentai::class, "fetchMangaDetails", listOf(sManga("/g/177013/")))
        val manga = (details as rx.Observable<*>).toBlocking().first() as eu.kanade.tachiyomi.source.model.SManga
        harness.takeRequest().target shouldBe "/g/177013/"
        harness.takeRequest().target shouldBe "/api/v2/config"
        manga.title shouldBe "English Title"
    }

    @Test
    fun searchDelegatesUnlessUrl() {
        runBlocking { source.getSearchManga(1, "query", FilterList()) }.mangas.single().url shouldBe "/search/1/query"
        val rx = source.invokeDeclared(NHentai::class, "fetchSearchManga", listOf(1, "q", FilterList()))
        ((rx as rx.Observable<*>).toBlocking().first() as MangasPage).mangas.single().url shouldBe "/search/1/q"
    }

    @Test
    fun mapUrlToMangaUrl() {
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://nhentai.net/G/123/")) } shouldBe
            "${harness.baseUrl}/g/123/"
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://nhentai.net/tag/x/")) }.shouldBeNull()
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://nhentai.net")) }.shouldBeNull()
    }

    @Test
    fun pagePreviewsFromMetadata() {
        harness.enqueue(CONFIG)
        harness.enqueue(full())
        val page = runBlocking { source.getPagePreviewList(sManga("/g/177013/"), emptyList(), 1) }
        page.page shouldBe 1
        page.hasNextPage shouldBe false
        page.pagePreviewPages shouldBe 1
        page.pagePreviews.single().index shouldBe 1
        page.pagePreviews.single().imageUrl shouldBe "https://t9.nhentai.net/galleries/987286/1t.jpg"
        harness.enqueue(full())
        runBlocking { source.getPagePreviewList(sManga("/g/177013/"), emptyList(), 2) }.page shouldBe 2
        harness.server.requestCount shouldBe 3
    }

    @Test
    fun previewImageCacheControl() {
        val preview = PagePreviewInfo(1, "https://t9.nhentai.net/galleries/1/1t.jpg")
        harness.enqueueBytes(byteArrayOf(1, 2), "image/jpeg")
        runBlocking { source.fetchPreviewImage(preview, CacheControl.FORCE_NETWORK) }.body.bytes().size shouldBe 2
        harness.takeRequest().headers["Cache-Control"] shouldBe "no-cache"
        harness.enqueueBytes(byteArrayOf(1), "image/jpeg")
        runBlocking { source.fetchPreviewImage(preview) }.body.bytes().size shouldBe 1
        harness.takeRequest().headers["Cache-Control"] shouldBe "max-age=600"
    }

    @Test
    fun jsonModelsRoundTrip() {
        val config = NHentai.JsonConfig(listOf("i"), listOf("t"))
        config.copy(thumbServers = emptyList()).thumbServers.isEmpty() shouldBe true
        NHentai.JsonConfig() shouldBe NHentai.JsonConfig(emptyList(), emptyList())
        val title = NHentai.JsonTitle(english = "e")
        title.copy(japanese = "j").japanese shouldBe "j"
        val pageJson = NHentai.JsonPage(path = "p", width = 1, height = 2, thumbnail = "t")
        pageJson.copy(path = null).path.shouldBeNull()
        NHentai.JsonPage(path = "p").thumbnail.shouldBeNull()
        val tag = NHentai.JsonTag(id = 1, type = "tag", name = "n", url = "u", count = 2)
        tag.copy(count = null).count.shouldBeNull()
        NHentai.JsonTag(id = 1).count.shouldBeNull()
        val response = NHentai.JsonResponse(id = 5, title = title, pages = listOf(pageJson), tags = listOf(tag))
        response.copy(numPages = 3).numPages shouldBe 3
        response.toString().isNotBlank() shouldBe true
    }
}
