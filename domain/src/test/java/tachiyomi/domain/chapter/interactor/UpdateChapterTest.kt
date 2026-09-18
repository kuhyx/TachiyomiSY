package tachiyomi.domain.chapter.interactor

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import java.io.IOException

internal class UpdateChapterTest {

    private val update = ChapterUpdate(id = 1L, read = true)
    private val repository = mockk<ChapterRepository>()
    private val updateChapter = UpdateChapter(repository)

    @Test
    fun awaitWritesTheUpdate() = runTest {
        coJustRun { repository.update(update) }

        updateChapter.await(update)

        coVerify(exactly = 1) { repository.update(update) }
    }

    @Test
    fun awaitSwallowsStoreFailure() = runTest {
        coEvery { repository.update(update) } throws IOException("store down")

        updateChapter.await(update)

        coVerify(exactly = 1) { repository.update(update) }
    }

    @Test
    fun awaitAllWritesEveryUpdate() = runTest {
        coJustRun { repository.updateAll(listOf(update)) }

        updateChapter.awaitAll(listOf(update))

        coVerify(exactly = 1) { repository.updateAll(listOf(update)) }
    }

    @Test
    fun awaitAllSwallowsStoreFailure() = runTest {
        coEvery { repository.updateAll(listOf(update)) } throws IOException("store down")

        updateChapter.awaitAll(listOf(update))

        coVerify(exactly = 1) { repository.updateAll(listOf(update)) }
    }
}
