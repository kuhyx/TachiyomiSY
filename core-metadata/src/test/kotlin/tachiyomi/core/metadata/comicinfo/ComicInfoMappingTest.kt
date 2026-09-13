package tachiyomi.core.metadata.comicinfo

import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ComicInfoMappingTest {
    private fun manga(): SManga = SManga.create().apply {
        title = "Title"
        status = SManga.ONGOING
    }

    @Test
    fun exportCopiesPresentFields() {
        val manga = manga().apply {
            description = "Summary"
            author = "Writer"
            artist = "Penciller"
            genre = "Action, Drama"
        }
        val info = manga.getComicInfo()
        info.series shouldBe ComicInfo.Series("Title")
        info.summary shouldBe ComicInfo.Summary("Summary")
        info.writer shouldBe ComicInfo.Writer("Writer")
        info.penciller shouldBe ComicInfo.Penciller("Penciller")
        info.genre shouldBe ComicInfo.Genre("Action, Drama")
        info.publishingStatus shouldBe ComicInfo.PublishingStatusTachiyomi("Ongoing")
        info.title shouldBe null
        info.padding shouldBe null
    }

    @Test
    fun exportLeavesAbsentNull() {
        val info = manga().getComicInfo()
        info.summary shouldBe null
        info.writer shouldBe null
        info.penciller shouldBe null
        info.genre shouldBe null
        info.xmlSchema shouldBe "http://www.w3.org/2001/XMLSchema"
        info.xmlSchemaInstance shouldBe "http://www.w3.org/2001/XMLSchema-instance"
    }

    @Test
    fun importMergesCreditsAndGenres() {
        val manga = manga()
        manga.copyFromComicInfo(
            ComicInfo(
                title = ComicInfo.Title("Chapter"),
                series = ComicInfo.Series("Series"),
                number = ComicInfo.Number("1"),
                summary = ComicInfo.Summary("Summary"),
                writer = ComicInfo.Writer("Writer"),
                penciller = ComicInfo.Penciller("A, B"),
                inker = ComicInfo.Inker("B"),
                colorist = ComicInfo.Colorist(" C "),
                letterer = ComicInfo.Letterer("D"),
                coverArtist = ComicInfo.CoverArtist("E"),
                translator = ComicInfo.Translator("T"),
                genre = ComicInfo.Genre("Action"),
                tags = ComicInfo.Tags("Action"),
                web = ComicInfo.Web("https://x"),
                publishingStatus = ComicInfo.PublishingStatusTachiyomi("Completed"),
                categories = ComicInfo.CategoriesTachiyomi("Cat"),
                source = ComicInfo.SourceMihon("src"),
                padding = ComicInfo.PaddingTachiyomiSY("1"),
            ),
        )
        manga.title shouldBe "Series"
        manga.author shouldBe "Writer"
        manga.description shouldBe "Summary"
        manga.genre shouldBe "Action, Cat"
        manga.artist shouldBe "A, B, C, D, E"
        manga.status shouldBe SManga.COMPLETED
    }

    @Test
    fun importKeepsFieldsWhenEmpty() {
        val manga = manga().apply {
            author = "Kept"
            artist = "Kept"
            genre = "Kept"
            description = "Kept"
        }
        manga.copyFromComicInfo(
            ComicInfo(
                title = null, series = null, number = null, summary = null, writer = null, penciller = null,
                inker = null, colorist = null, letterer = null, coverArtist = null, translator = null,
                genre = null, tags = null, web = null, publishingStatus = null, categories = null,
                source = null, padding = null,
            ),
        )
        manga.title shouldBe "Title"
        manga.author shouldBe "Kept"
        manga.artist shouldBe "Kept"
        manga.genre shouldBe "Kept"
        manga.description shouldBe "Kept"
        manga.status shouldBe SManga.UNKNOWN
    }
}
