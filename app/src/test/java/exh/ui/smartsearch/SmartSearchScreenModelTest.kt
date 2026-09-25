package exh.ui.smartsearch

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

private const val WAIT_MS = 10_000L

internal class SmartSearchScreenModelTest {
    private val config = SourcesScreen.SmartSearchConfig(origTitle = "Needle")
    private val networkToLocalManga = mockk<NetworkToLocalManga>()
    private val source = mockk<Source> {
        every { id } returns 3L
        every { name } returns "Src"
        every { getFilterList() } returns FilterList()
    }
    private val sourceManager = mockk<SourceManager> { every { getOrStub(3L) } returns source }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        coEvery { networkToLocalManga(any<Manga>()) } answers { firstArg<Manga>().copy(id = 42L) }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    private fun answer(block: () -> List<SManga>) {
        coEvery { source.getSearchManga(any(), any(), any()) } answers { MangasPage(block(), false) }
    }

    private fun model(): SmartSearchScreenModel = SmartSearchScreenModel(
        sourceId = 3L,
        config = config,
        networkToLocalManga = networkToLocalManga,
        sourceManager = sourceManager,
    )

    private fun SmartSearchScreenModel.settled(): SmartSearchScreenModel.SearchResults = runBlocking {
        withTimeout(WAIT_MS) { state.first { it != null } }!!
    }

    @Test
    fun aMatchIsFound() {
        answer { listOf(SManga.create().apply { title = "Needle" }) }
        val model = model()
        model.source shouldBe source
        val found = model.settled().shouldBeInstanceOf<SmartSearchScreenModel.SearchResults.Found>()
        found.manga.id shouldBe 42L
    }

    @Test
    fun noMatchIsNotFound() {
        answer { emptyList() }
        model().settled() shouldBe SmartSearchScreenModel.SearchResults.NotFound
    }

    @Test
    fun aFailureIsAnError() {
        answer { error("boom") }
        model().settled() shouldBe SmartSearchScreenModel.SearchResults.Error
    }

    @Test
    fun cancellationLeavesNoResult() {
        answer { throw CancellationException("stop") }
        val model = model()
        Thread.sleep(300)
        model.state.value shouldBe null
    }

    @Test
    fun collaboratorsComeFromInjekt() {
        answer { emptyList() }
        startKoin {
            modules(
                module {
                    single { networkToLocalManga }
                    single { sourceManager }
                },
            )
        }
        SmartSearchScreenModel(sourceId = 3L, config = config).settled() shouldBe
            SmartSearchScreenModel.SearchResults.NotFound
    }
}
