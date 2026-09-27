package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.base.labelShown
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.base.resetUiDispatcher
import eu.kanade.tachiyomi.ui.browse.ScreenHost
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchHarness
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import mihon.domain.migration.usecases.MigrateMangaUseCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.manga.model.Manga

/** The migration search shows its sources before the entry being migrated has loaded. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class MigrateSearchWaitTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = SearchHarness()
    private val entry = CompletableDeferred<Manga>()

    @Before
    fun setUp() {
        resetUiDispatcher()
        harness.source(1L)
        harness.koin.sourcePreferences.migrationSources.set(listOf(1L))
        coEvery { harness.getManga.await(7L) } coAnswers { entry.await() }
        every { harness.getManga.subscribe(any<String>(), any()) } returns flowOf(null)
        harness.start()
        loadKoinModules(
            module {
                single { mockk<DownloadManager>(relaxed = true) }
                single { mockk<CoverCache>(relaxed = true) }
                single { mockk<MigrateMangaUseCase>(relaxed = true) }
            },
        )
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun resultsWaitForTheEntry() {
        ScreenHost(MigrateSearchScreen(7L)).show(compose)
        coVerify(timeout = 5_000) { harness.getManga.await(7L) }
        compose.waitForIdle()
        compose.labelShown("T1") shouldBe false
        entry.complete(Manga.create().copy(id = 7L, ogTitle = "Old", source = 1L))
        compose.pollLabel("T1")
    }
}
