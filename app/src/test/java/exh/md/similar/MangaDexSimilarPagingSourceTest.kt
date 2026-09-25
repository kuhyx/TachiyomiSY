package exh.md.similar

import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.source.model.MetadataMangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.InjektStub
import eu.kanade.tachiyomi.source.online.all.MangaDex
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.recs.sources.sourceManga
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.source.NoResultsException
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.sy.SYMR
import java.net.HttpURLConnection

internal class MangaDexSimilarPagingSourceTest {
    private val stub = InjektStub()
    private val mangaDex = mockk<MangaDex>(relaxed = true)
    private lateinit var source: MangaDexSimilarPagingSource

    @BeforeEach
    fun setUp() {
        stub.install()
        val networkToLocal = mockk<NetworkToLocalManga>()
        coEvery { networkToLocal(any<Manga>()) } answers { firstArg() }
        stub.serve(networkToLocal)
        every { mangaDex.id } returns 8L
        source = MangaDexSimilarPagingSource(sourceManga(), mangaDex)
    }

    @AfterEach
    fun tearDown() = stub.uninstall()

    private fun page(vararg titles: String) = MetadataMangasPage(
        mangas = titles.map { SManga(url = "/manga/$it", title = it) },
        hasNextPage = false,
        mangasMetadata = titles.map { MangaDexSearchMetadata() },
    )

    @Test
    fun identity() {
        source.name shouldBe "MangaDex"
        source.category shouldBe SYMR.strings.similar_titles
        source.associatedSourceId shouldBe 8L
    }

    @Test
    fun relatedThenSimilar() {
        coEvery { mangaDex.getMangaSimilar(any()) } returns page("similar")
        coEvery { mangaDex.getMangaRelated(any()) } returns page("related")
        val result = runBlocking { source.requestNextPage(1) } as MetadataMangasPage
        result.mangas.map { it.title } shouldContainExactly listOf("related", "similar")
        result.mangasMetadata.size shouldBe 2
        result.hasNextPage shouldBe false
    }

    @Test
    fun emptyResultsRaise() {
        coEvery { mangaDex.getMangaSimilar(any()) } returns page()
        coEvery { mangaDex.getMangaRelated(any()) } returns page()
        shouldThrow<NoResultsException> { runBlocking { source.requestNextPage(1) } }
    }

    @Test
    fun notFoundIsMapped() {
        coEvery { mangaDex.getMangaSimilar(any()) } throws HttpException(HttpURLConnection.HTTP_NOT_FOUND)
        coEvery { mangaDex.getMangaRelated(any()) } returns page("related")
        // The 404 arm does run, but the failed `async` has already cancelled the enclosing scope,
        // so the scope's own failure -- the HttpException -- is what the caller sees.
        shouldThrow<HttpException> { runBlocking { source.requestNextPage(1) } }.code shouldBe
            HttpURLConnection.HTTP_NOT_FOUND
    }

    @Test
    fun otherHttpErrorsPropagate() {
        coEvery { mangaDex.getMangaSimilar(any()) } throws HttpException(HttpURLConnection.HTTP_UNAVAILABLE)
        coEvery { mangaDex.getMangaRelated(any()) } returns page("related")
        shouldThrow<HttpException> { runBlocking { source.requestNextPage(1) } }.code shouldBe
            HttpURLConnection.HTTP_UNAVAILABLE
    }
}
