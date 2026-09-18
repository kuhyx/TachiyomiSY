package exh.source

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.StubSource

internal class DomainSourceHelpersTest {

    @AfterEach
    fun afterEach() {
        metadataDelegatedSourceIds = emptyList()
        mangaDexSourceIds = emptyList()
        nHentaiSourceIds = emptyList()
        lanraragiSourceIds = emptyList()
        LIBRARY_UPDATE_EXCLUDED_SOURCES = listOf(EH_SOURCE_ID, EXH_SOURCE_ID, PURURIN_SOURCE_ID)
    }

    @Test
    fun metadataRangeIsMetadata() {
        isMetadataSource(6900L) shouldBe true
        isMetadataSource(6999L) shouldBe true
    }

    @Test
    fun outsideRangeNeedsDelegation() {
        isMetadataSource(6899L) shouldBe false
        isMetadataSource(7000L) shouldBe false
    }

    @Test
    fun delegatedIdIsMetadata() {
        metadataDelegatedSourceIds = listOf(5L, 7L, 9L)

        metadataDelegatedSourceIds shouldContainExactly listOf(5L, 7L, 9L)
        isMetadataSource(7L) shouldBe true
        isMetadataSource(8L) shouldBe false
    }

    @Test
    fun ehBasedSourceIsEhOrExh() {
        stub(EH_SOURCE_ID).isEhBasedSource() shouldBe true
        stub(EXH_SOURCE_ID).isEhBasedSource() shouldBe true
        stub(PURURIN_SOURCE_ID).isEhBasedSource() shouldBe false
    }

    @Test
    fun mdBasedSourceIsDelegated() {
        mangaDexSourceIds = listOf(3L)

        mangaDexSourceIds shouldContainExactly listOf(3L)
        stub(3L).isMdBasedSource() shouldBe true
        stub(4L).isMdBasedSource() shouldBe false
    }

    @Test
    fun ehBasedMangaIsEhOrExh() {
        Manga.create().copy(source = EH_SOURCE_ID).isEhBasedManga() shouldBe true
        Manga.create().copy(source = EXH_SOURCE_ID).isEhBasedManga() shouldBe true
        Manga.create().copy(source = PURURIN_SOURCE_ID).isEhBasedManga() shouldBe false
    }

    @Test
    fun excludedSourcesDefault() {
        LIBRARY_UPDATE_EXCLUDED_SOURCES shouldContainExactly listOf(EH_SOURCE_ID, EXH_SOURCE_ID, PURURIN_SOURCE_ID)
        nHentaiSourceIds shouldBe emptyList()
        lanraragiSourceIds shouldBe emptyList()
    }

    @Test
    fun loadedIdsAreReplaceable() {
        nHentaiSourceIds = listOf(21L)
        lanraragiSourceIds = listOf(22L)
        LIBRARY_UPDATE_EXCLUDED_SOURCES = LIBRARY_UPDATE_EXCLUDED_SOURCES + nHentaiSourceIds

        nHentaiSourceIds shouldContainExactly listOf(21L)
        lanraragiSourceIds shouldContainExactly listOf(22L)
        LIBRARY_UPDATE_EXCLUDED_SOURCES shouldContainExactly listOf(EH_SOURCE_ID, EXH_SOURCE_ID, PURURIN_SOURCE_ID, 21L)
    }

    private fun stub(id: Long): StubSource = StubSource(id = id, lang = "en", name = "Src")
}
