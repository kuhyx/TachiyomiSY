package tachiyomi.domain.source.model

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class StubSourceTest {

    private val stub = StubSource(id = 1L, lang = "en", name = "Src")

    @Test
    fun keepsIdentity() {
        stub.id shouldBe 1L
        stub.lang shouldBe "en"
        stub.name shouldBe "Src"
        stub.supportsLatest shouldBe false
    }

    @Test
    fun toStringOfValidStub() {
        stub.toString() shouldBe "Src (EN)"
    }

    @Test
    fun toStringWithBlankName() {
        StubSource(id = 2L, lang = "en", name = " ").toString() shouldBe "2"
    }

    @Test
    fun toStringWithBlankLang() {
        StubSource(id = 3L, lang = "", name = "Src").toString() shouldBe "3"
    }

    @Test
    fun fromCopiesSource() {
        val copy = StubSource.from(stub)

        copy.id shouldBe 1L
        copy.lang shouldBe "en"
        copy.name shouldBe "Src"
    }

    @Test
    fun popularThrows() = runTest {
        shouldThrow<SourceNotInstalledException> { stub.getPopularManga(1) }
    }

    @Test
    fun latestThrows() = runTest {
        shouldThrow<SourceNotInstalledException> { stub.getLatestUpdates(1) }
    }

    @Test
    fun searchThrows() = runTest {
        shouldThrow<SourceNotInstalledException> { stub.getSearchManga(1, "q", FilterList()) }
    }

    @Test
    fun mangaUpdateThrows() = runTest {
        shouldThrow<SourceNotInstalledException> {
            stub.getMangaUpdate(
                manga = SManga.create(),
                chapters = emptyList(),
                fetchDetails = true,
                fetchChapters = true,
            )
        }
    }

    @Test
    fun pageListThrows() = runTest {
        shouldThrow<SourceNotInstalledException> { stub.getPageList(SChapter.create()) }
    }
}
