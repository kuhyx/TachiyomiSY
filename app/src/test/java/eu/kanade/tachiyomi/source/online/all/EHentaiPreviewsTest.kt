package eu.kanade.tachiyomi.source.online.all

import android.graphics.BitmapFactory
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.cannedResponse
import eu.kanade.tachiyomi.source.online.jsoup
import eu.kanade.tachiyomi.source.online.sChapter
import eu.kanade.tachiyomi.source.online.sManga
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO

private const val SHEET = "https://ehgt.org/m/sheet.jpg"
private const val SHEET_ENCODED = "https%3A%2F%2Fehgt.org%2Fm%2Fsheet.jpg"
private const val STYLE = "width:100px;height:140px;background:transparent url($SHEET) -200px 0 no-repeat"

/** A real PNG of [width] x [height] pixels. */
internal fun pngBytes(width: Int, height: Int): ByteArray = ByteArrayOutputStream().also {
    ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", it)
}.toByteArray()

@RunWith(RobolectricTestRunner::class)
internal class EHentaiPreviewsTest {
    private val harness = SourceTestHarness()
    private lateinit var source: EHentai

    @Before
    fun setUp() {
        harness.install()
        source = harness.ehentai()
    }

    @After
    fun tearDown() = harness.uninstall()

    @Test
    fun parsePreviewWithImage() {
        val element = jsoup("""<div style="$STYLE"><img alt="3"></div>""", "").selectFirst("div")!!
        parseNormalPreview(element) shouldBe
            EHentaiThumbnailPreview(SHEET, width = 100, height = 140, widthOffset = 200, index = 3)
    }

    @Test
    fun parsePreviewWithTitle() {
        val element = jsoup("""<a><div title="Page 7: x.jpg" style="$STYLE"></div></a>""", "").selectFirst("a")!!
        parseNormalPreview(element).index shouldBe 7
    }

    @Test
    fun thumbnailUrlRoundTrip() {
        val preview = EHentaiThumbnailPreview(SHEET, width = 100, height = 140, widthOffset = 200, index = 3)
        val url = preview.toUrl()
        url shouldBe "https://ehgt.org/g/blank.gif?imageUrl=$SHEET_ENCODED&width=100&height=140&widthOffset=200"
        EHentaiThumbnailPreview.parseFromUrl(url.toHttpUrl()) shouldBe preview.copy(index = -1)
    }

    @Test
    fun interceptorPassesOtherHosts() {
        val request = Request.Builder().url("https://e-hentai.org/g/1/a/").build()
        val response = cannedResponse("x", request.url.toString())
        val chain = mockk<Interceptor.Chain> {
            every { request() } returns request
            every { proceed(request) } returns response
        }
        ThumbnailPreviewInterceptor().intercept(chain) shouldBe response
        val other = Request.Builder().url("https://ehgt.org/g/other.gif").build()
        every { chain.request() } returns other
        every { chain.proceed(other) } returns response
        ThumbnailPreviewInterceptor().intercept(chain) shouldBe response
    }

    @Test
    fun interceptorCropsSheet() {
        val preview = EHentaiThumbnailPreview(SHEET, width = 10, height = 10, widthOffset = 5, index = 1)
        val request = Request.Builder().url(preview.toUrl()).build()
        val forwarded = slot<Request>()
        val chain = mockk<Interceptor.Chain> {
            every { request() } returns request
            every { proceed(capture(forwarded)) } answers {
                cannedResponse("", SHEET).newBuilder().body(pngBytes(20, 12).toResponseBody()).build()
            }
        }
        val response = ThumbnailPreviewInterceptor().intercept(chain)
        forwarded.captured.url.toString() shouldBe SHEET
        response.body.contentType().toString() shouldBe "image/jpeg"
        response.body.bytes().isNotEmpty() shouldBe true
    }

    @Test
    fun interceptorKeepsFailure() {
        val preview = EHentaiThumbnailPreview(SHEET, width = 10, height = 10, widthOffset = 5, index = 1)
        val request = Request.Builder().url(preview.toUrl()).build()
        val failure = cannedResponse("", SHEET, code = 404)
        val chain = mockk<Interceptor.Chain> {
            every { request() } returns request
            every { proceed(any()) } returns failure
        }
        ThumbnailPreviewInterceptor().intercept(chain) shouldBe failure
        // Robolectric's legacy graphics decode anything, so the null-bitmap path is forced through a static mock.
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any()) } returns null
        val garbage = cannedResponse("not an image", SHEET)
        shouldThrow<IOException> { garbage.croppedTo(preview) }.message shouldBe "Null bitmap($preview)"
        unmockkStatic(BitmapFactory::class)
    }

    @Test
    fun pagePreviewListSpriteLayout() {
        harness.enqueue(
            """<div id="gdt"><div><div style="$STYLE"><img alt="1"></div></div>""" +
                """<a><div title="Page 2: b" style="$STYLE"></div></a></div>""" +
                """<table class="ptt"><tbody><tr><td><a href="#">1</a></td><td><a href="#">2</a></td>""" +
                """<td><a href="#">&gt;</a></td></tr></tbody></table>""",
        )
        val page = runBlocking { source.pagePreviewList(sManga("/g/1/a/?nw=always"), emptyList(), 2) }
        harness.takeRequest().target shouldBe "/g/1/a/?p=1"
        page.page shouldBe 2
        page.pagePreviews.map { it.index } shouldContainExactly listOf(1, 2)
        page.pagePreviews[0].imageUrl shouldBe
            "https://ehgt.org/g/blank.gif?imageUrl=$SHEET_ENCODED&width=100&height=140&widthOffset=200"
        page.hasNextPage shouldBe true
        page.pagePreviewPages shouldBe 2
    }

    @Test
    fun pagePreviewListImageLayout() {
        harness.enqueue(
            """<div id="gdt"><div><a><img alt="4" src="https://ehgt.org/t/4.jpg"></a></div></div>""" +
                """<table class="ptt"><tbody><tr><td class="ptdd">&lt;</td>""" +
                """<td class="ptdd">&gt;</td></tr></tbody></table>""",
        )
        val chapters = listOf(sChapter("/g/2/b/"))
        val page = runBlocking { source.pagePreviewList(sManga("/g/1/a/"), chapters, 1) }
        harness.takeRequest().target shouldBe "/g/2/b/?p=0"
        page.pagePreviews.single().index shouldBe 4
        page.pagePreviews.single().imageUrl shouldBe "https://ehgt.org/t/4.jpg"
        page.hasNextPage shouldBe false
        page.pagePreviewPages.shouldBeNull()
    }
}
