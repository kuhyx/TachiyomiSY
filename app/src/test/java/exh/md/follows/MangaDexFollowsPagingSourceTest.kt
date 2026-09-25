package exh.md.follows

import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.InjektStub
import eu.kanade.tachiyomi.source.online.all.MangaDex
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga

internal class MangaDexFollowsPagingSourceTest {
    private val stub = InjektStub()
    private val mangaDex = mockk<MangaDex>(relaxed = true)

    @BeforeEach
    fun setUp() {
        stub.install()
        val networkToLocal = mockk<NetworkToLocalManga>()
        coEvery { networkToLocal(any<Manga>()) } answers { firstArg() }
        stub.serve(networkToLocal)
    }

    @AfterEach
    fun tearDown() = stub.uninstall()

    @Test
    fun pagesComeFromTheFollowsApi() {
        val page = MangasPage(listOf(SManga(url = "/manga/m1", title = "Followed")), true)
        coEvery { mangaDex.fetchFollows(3) } returns page
        val source = MangaDexFollowsPagingSource(mangaDex)
        source.mangadex shouldBe mangaDex
        runBlocking { source.requestNextPage(3) } shouldBe page
    }
}
