package exh.metadata.metadata

import exh.metadata.MetadataUtil
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** [NHentaiSearchMetadata.getExtraInfoPairs] and JSON serialization. */
internal class NHentaiInfoPairsTest {
    private fun fullMetadata(): NHentaiSearchMetadata = NHentaiSearchMetadata().apply {
        nhId = 1
        uploadDate = 1_600_000_000L
        favoritesCount = 20
        mediaId = "media"
        japaneseTitle = "jp"
        englishTitle = "en"
        shortTitle = "short"
        coverImageUrl = "https://t/cover.jpg"
        pageImagePreviewUrls = listOf("p1", "p2")
        scanlator = "scan"
        preferredTitle = NHentaiSearchMetadata.TITLE_TYPE_SHORT
    }

    @Test
    fun extraPairsListEveryField() {
        val posted = MetadataUtil.EX_DATE_FORMAT.format(
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(1_600_000_000L), ZoneId.systemDefault()),
        )
        fullMetadata().getExtraInfoPairs(stubbedContext()) shouldContainExactly listOf(
            labelFor(SYMR.strings.id) to "1",
            labelFor(SYMR.strings.date_posted) to posted,
            labelFor(SYMR.strings.total_favorites) to "20",
            labelFor(SYMR.strings.media_id) to "media",
            labelFor(SYMR.strings.japanese_title) to "jp",
            labelFor(SYMR.strings.english_title) to "en",
            labelFor(SYMR.strings.short_title) to "short",
            labelFor(SYMR.strings.thumbnail_url) to "https://t/cover.jpg",
            labelFor(SYMR.strings.page_count) to "2",
            labelFor(MR.strings.scanlator) to "scan",
        )
    }

    @Test
    fun extraPairsSkipNullFields() {
        NHentaiSearchMetadata().getExtraInfoPairs(stubbedContext()) shouldContainExactly listOf(
            labelFor(SYMR.strings.page_count) to "0",
        )
    }

    @Test
    fun jsonRoundTripsEveryField() {
        val encoded = Json.encodeToString(NHentaiSearchMetadata.serializer(), fullMetadata())
        val decoded = Json.decodeFromString(NHentaiSearchMetadata.serializer(), encoded)
        decoded.nhId shouldBe 1
        decoded.uploadDate shouldBe 1_600_000_000L
        decoded.favoritesCount shouldBe 20
        decoded.mediaId shouldBe "media"
        decoded.coverImageUrl shouldBe "https://t/cover.jpg"
        decoded.pageImagePreviewUrls shouldBe listOf("p1", "p2")
        decoded.scanlator shouldBe "scan"
        decoded.preferredTitle shouldBe NHentaiSearchMetadata.TITLE_TYPE_SHORT
        // Titles are transient: they live in the titles table, not in the extra column.
        decoded.titles shouldBe emptyList()
    }

    @Test
    fun jsonOmitsDefaults() {
        Json.encodeToString(NHentaiSearchMetadata.serializer(), NHentaiSearchMetadata()) shouldBe "{}"
        val decoded = Json.decodeFromString(NHentaiSearchMetadata.serializer(), "{}")
        decoded.nhId shouldBe null
        decoded.pageImagePreviewUrls shouldBe emptyList()
    }

    @Test
    fun jsonAcceptsExplicitNulls() {
        val text = """
            {"nhId":null,"uploadDate":null,"favoritesCount":null,"mediaId":null,
             "coverImageUrl":null,"pageImagePreviewUrls":[],"scanlator":null,"preferredTitle":null}
        """.trimIndent()
        val decoded = Json.decodeFromString(NHentaiSearchMetadata.serializer(), text)
        decoded.nhId shouldBe null
        decoded.uploadDate shouldBe null
        decoded.favoritesCount shouldBe null
        decoded.mediaId shouldBe null
        decoded.coverImageUrl shouldBe null
        decoded.pageImagePreviewUrls shouldBe emptyList()
        decoded.scanlator shouldBe null
        decoded.preferredTitle shouldBe null
    }
}
