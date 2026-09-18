package exh.metadata.metadata

import exh.metadata.metadata.base.RaisedTitle
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class NHentaiSearchMetadataTest {
    @Test
    fun urlIsNullWithoutId() {
        NHentaiSearchMetadata().url shouldBe null
    }

    @Test
    fun urlDerivesFromId() {
        val meta = NHentaiSearchMetadata().apply { nhId = 123 }
        meta.url shouldBe "https://nhentai.net/g/123/"
    }

    @Test
    fun settingUrlNullKeepsId() {
        val meta = NHentaiSearchMetadata().apply { nhId = 5 }
        meta.url = null
        meta.nhId shouldBe 5
    }

    @Test
    fun settingUrlParsesId() {
        val meta = NHentaiSearchMetadata()
        meta.url = "https://nhentai.net/g/456/"
        meta.nhId shouldBe 456
    }

    @Test
    fun nhUrlToIdTakesLastSegment() {
        NHentaiSearchMetadata.nhUrlToId("https://nhentai.net/g/123/") shouldBe 123
        NHentaiSearchMetadata.nhUrlToId("/g/7") shouldBe 7
    }

    @Test
    fun nhUrlToIdRejectsNonNumeric() {
        shouldThrow<NumberFormatException> { NHentaiSearchMetadata.nhUrlToId("/g/abc/") }
    }

    @Test
    fun nhUrlToIdRejectsBlankSegments() {
        shouldThrow<NoSuchElementException> { NHentaiSearchMetadata.nhUrlToId("///") }
        shouldThrow<NoSuchElementException> { NHentaiSearchMetadata.nhUrlToId("") }
    }

    @Test
    fun nhIdToPathWrapsInSlashes() {
        NHentaiSearchMetadata.nhIdToPath(42) shouldBe "/g/42/"
    }

    @Test
    fun titleDelegatesWriteTitles() {
        val meta = NHentaiSearchMetadata().apply {
            japaneseTitle = "jp"
            englishTitle = "en"
            shortTitle = "short"
        }
        meta.titles shouldContainExactly listOf(
            RaisedTitle(title = "jp", type = 0),
            RaisedTitle(title = "en", type = NHentaiSearchMetadata.TITLE_TYPE_ENGLISH),
            RaisedTitle(title = "short", type = NHentaiSearchMetadata.TITLE_TYPE_SHORT),
        )
        meta.japaneseTitle shouldBe "jp"
        meta.englishTitle shouldBe "en"
        meta.shortTitle shouldBe "short"
    }

    @Test
    fun titleDelegateNullRemovesTitle() {
        val meta = NHentaiSearchMetadata().apply { englishTitle = "en" }
        meta.englishTitle = null
        meta.englishTitle shouldBe null
        meta.titles shouldBe emptyList()
    }
}
