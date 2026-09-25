package exh.ui.metadata.adapters

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.R
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.MangaDexSearchMetadata
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EHentaiDescriptionTest {
    @get:Rule
    val compose = createComposeRule()

    private val host = DescriptionHost(compose)
    private val searches = mutableListOf<String>()

    private fun show(meta: EHentaiSearchMetadata?) = host.show { open ->
        EHentaiDescription(state = successState(meta), openMetadataViewer = open, search = { searches += it })
    }

    @Test
    fun everyFieldIsBound() {
        show(
            EHentaiSearchMetadata().apply {
                genre = "doujinshi"
                visible = "Yes"
                favorites = 1234
                uploader = "someone"
                size = 2048
                length = 3
                language = "english"
                translated = true
                averageRating = 4.5
            },
        )
        host.text(R.id.genre) shouldBe "Doujinshi"
        host.text(R.id.visible) shouldBe "Visible: Yes"
        host.text(R.id.uploader) shouldBe "someone"
        host.text(R.id.pages) shouldBe "3 pages"
        host.text(R.id.language) shouldBe "english TR"
        host.text(R.id.rating) shouldBe "4.5 - Amazing"
        host.pressAll(R.id.favorites, R.id.genre, R.id.language, R.id.pages, R.id.rating, R.id.visible)
        host.view(R.id.uploader).performClick()
        host.viewerOpened shouldBe 1
        searches shouldContainExactly listOf("uploader:\"someone\"")
    }

    @Test
    fun missingFieldsFallBack() {
        show(EHentaiSearchMetadata())
        host.text(R.id.genre) shouldBe "Unknown"
        host.text(R.id.visible) shouldBe "Visible: Unknown"
        host.text(R.id.uploader) shouldBe "Unknown"
        host.text(R.id.language) shouldBe "Unknown"
        host.text(R.id.rating) shouldBe "0.0 - No rating"
        host.view(R.id.uploader).performClick()
        searches shouldContainExactly emptyList()
    }

    @Test
    fun anUnknownGenreStaysRaw() {
        show(EHentaiSearchMetadata().apply { genre = "private" })
        host.text(R.id.genre) shouldBe "private"
    }

    @Test
    fun otherMetadataIsIgnored() {
        host.show { open ->
            EHentaiDescription(successState(MangaDexSearchMetadata()), openMetadataViewer = open, search = {})
        }
        host.text(R.id.visible) shouldBe ""
    }

    @Test
    fun noMetadataIsIgnored() {
        show(null)
        host.text(R.id.visible) shouldBe ""
    }
}
