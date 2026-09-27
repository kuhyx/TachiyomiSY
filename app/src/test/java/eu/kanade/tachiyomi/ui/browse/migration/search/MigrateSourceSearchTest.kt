package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performTouchInput
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.clickLabel
import eu.kanade.tachiyomi.ui.base.labelShown
import eu.kanade.tachiyomi.ui.base.readObjectMember
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseScreenRig
import eu.kanade.tachiyomi.ui.browse.source.browse.listed
import eu.kanade.tachiyomi.ui.home.HomeScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mihon.feature.migration.list.MigrationListScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.source.local.LocalSource

/** Searching one source for the entry to migrate to, from the manga screen or the migration list. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class MigrateSourceSearchTest {
    @get:Rule
    val compose = createComposeRule()

    private val rig = BrowseScreenRig(compose)
    private val current = listed(7L).copy(ogTitle = "Old one")
    private val screen = MigrateSourceSearchScreen(current, 1L, "q")
    private val receiver = CoroutineScope(Dispatchers.Default)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() {
        receiver.cancel()
        rig.stop()
    }

    @Test
    fun waitsForSources() {
        rig.loaded.value = false
        rig.show(screen)
        compose.labelShown("Manga 1") shouldBe false
        rig.loaded.value = true
        rig.await("Manga 1")
    }

    @Test
    fun pickShowsTheMigrateDialog() {
        rig.show(screen)
        rig.await("Manga 1")
        compose.clickLabel("Manga 1")
        rig.await("Show entry")
        compose.clickLabel("Show entry")
        rig.await("opened:MangaScreen")
    }

    @Test
    fun migratingReturnsHome() {
        // The finished migration asks the home screen for the browse tab; stand in for it.
        val tabs = readObjectMember(HomeScreen::class, "openTabEvent") as Channel<*>
        receiver.launch { tabs.receive() }
        rig.show(screen)
        rig.await("Manga 1")
        compose.clickLabel("Manga 1")
        rig.await("Migrate")
        compose.clickLabel("Migrate")
        rig.await("opened:MangaScreen")
    }

    @Test
    fun migrationListTakesTheMatch() {
        val list = MigrationListScreen(listOf(7L), null)
        lateinit var navigator: Navigator
        compose.setContent {
            MaterialTheme {
                Navigator(listOf(list, screen)) {
                    navigator = it
                    screen.Content()
                }
            }
        }
        rig.await("Manga 1")
        compose.clickLabel("Manga 1")
        compose.waitForIdle()
        navigator.lastItem shouldBe list
    }

    @Test
    fun longPressOpensManga() {
        rig.show(screen)
        rig.await("Manga 1")
        compose.onAllNodes(hasText("Manga 1")).onLast().performTouchInput { longClick() }
        rig.await("opened:MangaScreen")
    }

    @Test
    fun filterSheetSearches() {
        rig.harness.filters = { FilterList(object : Filter.CheckBox("Check") {}) }
        rig.show(screen)
        rig.await("Filter")
        compose.clickLabel("Filter")
        rig.await("Reset")
        compose.clickLabel("Reset")
        compose.clickLabel("Filter")
        rig.await("Manga 1")
    }

    @Test
    fun toolbarSearchesAndCloses() {
        rig.show(screen)
        rig.await("Manga 1")
        compose.clickLabel("Navigate up")
        compose.waitForIdle()
    }

    @Test
    fun emptyWebSourceOffersWebView() {
        val web = mockk<HttpSource>(relaxed = true) {
            every { id } returns 1L
            every { name } returns "Web"
            every { getFilterList() } returns FilterList()
        }
        every { rig.harness.sourceManager.getOrStub(1L) } returns web
        rig.items = emptyList()
        rig.show(screen)
        rig.await("Open in WebView")
        compose.clickLabel("Open in WebView")
        rig.await("opened:WebViewScreen")
    }

    @Test
    fun emptyPlainSourceOffersHelp() {
        rig.items = emptyList()
        rig.show(screen)
        rig.await("Open in WebView")
        compose.clickLabel("Open in WebView")
        compose.clickLabel("Help")
        compose.labelShown("opened:WebViewScreen") shouldBe false
    }

    @Test
    fun emptyLocalSourceHelps() {
        val local = mockk<LocalSource>(relaxed = true) { every { id } returns 1L }
        every { rig.harness.sourceManager.getOrStub(1L) } returns local
        rig.items = emptyList()
        rig.show(screen)
        rig.await("Local source guide")
        compose.clickLabel("Local source guide")
        local.shouldBeInstanceOf<LocalSource>()
    }
}
