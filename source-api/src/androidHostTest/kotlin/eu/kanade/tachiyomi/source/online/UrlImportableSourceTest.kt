package eu.kanade.tachiyomi.source.online

import android.net.Uri
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** A [UrlImportableSource] claiming two hosts and mapping every manga link to its path. */
private class ImportableStubSource : StubSource(), UrlImportableSource {
    override val matchingHosts: List<String> = listOf("manga.example", "m.manga.example")

    override suspend fun mapUrlToMangaUrl(uri: Uri): String? = uri.path
}

/** Host matching, the null mapping defaults and the url cleaners of [UrlImportableSource]. */
internal class UrlImportableSourceTest {
    private val source = ImportableStubSource()

    @Test
    fun matchesListedHostIgnoringCase() {
        source.matchesUri(uriWithHost("Manga.Example")) shouldBe true
        source.matchesUri(uriWithHost("m.manga.example")) shouldBe true
    }

    @Test
    fun rejectsOtherHosts() {
        source.matchesUri(uriWithHost("other.example")) shouldBe false
    }

    @Test
    fun rejectsUriWithoutHost() {
        source.matchesUri(uriWithHost(null)) shouldBe false
    }

    @Test
    fun chapterMappingsDefaultToNull() = runTest {
        val uri = uriWithHost("manga.example")
        source.mapUrlToChapterUrl(uri) shouldBe null
        source.mapChapterUrlToMangaUrl(uri) shouldBe null
    }

    @Test
    fun mangaMappingIsImplemented() = runTest {
        val uri = mockk<Uri> { every { path } returns "/manga/1" }
        source.mapUrlToMangaUrl(uri) shouldBe "/manga/1"
    }

    @Test
    fun cleanMangaUrlStripsHost() {
        source.cleanMangaUrl("https://manga.example/manga/1") shouldBe "/manga/1"
        source.cleanMangaUrl("https://manga.example/manga?id=1") shouldBe "/manga?id=1"
        source.cleanMangaUrl("https://manga.example/manga#top") shouldBe "/manga#top"
        source.cleanMangaUrl("https://manga.example/manga?id=1#top") shouldBe "/manga?id=1#top"
    }

    @Test
    fun cleanMangaUrlKeepsInvalidUri() {
        source.cleanMangaUrl("https://manga.example/a b") shouldBe "https://manga.example/a b"
    }

    @Test
    fun cleanChapterUrlStripsHost() {
        source.cleanChapterUrl("https://manga.example/c/1") shouldBe "/c/1"
        source.cleanChapterUrl("https://manga.example/c?p=1") shouldBe "/c?p=1"
        source.cleanChapterUrl("https://manga.example/c#x") shouldBe "/c#x"
        source.cleanChapterUrl("https://manga.example/c?p=1#x") shouldBe "/c?p=1#x"
    }

    @Test
    fun cleanChapterUrlKeepsInvalidUri() {
        source.cleanChapterUrl("https://manga.example/a|b") shouldBe "https://manga.example/a|b"
    }

    private fun uriWithHost(hostName: String?): Uri = mockk { every { host } returns hostName }
}
