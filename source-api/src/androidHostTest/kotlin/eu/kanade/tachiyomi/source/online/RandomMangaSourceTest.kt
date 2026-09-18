package eu.kanade.tachiyomi.source.online

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** A [RandomMangaSource] that always picks the same url. */
private class RandomStubSource : StubSource(), RandomMangaSource {
    override suspend fun fetchRandomMangaUrl(): String = "/random/4"
}

/** The random-manga contract of [RandomMangaSource]. */
internal class RandomMangaSourceTest {
    @Test
    fun randomUrlIsImplemented() = runTest {
        val source: RandomMangaSource = RandomStubSource()
        source.fetchRandomMangaUrl() shouldBe "/random/4"
        source.id shouldBe 7L
    }
}
