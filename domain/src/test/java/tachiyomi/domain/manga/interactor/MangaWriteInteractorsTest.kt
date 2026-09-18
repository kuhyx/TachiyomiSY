package tachiyomi.domain.manga.interactor

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.MangaFixtures
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository

internal class MangaWriteInteractorsTest {

    private val manga = MangaFixtures.manga(id = 4L)
    private val repository = mockk<MangaRepository>()

    @Test
    fun deleteMangaByIdDelegates() = runTest {
        coEvery { repository.deleteManga(4L) } returns Unit

        DeleteMangaById(repository).await(4L)

        coVerify(exactly = 1) { repository.deleteManga(4L) }
    }

    @Test
    fun networkToLocalSingleManga() = runTest {
        val inserted = manga.copy(id = 10L)
        coEvery { repository.insertNetworkManga(listOf(manga)) } returns listOf(inserted)

        NetworkToLocalManga(repository)(manga) shouldBe inserted
    }

    @Test
    fun networkToLocalMangaList() = runTest {
        val batch = listOf(manga, manga.copy(id = 5L))
        coEvery { repository.insertNetworkManga(batch) } returns batch

        NetworkToLocalManga(repository)(batch) shouldContainExactly batch
    }

    @Test
    fun resetViewerFlagsDelegates() = runTest {
        coEvery { repository.resetViewerFlags() } returns true

        ResetViewerFlags(repository).await() shouldBe true
    }

    @Test
    fun updateNotesSendsOnlyNotes() = runTest {
        val update = slot<MangaUpdate>()
        coEvery { repository.update(capture(update)) } returns true

        UpdateMangaNotes(repository)(4L, "hello") shouldBe true

        update.captured shouldBe MangaUpdate(id = 4L, notes = "hello")
    }
}
