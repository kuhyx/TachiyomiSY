package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performTouchInput
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.base.clickLabel
import eu.kanade.tachiyomi.ui.base.poll
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.base.resetUiDispatcher
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchHarness
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.manga.eventually
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import mihon.domain.migration.usecases.MigrateMangaUseCase
import mihon.feature.migration.list.MigrationListScreen
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

/** Searching the migration sources for a manga's new home, and picking the match. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class MigrateSearchScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = SearchHarness()
    private val screen = MigrateSearchScreen(7L)
    private lateinit var navigator: Navigator

    @Before
    fun setUp() {
        resetUiDispatcher()
        harness.source(1L)
        harness.koin.sourcePreferences.migrationSources.set(listOf(1L))
        coEvery { harness.getManga.await(7L) } returns Manga.create().copy(id = 7L, ogTitle = "Old", source = 1L)
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

    private fun show(vararg below: Screen = arrayOf(BlankScreen())) {
        compose.setContent {
            MaterialTheme {
                Navigator(below.toList() + screen) {
                    navigator = it
                    screen.Content()
                }
            }
        }
        compose.pollLabel("T1")
    }

    @Test
    fun sourceRowSearchesThatSource() {
        show()
        compose.clickLabel("▶ S1")
        navigator.lastItem.shouldBeInstanceOf<MigrateSourceSearchScreen>()
    }

    @Test
    fun longPressOpensTheResult() {
        show()
        compose.onAllNodes(hasText("T1")).onLast().performTouchInput { longClick() }
        compose.waitForIdle()
        navigator.lastItem.shouldBeInstanceOf<MangaScreen>()
    }

    @Test
    fun pickShowsTheMigrateDialog() {
        show()
        compose.clickLabel("T1")
        compose.pollLabel("Show entry")
        compose.clickLabel("Show entry")
        navigator.lastItem.shouldBeInstanceOf<MangaScreen>()
    }

    @Test
    fun migratingReplacesTheSearch() {
        show()
        compose.clickLabel("T1")
        compose.pollLabel("Migrate")
        compose.clickLabel("Migrate")
        eventually { navigator.lastItem is MangaScreen }
    }

    @Test
    fun migratingFromMangaGoesBack() {
        show()
        compose.clickLabel("T1")
        compose.pollLabel("Migrate")
        navigator.push(MangaScreen(7L))
        compose.waitForIdle()
        compose.clickLabel("Migrate")
        compose.poll({ "stack ${navigator.items.map { it::class.simpleName }}" }) {
            navigator.items.size == 4 && navigator.lastItem is MangaScreen
        }
    }

    @Test
    fun migrationListTakesTheMatch() {
        val list = MigrationListScreen(listOf(7L), null)
        show(list)
        compose.clickLabel("T1")
        navigator.lastItem shouldBe list
    }

    @Test
    fun upLeaves() {
        show()
        compose.clickLabel("Navigate up")
        navigator.lastItem.shouldBeInstanceOf<BlankScreen>()
    }
}
