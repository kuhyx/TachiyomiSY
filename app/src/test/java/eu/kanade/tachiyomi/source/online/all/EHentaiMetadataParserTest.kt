package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.fixture
import eu.kanade.tachiyomi.source.online.jsoup
import eu.kanade.tachiyomi.source.online.rawTag
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val PACKAGE = "eu/kanade/tachiyomi/source/online/all"

@RunWith(RobolectricTestRunner::class)
internal class EHentaiMetadataParserTest {
    private val harness = SourceTestHarness()

    @Before
    fun setUp() = harness.install()

    @After
    fun tearDown() = harness.uninstall()

    private fun parse(html: String, exh: Boolean = false, url: String = "https://e-hentai.org/g/123/abc/") =
        EHentaiSearchMetadata().also { EHentaiMetadataParser(exh).parseInto(it, jsoup(html, url)) }

    @Test
    fun fullGalleryFields() {
        val meta = parse(fixture("$PACKAGE/eh_gallery_full.html"), exh = true)
        meta.gId shouldBe "123"
        meta.gToken shouldBe "abc"
        meta.exh shouldBe true
        meta.title shouldBe "Main Title"
        meta.altTitle shouldBe "Alt Title"
        meta.thumbnailUrl shouldBe "https://ehgt.org/cover.jpg"
        meta.genre shouldBe "doujinshi"
        meta.uploader shouldBe "Uploader Name"
        meta.datePosted shouldBe 1_420_167_840_000L
        meta.parent shouldBe "https://e-hentai.org/g/100/parent/"
        meta.visible shouldBe "No (Replaced)"
        meta.language shouldBe "Japanese"
        meta.translated shouldBe true
        meta.size shouldBe 12_500_000L
        meta.length shouldBe 24
        meta.favorites shouldBe 5
        meta.averageRating shouldBe 4.53
        meta.ratingCount shouldBe 12
        meta.aged shouldBe true
        meta.lastUpdateCheck shouldBe meta.lastUpdateCheck.coerceAtLeast(1L)
    }

    @Test
    fun fullGalleryTags() {
        val meta = parse(fixture("$PACKAGE/eh_gallery_full.html"))
        meta.exh shouldBe false
        meta.tags shouldContainExactly listOf(
            rawTag("artist", "someone", EHentaiSearchMetadata.TAG_TYPE_NORMAL),
            rawTag("artist", "light", EHentaiSearchMetadata.TAG_TYPE_LIGHT),
            rawTag("artist", "weak", EHentaiSearchMetadata.TAG_TYPE_WEAK),
            rawTag("genre", "doujinshi", RaisedSearchMetadata.TAG_TYPE_VIRTUAL),
            rawTag("meta", "aged", RaisedSearchMetadata.TAG_TYPE_VIRTUAL),
            rawTag("uploader", "Uploader Name", RaisedSearchMetadata.TAG_TYPE_VIRTUAL),
            rawTag("visibility", "Replaced", RaisedSearchMetadata.TAG_TYPE_VIRTUAL),
        )
    }

    @Test
    fun sparseGalleryFields() {
        val meta = parse(fixture("$PACKAGE/eh_gallery_sparse.html"), url = "https://exhentai.org/g/9/tok/")
        meta.gId shouldBe "9"
        meta.gToken shouldBe "tok"
        meta.title.shouldBeNull()
        meta.altTitle.shouldBeNull()
        meta.thumbnailUrl.shouldBeNull()
        meta.genre.shouldBeNull()
        meta.uploader.shouldBeNull()
        meta.datePosted.shouldBeNull()
        meta.parent.shouldBeNull()
        meta.visible shouldBe "Yes"
        meta.language shouldBe "English"
        meta.translated shouldBe false
        meta.size.shouldBeNull()
        meta.length.shouldBeNull()
        meta.favorites.shouldBeNull()
        meta.averageRating.shouldBeNull()
        meta.ratingCount.shouldBeNull()
        meta.aged shouldBe false
        meta.tags shouldContainExactly listOf(rawTag("visibility", "Yes", RaisedSearchMetadata.TAG_TYPE_VIRTUAL))
    }

    @Test
    fun recentGalleryIsNotAged() {
        val recent = fixture("$PACKAGE/eh_gallery_full.html").replace("2015-01-02 03:04", "2999-01-02 03:04")
        val meta = parse(recent)
        meta.aged shouldBe false
        meta.tags.none { it.name == "aged" } shouldBe true
    }

    @Test
    fun emptyDocument() {
        val meta = parse("<html><body></body></html>")
        meta.tags.isEmpty() shouldBe true
        meta.visible.shouldBeNull()
        meta.datePosted.shouldBeNull()
    }
}
