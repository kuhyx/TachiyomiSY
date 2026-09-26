package eu.kanade.presentation.more.settings.screen.advanced

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.data.Database
import tachiyomi.domain.source.interactor.GetSourcesWithNonLibraryManga
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.SourceWithCount

internal fun sourceWithCount(id: Long, name: String): SourceWithCount =
    SourceWithCount(Source(id = id, lang = "en", name = name, supportsLatest = false, isStub = false), count = id)

internal class ClearDatabaseScreenModelTest {
    private val sources = MutableSharedFlow<List<SourceWithCount>>(replay = 1)
    private val database = mockk<Database>(relaxed = true)
    private val b = sourceWithCount(id = 2, name = "Beta")
    private val a = sourceWithCount(id = 1, name = "Alpha")

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val getSources = mockk<GetSourcesWithNonLibraryManga> { every { subscribe() } returns sources }
        startKoin {
            modules(
                module {
                    single { getSources }
                    single { database }
                },
            )
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun ready(model: ClearDatabaseScreenModel): ClearDatabaseScreenModel.State.Ready {
        val deadline = System.currentTimeMillis() + 5_000
        while (model.state.value !is ClearDatabaseScreenModel.State.Ready && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }
        return model.state.value as ClearDatabaseScreenModel.State.Ready
    }

    @Test
    fun loadingIgnoresActions() = runTest {
        val model = ClearDatabaseScreenModel()
        model.toggleSelection(a.source)
        model.clearSelection()
        model.selectAll()
        model.invertSelection()
        model.showConfirmation()
        model.hideConfirmation()
        model.removeMangaBySourceId(keepReadManga = true)
        model.state.value shouldBe ClearDatabaseScreenModel.State.Loading
        verify(exactly = 0) { database.mangasQueries }
    }

    @Test
    fun readySortsAndSelects() = runTest {
        val model = ClearDatabaseScreenModel()
        sources.emit(listOf(b, a))
        ready(model).items shouldBe listOf(a, b)
        model.toggleSelection(a.source)
        ready(model).selection shouldBe listOf(1L)
        model.toggleSelection(a.source)
        ready(model).selection shouldBe emptyList()
        model.selectAll()
        ready(model).selection shouldBe listOf(1L, 2L)
        model.clearSelection()
        model.toggleSelection(b.source)
        model.invertSelection()
        ready(model).selection shouldBe listOf(1L)
    }

    @Test
    fun confirmationAndRemoval() = runTest {
        val model = ClearDatabaseScreenModel()
        sources.emit(listOf(a))
        ready(model)
        model.showConfirmation()
        ready(model).showConfirmation shouldBe true
        model.hideConfirmation()
        ready(model).showConfirmation shouldBe false
        model.selectAll()
        model.removeMangaBySourceId(keepReadManga = false)
        coVerify { database.mangasQueries.deleteNonLibraryManga(listOf(1L), 0L) }
        coVerify { database.historyQueries.removeResettedHistory() }
        sources.emit(listOf(a, b))
        val deadline = System.currentTimeMillis() + 5_000
        while (ready(model).items.size < 2 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }
        ready(model).items shouldBe listOf(a, b)
    }

    @Test
    fun finishedSourceKeepsItems() {
        stopKoin()
        val finite = mockk<GetSourcesWithNonLibraryManga> { every { subscribe() } returns flowOf(listOf(b, a)) }
        startKoin {
            modules(
                module {
                    single { finite }
                    single { database }
                },
            )
        }
        ready(ClearDatabaseScreenModel()).items shouldBe listOf(a, b)
    }
}
