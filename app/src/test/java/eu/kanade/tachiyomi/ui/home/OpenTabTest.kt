package eu.kanade.tachiyomi.ui.home

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.tachiyomi.source.online.readMember
import eu.kanade.tachiyomi.ui.base.readObjectMember
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Test

/** Which tab each request switches to, and what it pushes over it. */
internal class OpenTabTest {
    private val tabs: TabNavigator = mockk(relaxed = true)
    private val navigator: Navigator = mockk(relaxed = true)

    @Test
    fun plainRequestsOnlySwitch() {
        openTab(HomeScreen.Tab.Library(), tabs, navigator)
        openTab(HomeScreen.Tab.Updates, tabs, navigator)
        openTab(HomeScreen.Tab.History, tabs, navigator)
        openTab(HomeScreen.Tab.Browse(), tabs, navigator)
        openTab(HomeScreen.Tab.More(toDownloads = false), tabs, navigator)
        verify {
            tabs.current = LibraryTab
            tabs.current = UpdatesTab
            tabs.current = HistoryTab
            tabs.current = BrowseTab
            tabs.current = MoreTab
        }
        verify(exactly = 0) { navigator.push(any<Screen>()) }
    }

    @Test
    fun libraryRequestPushesManga() {
        openTab(HomeScreen.Tab.Library(9L), tabs, navigator)
        val pushed = slot<Screen>()
        verify { navigator.push(capture(pushed)) }
        pushed.captured.readMember(MangaScreen::class, "mangaId") shouldBe 9L
    }

    @Test
    fun extensionsRequestShowsThem() {
        openTab(HomeScreen.Tab.Browse(toExtensions = true), tabs, navigator)
        verify { tabs.current = BrowseTab }
        // The request is buffered for the browse tab; taking it back also keeps it from leaking.
        val channel = readObjectMember(BrowseTab::class, "switchToExtensionTabChannel") as Channel<*>
        channel.tryReceive().isSuccess shouldBe true
    }

    @Test
    fun downloadsRequestPushesQueue() {
        openTab(HomeScreen.Tab.More(toDownloads = true), tabs, navigator)
        verify { tabs.current = MoreTab }
        verify { navigator.push(DownloadQueueScreen) }
    }
}
