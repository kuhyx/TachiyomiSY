package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.source.online.fixture
import eu.kanade.tachiyomi.source.online.jsoup
import eu.kanade.tachiyomi.source.online.rawTag
import exh.metadata.metadata.EHentaiSearchMetadata
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

private const val PACKAGE = "eu/kanade/tachiyomi/source/online/all"

@RunWith(RobolectricTestRunner::class)
internal class EHentaiGalleryListParserTest {
    private val parser = EHentaiGalleryListParser()
    private val thumbHtml = fixture("$PACKAGE/eh_list_thumb_layout.html")
    private val extendedHtml = fixture("$PACKAGE/eh_list_extended_layout.html")

    @Test
    fun thumbLayoutFullRow() {
        val (mangas, next) = parser.parse(jsoup(thumbHtml, "https://e-hentai.org/"))
        mangas.size shouldBe 3
        next shouldBe 7L
        val first = mangas[0]
        first.fav shouldBe 1
        first.manga.title shouldBe "Title A"
        first.manga.url shouldBe "/g/123/abc/?nw=always"
        first.manga.thumbnail_url shouldBe "https://ehgt.org/a.jpg"
        first.manga.genre shouldBe "female: tag one, female: light, female: weak, misc: solo"
        first.metadata.tags shouldContainExactly listOf(
            rawTag("female", "tag one", EHentaiSearchMetadata.TAG_TYPE_NORMAL),
            rawTag("female", "light", EHentaiSearchMetadata.TAG_TYPE_LIGHT),
            rawTag("female", "weak", EHentaiSearchMetadata.TAG_TYPE_WEAK),
            rawTag("misc", "solo", EHentaiSearchMetadata.TAG_TYPE_NORMAL),
        )
        first.metadata.genre shouldBe "doujinshi"
        first.metadata.datePosted shouldBe 1_672_628_640_000L
        first.metadata.averageRating shouldBe 4.0
        first.metadata.uploader shouldBe "someone"
        first.metadata.length shouldBe 24
    }

    @Test
    fun thumbLayoutSparseRows() {
        val (mangas, _) = parser.parse(jsoup(thumbHtml, "https://e-hentai.org/"))
        val half = mangas[1]
        half.fav shouldBe -1
        half.metadata.tags.isEmpty() shouldBe true
        half.metadata.genre shouldBe "artistcg"
        half.metadata.datePosted.shouldBeNull()
        half.metadata.averageRating shouldBe 3.5
        half.metadata.uploader.shouldBeNull()
        half.metadata.length.shouldBeNull()
        val bare = mangas[2]
        bare.metadata.genre.shouldBeNull()
        bare.metadata.averageRating.shouldBeNull()
        bare.metadata.uploader.shouldBeNull()
        bare.metadata.length.shouldBeNull()
        bare.manga.genre shouldBe ""
    }

    @Test
    fun extendedLayoutRows() {
        val (mangas, next) = parser.parse(jsoup(extendedHtml, "https://e-hentai.org/?f_search=x"))
        mangas.size shouldBe 3
        next.shouldBeNull()
        val first = mangas[0]
        first.fav shouldBe 3
        first.manga.title shouldBe "Title B"
        first.manga.url shouldBe "/g/456/def/?nw=always"
        first.metadata.tags shouldContainExactly listOf(
            rawTag("artist", "foo", EHentaiSearchMetadata.TAG_TYPE_NORMAL),
            rawTag("misc", "bare", EHentaiSearchMetadata.TAG_TYPE_NORMAL),
        )
        first.metadata.genre shouldBe "manga"
        first.metadata.datePosted shouldBe 1_672_531_140_000L
        first.metadata.averageRating shouldBe 2.0
        first.metadata.uploader shouldBe "up"
        first.metadata.length shouldBe 12
        val second = mangas[1]
        second.fav shouldBe -1
        second.metadata.genre shouldBe "imageset"
        second.metadata.datePosted.shouldBeNull()
        second.metadata.averageRating.shouldBeNull()
        second.metadata.uploader shouldBe "u2"
        second.metadata.length shouldBe 3
        val third = mangas[2]
        third.metadata.genre.shouldBeNull()
        third.metadata.uploader.shouldBeNull()
        third.metadata.length.shouldBeNull()
    }

    @Test
    fun reversedUsesPrevAndFirst() {
        val (mangas, next) = parser.parse(jsoup(extendedHtml, "https://e-hentai.org/?TEH_REVERSE=on"))
        mangas.map { it.manga.title } shouldContainExactly listOf("Title D", "Title C", "Title B")
        next shouldBe 456L
    }

    @Test
    fun reversedWithoutPrevLink() {
        val (mangas, next) = parser.parse(jsoup(thumbHtml, "https://e-hentai.org/?TEH_REVERSE=on"))
        mangas.map { it.manga.title } shouldContainExactly listOf("Title Bare", "Title Half", "Title A")
        next.shouldBeNull()
    }

    @Test
    fun toplistPagesByNumber() {
        parser.parse(jsoup(thumbHtml, "https://e-hentai.org/toplist.php?tl=11&p=3")).second shouldBe 5L
        parser.parse(jsoup(thumbHtml, "https://e-hentai.org/toplist.php?tl=11")).second shouldBe 2L
        parser.parse(jsoup(thumbHtml, "https://e-hentai.org/toplist.php?tl=11&p=199")).second.shouldBeNull()
    }

    @Test
    fun emptyListingWithWarning() {
        val html = """<table class="itg"><tbody></tbody></table><p class="searchwarn">Nothing here</p>"""
        shouldThrow<IOException> { parser.parse(jsoup(html, "https://e-hentai.org/")) }.message shouldBe "Nothing here"
    }

    @Test
    fun emptyListingWithoutWarning() {
        val html = """<table class="itg"><tbody><tr><th>only header</th></tr></tbody></table>"""
        val (mangas, next) = parser.parse(jsoup(html, "https://e-hentai.org/"))
        mangas.isEmpty() shouldBe true
        next.shouldBeNull()
    }

    @Test
    fun locationlessDocument() {
        val (mangas, next) = parser.parse(jsoup(thumbHtml, ""))
        mangas.size shouldBe 3
        next shouldBe 7L
    }
}
