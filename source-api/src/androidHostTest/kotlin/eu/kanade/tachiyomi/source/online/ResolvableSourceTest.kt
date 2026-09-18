package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** A [ResolvableSource] that classifies uris by path prefix. */
private class ResolvableStubSource : StubSource(), ResolvableSource {
    override fun getUriType(uri: String): UriType = when {
        uri.contains("/manga/") -> UriType.Manga
        uri.contains("/chapter/") -> UriType.Chapter
        else -> UriType.Unknown
    }

    override suspend fun getManga(uri: String): SManga? =
        SManga(url = uri, title = "resolved").takeIf { "manga" in uri }

    override suspend fun getChapter(uri: String): SChapter? =
        SChapter(name = "resolved", url = uri).takeIf { "chapter" in uri }
}

/** The uri classification contract of [ResolvableSource] and the [UriType] objects. */
internal class ResolvableSourceTest {
    private val source = ResolvableStubSource()

    @Test
    fun classifiesUris() {
        source.getUriType("https://x/manga/1") shouldBe UriType.Manga
        source.getUriType("https://x/chapter/1") shouldBe UriType.Chapter
        source.getUriType("https://x/other") shouldBe UriType.Unknown
    }

    @Test
    fun resolvesMangaAndChapter() = runTest {
        source.getManga("https://x/manga/1")?.url shouldBe "https://x/manga/1"
        source.getManga("https://x/other") shouldBe null
        source.getChapter("https://x/chapter/1")?.url shouldBe "https://x/chapter/1"
        source.getChapter("https://x/other") shouldBe null
    }

    @Test
    fun uriTypesAreDistinctSingletons() {
        val types: List<UriType> = listOf(UriType.Manga, UriType.Chapter, UriType.Unknown)
        types.toSet().size shouldBe 3
        types.map { it.toString() } shouldBe listOf("Manga", "Chapter", "Unknown")
        UriType.Manga.hashCode() shouldBe UriType.Manga.hashCode()
        UriType.Manga shouldNotBe UriType.Chapter
    }
}
