package eu.kanade.tachiyomi.ui.home

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class HomeOpenTabTest {
    private val tabNavigator = mockk<TabNavigator>(relaxed = true)
    private val navigator = mockk<Navigator>(relaxed = true)

    @Test
    fun eachRequestSelectsItsTab() {
        openTab(HomeScreen.Tab.Library(), tabNavigator, navigator)
        verify { tabNavigator.current = LibraryTab }
        openTab(HomeScreen.Tab.Updates, tabNavigator, navigator)
        verify { tabNavigator.current = UpdatesTab }
        openTab(HomeScreen.Tab.History, tabNavigator, navigator)
        verify { tabNavigator.current = HistoryTab }
        openTab(HomeScreen.Tab.Browse(), tabNavigator, navigator)
        openTab(HomeScreen.Tab.Browse(toExtensions = true), tabNavigator, navigator)
        verify(exactly = 2) { tabNavigator.current = BrowseTab }
        openTab(HomeScreen.Tab.More(toDownloads = false), tabNavigator, navigator)
        verify { tabNavigator.current = MoreTab }
        verify(exactly = 0) { navigator.push(any<Screen>()) }
    }

    @Test
    fun moreCanOpenTheDownloads() {
        openTab(HomeScreen.Tab.More(toDownloads = true), tabNavigator, navigator)
        verify { navigator.push(DownloadQueueScreen) }
    }
}
