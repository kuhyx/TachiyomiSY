package exh.ui.metadata.adapters

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.R
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.metadata.metadata.NHentaiSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.RaisedTag
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NHentaiDescriptionTest {
    @get:Rule
    val compose = createComposeRule()

    private val host = DescriptionHost(compose)

    private fun show(meta: RaisedSearchMetadata?) = host.show { open ->
        NHentaiDescription(state = successState(meta), openMetadataViewer = open)
    }

    private fun category(name: String) =
        RaisedTag(namespace = NHentaiSearchMetadata.NHENTAI_CATEGORIES_NAMESPACE, name = name, type = 0)

    @Test
    fun everyFieldIsBound() {
        show(
            NHentaiSearchMetadata().apply {
                tags += category("doujinshi")
                tags += RaisedTag(namespace = "artist", name = "a", type = 0)
                favoritesCount = 12L
                uploadDate = 0L
                pageImagePreviewUrls = listOf("p1", "p2")
                nhId = 99L
            },
        )
        host.text(R.id.genre) shouldBe "Doujinshi"
        host.text(R.id.favorites) shouldBe "12"
        host.text(R.id.pages) shouldBe "2 pages"
        host.text(R.id.id) shouldBe "#99"
        host.pressAll(R.id.favorites, R.id.genre, R.id.id, R.id.pages, R.id.when_posted)
        host.viewerOpened shouldBe 1
    }

    @Test
    fun unknownCategoriesStayRaw() {
        show(
            NHentaiSearchMetadata().apply {
                tags += category("one")
                tags += category("two")
                favoritesCount = 0L
            },
        )
        host.text(R.id.genre) shouldBe "one, two"
        host.text(R.id.id) shouldBe "#0"
        host.text(R.id.pages) shouldBe "0 pages"
    }

    @Test
    fun noCategoryIsUnknown() {
        show(NHentaiSearchMetadata())
        host.text(R.id.genre) shouldBe "Unknown"
    }

    @Test
    fun otherMetadataIsIgnored() {
        show(MangaDexSearchMetadata())
        host.text(R.id.id) shouldBe ""
    }

    @Test
    fun noMetadataIsIgnored() {
        show(null)
        host.text(R.id.id) shouldBe ""
    }
}
