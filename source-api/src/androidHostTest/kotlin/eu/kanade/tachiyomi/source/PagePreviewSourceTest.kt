package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.StubSource
import eu.kanade.tachiyomi.source.online.cannedResponse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.Response
import org.junit.jupiter.api.Test

/** A [PagePreviewSource] that records the cache control it was handed. */
private class PreviewStubSource : StubSource(), PagePreviewSource {
    val received: MutableList<CacheControl?> = mutableListOf()

    override suspend fun getPagePreviewList(manga: SManga, chapters: List<SChapter>, page: Int): PagePreviewPage =
        PagePreviewPage(page = page, pagePreviews = emptyList(), hasNextPage = false, pagePreviewPages = chapters.size)

    override suspend fun fetchPreviewImage(page: PagePreviewInfo, cacheControl: CacheControl?): Response {
        received += cacheControl
        return cannedResponse(page.imageUrl)
    }
}

/** The preview models of [PagePreviewSource] and its default cache-control argument. */
internal class PagePreviewSourceTest {
    private val json = Json
    private val info = PagePreviewInfo(index = 3, imageUrl = "https://img.example/3.png")
    private val page = PagePreviewPage(page = 1, pagePreviews = listOf(info), hasNextPage = true, pagePreviewPages = 4)
    private val source = PreviewStubSource()

    @Test
    fun previewPageIsAValueObject() {
        val same = PagePreviewPage(page = 1, pagePreviews = listOf(info), hasNextPage = true, pagePreviewPages = 4)
        page shouldBe same
        page.hashCode() shouldBe same.hashCode()
        page.copy(hasNextPage = false) shouldNotBe page
        page.copy(pagePreviewPages = null).pagePreviewPages shouldBe null
        page.toString() shouldBe "PagePreviewPage(page=1, pagePreviews=[$info], hasNextPage=true, pagePreviewPages=4)"
        page.component1() shouldBe 1
        page.component2() shouldBe listOf(info)
        page.component3() shouldBe true
        page.component4() shouldBe 4
    }

    @Test
    fun previewPageRoundTripsJson() {
        val encoded = json.encodeToString(PagePreviewPage.serializer(), page)
        val decoded = json.decodeFromString(PagePreviewPage.serializer(), encoded)
        decoded.page shouldBe 1
        decoded.hasNextPage shouldBe true
        decoded.pagePreviewPages shouldBe 4
        decoded.pagePreviews.single().imageUrl shouldBe "https://img.example/3.png"
    }

    @Test
    fun previewPageDecodesNullTotal() {
        val decoded = json.decodeFromString(
            PagePreviewPage.serializer(),
            """{"page":2,"pagePreviews":[],"hasNextPage":false,"pagePreviewPages":null}""",
        )
        decoded shouldBe PagePreviewPage(
            page = 2,
            pagePreviews = emptyList(),
            hasNextPage = false,
            pagePreviewPages = null,
        )
    }

    @Test
    fun previewInfoIsAValueObject() {
        val flow = MutableStateFlow(-1)
        val a = PagePreviewInfo(index = 1, imageUrl = "u", progressState = flow)
        val b = PagePreviewInfo(index = 1, imageUrl = "u", progressState = flow)
        a shouldBe b
        a.hashCode() shouldBe b.hashCode()
        a.copy(index = 2) shouldNotBe a
        a.copy(imageUrl = "v").imageUrl shouldBe "v"
        a.toString() shouldBe "PagePreviewInfo(index=1, imageUrl=u, progressState=$flow)"
        val (index, url) = a
        index shouldBe 1
        url shouldBe "u"
    }

    @Test
    fun previewInfoJsonSkipsProgress() {
        val encoded = json.encodeToString(PagePreviewInfo.serializer(), info)
        encoded shouldBe """{"index":3,"imageUrl":"https://img.example/3.png"}"""
        val decoded = json.decodeFromString(PagePreviewInfo.serializer(), encoded)
        decoded.index shouldBe 3
        decoded.imageUrl shouldBe info.imageUrl
        decoded.progress.value shouldBe -1
    }

    @Test
    fun progressStartsUnknown() {
        info.progress.value shouldBe -1
    }

    @Test
    fun updateReportsPercent() {
        info.update(bytesRead = 25L, contentLength = 200L, done = false)
        info.progress.value shouldBe 12
        info.update(bytesRead = 200L, contentLength = 200L, done = true)
        info.progress.value shouldBe 100
    }

    @Test
    fun updateReportsUnknownLength() {
        info.update(bytesRead = 25L, contentLength = 200L, done = false)
        info.update(bytesRead = 25L, contentLength = 0L, done = false)
        info.progress.value shouldBe -1
        info.update(bytesRead = 25L, contentLength = -1L, done = false)
        info.progress.value shouldBe -1
    }

    @Test
    fun previewImageDefaultsCache() = runTest {
        source.fetchPreviewImage(info).body.string() shouldBe info.imageUrl
        source.fetchPreviewImage(info, CacheControl.FORCE_NETWORK).body.string() shouldBe info.imageUrl
        source.received shouldBe listOf(null, CacheControl.FORCE_NETWORK)
    }

    @Test
    fun previewListIsImplemented() = runTest {
        val chapters = listOf(SChapter(name = "c", url = "/c"))
        val result = source.getPagePreviewList(SManga(url = "/m", title = "t"), chapters, 5)
        result.page shouldBe 5
        result.pagePreviewPages shouldBe 1
    }
}
