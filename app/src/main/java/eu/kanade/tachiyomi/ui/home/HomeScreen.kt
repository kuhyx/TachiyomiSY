package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.core.preference.asState
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.presentation.core.components.material.Scaffold
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal object HomeScreen : Screen() {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    internal val showBottomNavEvent = Channel<Boolean>()

    @Suppress("ConstPropertyName")
    private const val TabFadeDuration = 200

    @Suppress("ConstPropertyName")
    private const val TabNavigatorKey = "HomeTabs"

    internal val TABS = listOf(
        LibraryTab,
        UpdatesTab,
        HistoryTab,
        BrowseTab,
        MoreTab,
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // SY -->
        val scope = rememberCoroutineScope()
        val alwaysShowLabel by remember {
            Injekt.get<UiPreferences>().bottomBarLabels.asState(scope)
        }
        // SY <--

        TabNavigator(
            tab = LibraryTab,
            key = TabNavigatorKey,
        ) { tabNavigator ->
            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Scaffold(
                    startBar = { if (isTabletUi()) HomeNavigationRail(alwaysShowLabel) },
                    bottomBar = { if (!isTabletUi()) HomeNavigationBar(alwaysShowLabel) },
                    contentWindowInsets = WindowInsets(0),
                ) { contentPadding ->
                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .consumeWindowInsets(contentPadding),
                    ) {
                        AnimatedContent(
                            targetState = tabNavigator.current,
                            transitionSpec = {
                                materialFadeThroughIn(initialScale = 1f, durationMillis = TabFadeDuration) togetherWith
                                    materialFadeThroughOut(durationMillis = TabFadeDuration)
                            },
                            label = "tabContent",
                        ) {
                            tabNavigator.saveableState(key = "currentTab", it) {
                                it.Content()
                            }
                        }
                    }
                }
            }

            val goToLibraryTab = { tabNavigator.current = LibraryTab }

            BackHandler(enabled = tabNavigator.current != LibraryTab, onBack = goToLibraryTab)

            TabRequestEffects(tabNavigator, navigator, goToLibraryTab)
        }
    }

    // Tab switches requested from outside the home screen (search, deep links, notifications). [navigator] is the
    // outer one: inside the TabNavigator, LocalNavigator is the tab navigator, which cannot hold a pushed screen.
    @Composable
    private fun TabRequestEffects(tabNavigator: TabNavigator, navigator: Navigator, goToLibraryTab: () -> Unit) {
        // mapLatest/launchIn as collectLatest does: the event channels never close, so nothing follows a collect.
        LaunchedEffect(Unit) {
            librarySearchEvent.receiveAsFlow()
                .mapLatest {
                    goToLibraryTab()
                    LibraryTab.search(it)
                }
                .launchIn(this)
            openTabEvent.receiveAsFlow()
                .mapLatest { openTab(it, tabNavigator, navigator) }
                .launchIn(this)
        }
    }

    suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    sealed interface Tab {
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data object Updates : Tab
        data object History : Tab
        data class Browse(val toExtensions: Boolean = false) : Tab
        data class More(val toDownloads: Boolean) : Tab
    }
}

// Switches to the requested tab, then pushes the screen the request points at inside it (if any).
internal fun openTab(request: HomeScreen.Tab, tabNavigator: TabNavigator, navigator: Navigator) {
    tabNavigator.current = request.target()
    when {
        request is HomeScreen.Tab.Library && request.mangaIdToOpen != null -> {
            navigator.push(MangaScreen(request.mangaIdToOpen))
        }
        request is HomeScreen.Tab.More && request.toDownloads -> {
            navigator.push(DownloadQueueScreen)
        }
    }
}

private fun HomeScreen.Tab.target(): eu.kanade.presentation.util.Tab = when (this) {
    is HomeScreen.Tab.Library -> LibraryTab
    HomeScreen.Tab.Updates -> UpdatesTab
    HomeScreen.Tab.History -> HistoryTab
    is HomeScreen.Tab.Browse -> BrowseTab.also { if (toExtensions) it.showExtension() }
    is HomeScreen.Tab.More -> MoreTab
}
