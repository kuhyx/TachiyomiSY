package tachiyomi.core.metadata.comicinfo

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ComicInfoElementsTest {
    private val info = ComicInfo(
        title = ComicInfo.Title("Chapter"),
        series = ComicInfo.Series("Series"),
        number = ComicInfo.Number("1"),
        summary = ComicInfo.Summary("Summary"),
        writer = ComicInfo.Writer("Writer"),
        penciller = ComicInfo.Penciller("Penciller"),
        inker = ComicInfo.Inker("Inker"),
        colorist = ComicInfo.Colorist("Colorist"),
        letterer = ComicInfo.Letterer("Letterer"),
        coverArtist = ComicInfo.CoverArtist("Cover"),
        translator = ComicInfo.Translator("Translator"),
        genre = ComicInfo.Genre("Genre"),
        tags = ComicInfo.Tags("Tags"),
        web = ComicInfo.Web("https://x"),
        publishingStatus = ComicInfo.PublishingStatusTachiyomi("Ongoing"),
        categories = ComicInfo.CategoriesTachiyomi("Cat"),
        source = ComicInfo.SourceMihon("src"),
        padding = ComicInfo.PaddingTachiyomiSY("2"),
    )

    @Test
    fun documentEqualsItsCopy() {
        info.copy() shouldBe info
        info.hashCode() shouldBe info.copy().hashCode()
        info.toString().isNotEmpty() shouldBe true
    }

    @Test
    fun elementsDefaultToEmptyText() {
        ComicInfo.Title().value shouldBe ""
        ComicInfo.Series().value shouldBe ""
        ComicInfo.Number().value shouldBe ""
        ComicInfo.Summary().value shouldBe ""
        ComicInfo.Writer().value shouldBe ""
        ComicInfo.Penciller().value shouldBe ""
        ComicInfo.Inker().value shouldBe ""
        ComicInfo.Colorist().value shouldBe ""
        ComicInfo.Letterer().value shouldBe ""
        ComicInfo.CoverArtist().value shouldBe ""
        ComicInfo.Translator().value shouldBe ""
        ComicInfo.Genre().value shouldBe ""
        ComicInfo.Tags().value shouldBe ""
        ComicInfo.Web().value shouldBe ""
        ComicInfo.PublishingStatusTachiyomi().value shouldBe ""
        ComicInfo.CategoriesTachiyomi().value shouldBe ""
        ComicInfo.SourceMihon().value shouldBe ""
        ComicInfo.PaddingTachiyomiSY().value shouldBe ""
    }
}
