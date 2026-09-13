package tachiyomi.core.metadata.comicinfo

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

/** Drives the generated equals/hashCode/toString of every element so the data classes are fully covered. */
internal class ComicInfoEqualityTest {
    private fun <T : Any> exercise(a: T, same: T, different: T) {
        a shouldBe a
        a shouldBe same
        a shouldNotBe different
        a.equals(Any()) shouldBe false
        a.hashCode() shouldBe same.hashCode()
        a.toString() shouldBe same.toString()
    }

    @Test
    fun elementsCompareByText() {
        exercise(ComicInfo.Title("a"), ComicInfo.Title("a"), ComicInfo.Title("b"))
        exercise(ComicInfo.Series("a"), ComicInfo.Series("a"), ComicInfo.Series("b"))
        exercise(ComicInfo.Number("a"), ComicInfo.Number("a"), ComicInfo.Number("b"))
        exercise(ComicInfo.Summary("a"), ComicInfo.Summary("a"), ComicInfo.Summary("b"))
        exercise(ComicInfo.Writer("a"), ComicInfo.Writer("a"), ComicInfo.Writer("b"))
        exercise(ComicInfo.Penciller("a"), ComicInfo.Penciller("a"), ComicInfo.Penciller("b"))
        exercise(ComicInfo.Inker("a"), ComicInfo.Inker("a"), ComicInfo.Inker("b"))
        exercise(ComicInfo.Colorist("a"), ComicInfo.Colorist("a"), ComicInfo.Colorist("b"))
        exercise(ComicInfo.Letterer("a"), ComicInfo.Letterer("a"), ComicInfo.Letterer("b"))
        exercise(ComicInfo.CoverArtist("a"), ComicInfo.CoverArtist("a"), ComicInfo.CoverArtist("b"))
        exercise(ComicInfo.Translator("a"), ComicInfo.Translator("a"), ComicInfo.Translator("b"))
        exercise(ComicInfo.Genre("a"), ComicInfo.Genre("a"), ComicInfo.Genre("b"))
        exercise(ComicInfo.Tags("a"), ComicInfo.Tags("a"), ComicInfo.Tags("b"))
        exercise(ComicInfo.Web("a"), ComicInfo.Web("a"), ComicInfo.Web("b"))
        exercise(
            ComicInfo.PublishingStatusTachiyomi("a"),
            ComicInfo.PublishingStatusTachiyomi("a"),
            ComicInfo.PublishingStatusTachiyomi("b"),
        )
        exercise(
            ComicInfo.CategoriesTachiyomi("a"),
            ComicInfo.CategoriesTachiyomi("a"),
            ComicInfo.CategoriesTachiyomi("b"),
        )
        exercise(ComicInfo.SourceMihon("a"), ComicInfo.SourceMihon("a"), ComicInfo.SourceMihon("b"))
        exercise(
            ComicInfo.PaddingTachiyomiSY("a"),
            ComicInfo.PaddingTachiyomiSY("a"),
            ComicInfo.PaddingTachiyomiSY("b"),
        )
    }

    @Test
    fun elementsCopyWithAndWithoutText() {
        ComicInfo.Title("a").copy(value = "b") shouldBe ComicInfo.Title("b")
        ComicInfo.Title("a").copy() shouldBe ComicInfo.Title("a")
        ComicInfo.Series("a").copy(value = "b") shouldBe ComicInfo.Series("b")
        ComicInfo.Series("a").copy() shouldBe ComicInfo.Series("a")
        ComicInfo.Number("a").copy(value = "b") shouldBe ComicInfo.Number("b")
        ComicInfo.Number("a").copy() shouldBe ComicInfo.Number("a")
        ComicInfo.Summary("a").copy(value = "b") shouldBe ComicInfo.Summary("b")
        ComicInfo.Summary("a").copy() shouldBe ComicInfo.Summary("a")
        ComicInfo.Writer("a").copy(value = "b") shouldBe ComicInfo.Writer("b")
        ComicInfo.Writer("a").copy() shouldBe ComicInfo.Writer("a")
        ComicInfo.Penciller("a").copy(value = "b") shouldBe ComicInfo.Penciller("b")
        ComicInfo.Penciller("a").copy() shouldBe ComicInfo.Penciller("a")
        ComicInfo.Inker("a").copy(value = "b") shouldBe ComicInfo.Inker("b")
        ComicInfo.Inker("a").copy() shouldBe ComicInfo.Inker("a")
        ComicInfo.Colorist("a").copy(value = "b") shouldBe ComicInfo.Colorist("b")
        ComicInfo.Colorist("a").copy() shouldBe ComicInfo.Colorist("a")
        ComicInfo.Letterer("a").copy(value = "b") shouldBe ComicInfo.Letterer("b")
        ComicInfo.Letterer("a").copy() shouldBe ComicInfo.Letterer("a")
        ComicInfo.CoverArtist("a").copy(value = "b") shouldBe ComicInfo.CoverArtist("b")
        ComicInfo.CoverArtist("a").copy() shouldBe ComicInfo.CoverArtist("a")
        ComicInfo.Translator("a").copy(value = "b") shouldBe ComicInfo.Translator("b")
        ComicInfo.Translator("a").copy() shouldBe ComicInfo.Translator("a")
        ComicInfo.Genre("a").copy(value = "b") shouldBe ComicInfo.Genre("b")
        ComicInfo.Genre("a").copy() shouldBe ComicInfo.Genre("a")
        ComicInfo.Tags("a").copy(value = "b") shouldBe ComicInfo.Tags("b")
        ComicInfo.Tags("a").copy() shouldBe ComicInfo.Tags("a")
        ComicInfo.Web("a").copy(value = "b") shouldBe ComicInfo.Web("b")
        ComicInfo.Web("a").copy() shouldBe ComicInfo.Web("a")
        ComicInfo.PublishingStatusTachiyomi("a").copy(value = "b") shouldBe ComicInfo.PublishingStatusTachiyomi("b")
        ComicInfo.PublishingStatusTachiyomi("a").copy() shouldBe ComicInfo.PublishingStatusTachiyomi("a")
        ComicInfo.CategoriesTachiyomi("a").copy(value = "b") shouldBe ComicInfo.CategoriesTachiyomi("b")
        ComicInfo.CategoriesTachiyomi("a").copy() shouldBe ComicInfo.CategoriesTachiyomi("a")
        ComicInfo.SourceMihon("a").copy(value = "b") shouldBe ComicInfo.SourceMihon("b")
        ComicInfo.SourceMihon("a").copy() shouldBe ComicInfo.SourceMihon("a")
        ComicInfo.PaddingTachiyomiSY("a").copy(value = "b") shouldBe ComicInfo.PaddingTachiyomiSY("b")
        ComicInfo.PaddingTachiyomiSY("a").copy() shouldBe ComicInfo.PaddingTachiyomiSY("a")
    }

    @Test
    fun documentDiffersOnEveryField() {
        val base = ComicInfo(
            title = null, series = null, number = null, summary = null, writer = null, penciller = null,
            inker = null, colorist = null, letterer = null, coverArtist = null, translator = null,
            genre = null, tags = null, web = null, publishingStatus = null, categories = null,
            source = null, padding = null,
        )
        exercise(base, base.copy(), base.copy(title = ComicInfo.Title("x")))
        val variants = listOf(
            base.copy(series = ComicInfo.Series("x")),
            base.copy(number = ComicInfo.Number("x")),
            base.copy(summary = ComicInfo.Summary("x")),
            base.copy(writer = ComicInfo.Writer("x")),
            base.copy(penciller = ComicInfo.Penciller("x")),
            base.copy(inker = ComicInfo.Inker("x")),
            base.copy(colorist = ComicInfo.Colorist("x")),
            base.copy(letterer = ComicInfo.Letterer("x")),
            base.copy(coverArtist = ComicInfo.CoverArtist("x")),
            base.copy(translator = ComicInfo.Translator("x")),
            base.copy(genre = ComicInfo.Genre("x")),
            base.copy(tags = ComicInfo.Tags("x")),
            base.copy(web = ComicInfo.Web("x")),
            base.copy(publishingStatus = ComicInfo.PublishingStatusTachiyomi("x")),
            base.copy(categories = ComicInfo.CategoriesTachiyomi("x")),
            base.copy(source = ComicInfo.SourceMihon("x")),
            base.copy(padding = ComicInfo.PaddingTachiyomiSY("x")),
        )
        variants.forEach { variant ->
            variant shouldNotBe base
            variant shouldBe variant.copy()
        }
        variants.toSet().size shouldBe variants.size
    }
}
