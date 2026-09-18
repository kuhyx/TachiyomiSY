package tachiyomi.data.manga

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.Database
import tachiyomi.data.MangasQueries
import tachiyomi.data.inMemoryDatabase
import tachiyomi.domain.manga.model.MangaUpdate

/** The failure paths: a store that throws is logged and reported as `false`. */
internal class MangaWriteRepositoryImplFailTest {
    private val queries = mockk<MangasQueries> {
        coEvery { resetViewerFlags() } throws IllegalStateException("boom")
    }
    private val failing = mockk<Database> {
        every { mangasQueries } returns queries
        coEvery { transaction(any(), any()) } throws IllegalStateException("boom")
    }
    private val repository = MangaWriteRepositoryImpl(failing)

    @Test
    fun resetViewerFlagsFalseOnError() = runTest {
        repository.resetViewerFlags() shouldBe false
    }

    @Test
    fun updateFalseOnError() = runTest {
        repository.update(MangaUpdate(id = 1L, title = "x")) shouldBe false
    }

    @Test
    fun updateAllFalseOnError() = runTest {
        repository.updateAll(listOf(MangaUpdate(id = 1L), MangaUpdate(id = 2L))) shouldBe false
    }

    @Test
    fun deleteMangaRemovesRow() = runTest {
        val database = inMemoryDatabase()
        val id = database.insertManga(url = "/a")

        MangaWriteRepositoryImpl(database).deleteManga(id)

        database.mangasQueries.getMangaById(id).awaitAsOneOrNull() shouldBe null
    }
}
