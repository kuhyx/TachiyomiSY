package tachiyomi.core.metadata.comicinfo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/** Drives the generated deserialization constructors: every element with and without its default. */
internal class ComicInfoJsonTest {
    private fun <T> decode(serializer: KSerializer<T>, json: String): T = Json.decodeFromString(serializer, json)

    private fun <T> exercise(serializer: KSerializer<T>, empty: T, filled: T) {
        decode(serializer, "{}") shouldBe empty
        decode(serializer, """{"value":"x"}""") shouldBe filled
        Json.encodeToString(serializer, empty) shouldBe "{}"
        Json.encodeToString(serializer, filled) shouldBe """{"value":"x"}"""
    }

    @Test
    fun elementsRoundTripJson() {
        exercise(ComicInfo.Title.serializer(), ComicInfo.Title(), ComicInfo.Title("x"))
        exercise(ComicInfo.Series.serializer(), ComicInfo.Series(), ComicInfo.Series("x"))
        exercise(ComicInfo.Number.serializer(), ComicInfo.Number(), ComicInfo.Number("x"))
        exercise(ComicInfo.Summary.serializer(), ComicInfo.Summary(), ComicInfo.Summary("x"))
        exercise(ComicInfo.Writer.serializer(), ComicInfo.Writer(), ComicInfo.Writer("x"))
        exercise(ComicInfo.Penciller.serializer(), ComicInfo.Penciller(), ComicInfo.Penciller("x"))
        exercise(ComicInfo.Inker.serializer(), ComicInfo.Inker(), ComicInfo.Inker("x"))
        exercise(ComicInfo.Colorist.serializer(), ComicInfo.Colorist(), ComicInfo.Colorist("x"))
        exercise(ComicInfo.Letterer.serializer(), ComicInfo.Letterer(), ComicInfo.Letterer("x"))
        exercise(ComicInfo.CoverArtist.serializer(), ComicInfo.CoverArtist(), ComicInfo.CoverArtist("x"))
        exercise(ComicInfo.Translator.serializer(), ComicInfo.Translator(), ComicInfo.Translator("x"))
        exercise(ComicInfo.Genre.serializer(), ComicInfo.Genre(), ComicInfo.Genre("x"))
        exercise(ComicInfo.Tags.serializer(), ComicInfo.Tags(), ComicInfo.Tags("x"))
        exercise(ComicInfo.Web.serializer(), ComicInfo.Web(), ComicInfo.Web("x"))
        exercise(
            ComicInfo.PublishingStatusTachiyomi.serializer(),
            ComicInfo.PublishingStatusTachiyomi(),
            ComicInfo.PublishingStatusTachiyomi("x"),
        )
        exercise(
            ComicInfo.CategoriesTachiyomi.serializer(),
            ComicInfo.CategoriesTachiyomi(),
            ComicInfo.CategoriesTachiyomi("x"),
        )
        exercise(ComicInfo.SourceMihon.serializer(), ComicInfo.SourceMihon(), ComicInfo.SourceMihon("x"))
        exercise(
            ComicInfo.PaddingTachiyomiSY.serializer(),
            ComicInfo.PaddingTachiyomiSY(),
            ComicInfo.PaddingTachiyomiSY("x"),
        )
    }

    @Test
    fun documentRequiresEveryField() {
        val fields = listOf(
            "title", "series", "number", "summary", "writer", "penciller", "inker", "colorist", "letterer",
            "coverArtist", "translator", "genre", "tags", "web", "publishingStatus", "categories", "source", "padding",
        )
        val complete = fields.joinToString(",", "{", "}") { "\"$it\":null" }
        val decoded = decode(ComicInfo.serializer(), complete)
        decoded.title shouldBe null
        decoded.padding shouldBe null
        val withTitle = complete.replace(""""title":null""", """"title":{"value":"x"}""")
        val titled = decode(ComicInfo.serializer(), withTitle)
        titled.title shouldBe ComicInfo.Title("x")
        Json.encodeToString(ComicInfo.serializer(), titled).contains(""""title":{"value":"x"}""") shouldBe true
        Json.encodeToString(ComicInfo.serializer(), decoded).contains(""""padding":null""") shouldBe true
        shouldThrow<SerializationException> { decode(ComicInfo.serializer(), "{}") }
    }
}
