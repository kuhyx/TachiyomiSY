package exh.ui.metadata.adapters

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.R
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.PururinSearchMetadata
import exh.metadata.metadata.TsuminoSearchMetadata
import exh.metadata.metadata.base.RaisedTag
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PururinTsuminoDescriptionTest {
    @get:Rule
    val compose = createComposeRule()

    private val host = DescriptionHost(compose)

    @Test
    fun pururinFields() {
        val meta = PururinSearchMetadata().apply {
            tags += RaisedTag(namespace = PururinSearchMetadata.TAG_NAMESPACE_CATEGORY, name = "manga", type = 0)
            uploaderDisp = "Shown"
            fileSize = "3 MB"
            pages = 2
            averageRating = 3.0
        }
        host.show { PururinDescription(successState(meta), it) }
        host.text(R.id.genre) shouldBe "Manga"
        host.text(R.id.uploader) shouldBe "Shown"
        host.text(R.id.size) shouldBe "3 MB"
        host.text(R.id.pages) shouldBe "2 pages"
        host.text(R.id.rating) shouldBe "3.0 - Okay"
        host.pressAll(R.id.genre, R.id.pages, R.id.rating, R.id.size, R.id.uploader)
        host.viewerOpened shouldBe 1
    }

    @Test
    fun pururinFallbacks() {
        host.show { PururinDescription(successState(PururinSearchMetadata().apply { uploader = "raw" }), it) }
        host.text(R.id.genre) shouldBe "Unknown"
        host.text(R.id.uploader) shouldBe "raw"
        host.text(R.id.size) shouldBe "Unknown"
        host.text(R.id.rating) shouldBe "0.0 - No rating"
    }

    @Test
    fun pururinWithoutUploader() {
        host.show { PururinDescription(successState(PururinSearchMetadata()), it) }
        host.text(R.id.uploader) shouldBe ""
    }

    @Test
    fun pururinIgnoresOthers() {
        host.show { PururinDescription(successState(EHentaiSearchMetadata()), it) }
        host.text(R.id.uploader) shouldBe ""
    }

    @Test
    fun tsuminoFields() {
        val meta = TsuminoSearchMetadata().apply {
            category = "Doujinshi"
            favorites = 5L
            uploadDate = 0L
            uploader = "up"
            length = 7
            averageRating = 2.5F
        }
        host.show { TsuminoDescription(successState(meta), it) }
        host.text(R.id.genre) shouldBe "Doujinshi"
        host.text(R.id.favorites) shouldBe "5"
        host.text(R.id.uploader) shouldBe "up"
        host.text(R.id.pages) shouldBe "7 pages"
        host.text(R.id.rating) shouldBe "2.5 - Mediocre"
        host.pressAll(R.id.favorites, R.id.genre, R.id.pages, R.id.rating, R.id.uploader, R.id.when_posted)
        host.viewerOpened shouldBe 1
    }

    @Test
    fun tsuminoFallbacks() {
        host.show { TsuminoDescription(successState(TsuminoSearchMetadata()), it) }
        host.text(R.id.genre) shouldBe "Unknown"
        host.text(R.id.favorites) shouldBe "0"
        host.text(R.id.uploader) shouldBe "Unknown"
        host.text(R.id.pages) shouldBe "0 pages"
        host.text(R.id.when_posted) shouldBe TsuminoSearchMetadata.TSUMINO_DATE_FORMAT.format(java.util.Date(0))
    }

    @Test
    fun tsuminoIgnoresOthers() {
        host.show { TsuminoDescription(successState(EHentaiSearchMetadata()), it) }
        host.text(R.id.uploader) shouldBe ""
    }
}
