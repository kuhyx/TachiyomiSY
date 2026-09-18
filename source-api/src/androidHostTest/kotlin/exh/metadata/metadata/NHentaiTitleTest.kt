package exh.metadata.metadata

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** The title-fallback chains of [NHentaiSearchMetadata.createMangaInfo]. */
internal class NHentaiTitleTest {
    private fun titleOf(meta: NHentaiSearchMetadata): String = meta.createMangaInfo(sampleManga()).title

    private fun shortPreferred(): NHentaiSearchMetadata = NHentaiSearchMetadata().apply {
        preferredTitle = NHentaiSearchMetadata.TITLE_TYPE_SHORT
    }

    @Test
    fun shortPrefersShort() {
        val meta = shortPreferred().apply {
            shortTitle = "short"
            englishTitle = "en"
            japaneseTitle = "jp"
        }
        titleOf(meta) shouldBe "short"
    }

    @Test
    fun shortFallsBackToEnglish() {
        val meta = shortPreferred().apply {
            englishTitle = "en"
            japaneseTitle = "jp"
        }
        titleOf(meta) shouldBe "en"
    }

    @Test
    fun shortFallsBackToJapanese() {
        val meta = shortPreferred().apply { japaneseTitle = "jp" }
        titleOf(meta) shouldBe "jp"
    }

    @Test
    fun shortFallsBackToManga() {
        titleOf(shortPreferred()) shouldBe "Fallback title"
    }

    @Test
    fun defaultPrefersEnglish() {
        val meta = NHentaiSearchMetadata().apply {
            shortTitle = "short"
            englishTitle = "en"
            japaneseTitle = "jp"
        }
        titleOf(meta) shouldBe "en"
    }

    @Test
    fun defaultFallsBackToJapanese() {
        val meta = NHentaiSearchMetadata().apply {
            shortTitle = "short"
            japaneseTitle = "jp"
        }
        titleOf(meta) shouldBe "jp"
    }

    @Test
    fun defaultFallsBackToShort() {
        val meta = NHentaiSearchMetadata().apply { shortTitle = "short" }
        titleOf(meta) shouldBe "short"
    }

    @Test
    fun defaultFallsBackToManga() {
        titleOf(NHentaiSearchMetadata()) shouldBe "Fallback title"
    }

    @Test
    fun englishPreferenceUsesDefault() {
        val meta = NHentaiSearchMetadata().apply {
            preferredTitle = NHentaiSearchMetadata.TITLE_TYPE_ENGLISH
            shortTitle = "short"
            japaneseTitle = "jp"
        }
        titleOf(meta) shouldBe "jp"
    }
}
