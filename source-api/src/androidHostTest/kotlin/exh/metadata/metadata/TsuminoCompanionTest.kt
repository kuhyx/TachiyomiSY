package exh.metadata.metadata

import android.net.Uri
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** The URL and date helpers in the [TsuminoSearchMetadata] companion. */
internal class TsuminoCompanionTest {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun stubLastSegment(segment: String?) {
        mockkStatic(Uri::class)
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns segment
        every { Uri.parse(any()) } returns uri
    }

    @Test
    fun tmIdFromUrlIsLastSegment() {
        stubLastSegment("123")
        TsuminoSearchMetadata.tmIdFromUrl("https://www.tsumino.com/entry/123") shouldBe "123"
        verify { Uri.parse("https://www.tsumino.com/entry/123") }
    }

    @Test
    fun tmIdFromUrlIsNullWithoutPath() {
        stubLastSegment(null)
        TsuminoSearchMetadata.tmIdFromUrl("https://www.tsumino.com") shouldBe null
    }

    @Test
    fun thumbUrlFromIdIsFirstPage() {
        TsuminoSearchMetadata.thumbUrlFromId("123") shouldBe "/thumbs/123/1"
    }

    @Test
    fun dateFormatIsIsoDay() {
        val format = TsuminoSearchMetadata.TSUMINO_DATE_FORMAT
        format.toPattern() shouldBe "yyyy-MM-dd"
        format.format(checkNotNull(format.parse("2020-01-02"))) shouldBe "2020-01-02"
    }

    @Test
    fun baseUrlIsWwwHost() {
        TsuminoSearchMetadata.BASE_URL shouldBe "https://www.tsumino.com"
        TsuminoSearchMetadata.TAG_TYPE_DEFAULT shouldBe 0
    }
}
