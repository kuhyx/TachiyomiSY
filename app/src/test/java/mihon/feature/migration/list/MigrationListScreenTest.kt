package mihon.feature.migration.list

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.util.ProvideBack
import eu.kanade.presentation.util.TestBackOwner
import eu.kanade.tachiyomi.ui.library.ComposableScreen
import eu.kanade.tachiyomi.ui.library.hasLabel
import eu.kanade.tachiyomi.ui.library.waitForLabel
import eu.kanade.tachiyomi.ui.manga.clearVoyagerScopes
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class MigrationListScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = MigrationListHarness()
    private val back = TestBackOwner()
    private var navigator: Navigator? = null

    @Before
    fun setUp() {
        harness.start()
        loadKoinModules(
            module {
                single { harness.sourceManager }
                single { harness.getManga }
                single { harness.networkToLocalManga }
                single { harness.getChaptersByMangaId }
                single { harness.migrateManga }
                single { harness.updateMangaFromRemote }
            },
        )
        harness.preferences.migrationSources.set(listOf(10L))
        harness.hits[10L] = { query ->
            if (query.startsWith("Entry 1")) listOf(hit("Entry 1", id = 51L)) else emptyList()
        }
        harness.chapters[51L] = listOf(1.0)
    }

    @After
    fun tearDown() {
        clearVoyagerScopes()
        harness.stop()
    }

    private fun show(screen: MigrationListScreen = MigrationListScreen(listOf(1L, 2L), null)) {
        // Above a base screen, so popping the list reveals "base".
        val base = ComposableScreen { Text("base") }
        compose.setContent {
            ProvideBack(back) {
                MaterialTheme {
                    Navigator(listOf(base, screen)) { current ->
                        navigator = current
                        val top = current.lastItem
                        if (top === screen || top === base) CurrentScreen() else Text("opened:${top::class.simpleName}")
                    }
                }
            }
        }
        compose.waitForLabel("Source 10")
        compose.waitForLabel("No alternatives found")
    }

    private fun click(label: String) {
        compose.waitForLabel(label)
        compose.onNodeWithText(label).performClick()
        compose.waitForIdle()
    }

    @Test
    fun rowsOpenTheirEntry() {
        show()
        click("Source 1")
        compose.waitForLabel("opened:MangaScreen")
    }

    @Test
    fun migratingAllGoesBack() {
        show()
        compose.onNodeWithContentDescription("Migrate").performClick()
        compose.waitForIdle()
        click("Migrate")
        compose.waitForLabel("base")
        coVerify { harness.migrateManga(any(), any(), true, any()) }
    }

    @Test
    fun copyingCanBeCancelled() {
        show()
        compose.onNodeWithContentDescription("Copy").performClick()
        compose.waitForIdle()
        click("Cancel")
        compose.hasLabel("Cancel") shouldBe false
        compose.onNodeWithContentDescription("Copy").performClick()
        compose.waitForIdle()
        click("Copy")
        compose.waitForLabel("base")
        coVerify { harness.migrateManga(any(), any(), false, any()) }
    }

    @Test
    fun progressCanBeCancelled() {
        val gate = CompletableDeferred<Unit>()
        coEvery { harness.migrateManga(any(), any(), any(), any()) } coAnswers { gate.await() }
        show()
        compose.onNodeWithContentDescription("Migrate").performClick()
        compose.waitForIdle()
        click("Migrate")
        // The confirmation has its own Cancel: wait until the progress dialog has replaced it.
        eventually { !compose.hasLabel("Migrate 2 entries?") }
        click("Cancel")
        eventually { !compose.hasLabel("Cancel") }
        compose.hasLabel("base") shouldBe false
    }

    @Test
    fun backAsksBeforeLeaving() {
        show()
        compose.runOnIdle { back.pressBack() }
        click("Cancel")
        compose.hasLabel("Stop migrating?") shouldBe false
        compose.runOnIdle { back.pressBack() }
        click("Stop")
        compose.waitForLabel("base")
    }

    // As after a manual search: the override is picked up when the screen comes back.
    private fun returnWithMatch(screen: MigrationListScreen, target: Long) {
        click("Source 2")
        compose.waitForLabel("opened:MangaScreen")
        screen.addMatchOverride(current = 2L, target = target)
        compose.runOnIdle { checkNotNull(navigator).pop() }
        compose.waitForIdle()
    }

    @Test
    fun aManualMatchIsUsed() {
        val screen = MigrationListScreen(listOf(1L, 2L), null)
        show(screen)
        returnWithMatch(screen, target = 3L)
        compose.waitForLabel("Source 3")
        compose.hasLabel("No alternatives found") shouldBe false
    }

    @Test
    fun aMatchWithoutChaptersWarns() {
        val screen = MigrationListScreen(listOf(1L, 2L), "extra")
        show(screen)
        returnWithMatch(screen, target = 300L)
        eventually { ShadowToast.getTextOfLatestToast() != null }
        ShadowToast.getTextOfLatestToast() shouldBe "No chapters found, this entry cannot be used for migration"
    }

    private fun menu(row: Int, label: String) {
        compose.onAllNodes(bareButton)[row].performClick()
        click(label)
    }

    @Test
    fun rowMenusSearchManually() {
        show()
        menu(row = 1, label = "Search manually")
        compose.waitForLabel("opened:MigrateSearchScreen")
    }

    @Test
    fun rowMenusMigrateOrSkip() {
        show()
        menu(row = 0, label = "Copy now")
        coVerify(timeout = 5_000) { harness.migrateManga(any(), any(), false, any()) }
        eventually { !compose.hasLabel("Source 1") }
        menu(row = 0, label = "Don't migrate")
        eventually { !compose.hasLabel("Source 2") }
    }
}
