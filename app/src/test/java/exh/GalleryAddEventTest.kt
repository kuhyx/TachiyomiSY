package exh

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class GalleryAddEventTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val manga = Manga.create().copy(ogTitle = "A title")

    @Test
    fun successCarriesTitleAndChapter() {
        val chapter = Chapter.create()
        val event = GalleryAddEvent.Success("u", manga, context, chapter)
        event.galleryTitle shouldBe "A title"
        event.logMessage shouldContain "A title"
        event.chapter shouldBeSameInstanceAs chapter
        GalleryAddEvent.Success("u", manga, context).chapter.shouldBeNull()
    }

    @Test
    fun failuresMentionTheUrl() {
        GalleryAddEvent.Fail.UnknownType("u1", context).logMessage shouldContain "u1"
        GalleryAddEvent.Fail.UnknownSource("u2", context).logMessage shouldContain "u2"
        GalleryAddEvent.Fail.NotFound("u3", context).logMessage shouldContain "u3"
        val error = GalleryAddEvent.Fail.Error("u4", "msg")
        error.logMessage shouldBe "msg"
        error.galleryUrl shouldBe "u4"
        error.galleryTitle.shouldBeNull()
    }
}
