package eu.kanade.tachiyomi.ui.download

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen

internal object DownloadQueueScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { DownloadQueueScreenModel() }
        val downloadList by screenModel.state.collectAsState()
        val downloadCount by remember {
            derivedStateOf { downloadList.sumOf { it.subItems.size } }
        }

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        var fabExpanded by remember { mutableStateOf(true) }
        val nestedScrollConnection = remember {
            // All this lines just for fab state :/
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    fabExpanded = available.y >= 0
                    return scrollBehavior.nestedScrollConnection.onPreScroll(available, source)
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                    scrollBehavior.nestedScrollConnection.onPostScroll(consumed, available, source)

                override suspend fun onPreFling(available: Velocity): Velocity =
                    scrollBehavior.nestedScrollConnection.onPreFling(available)

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                    scrollBehavior.nestedScrollConnection.onPostFling(consumed, available)
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    titleContent = { DownloadQueueTitle(downloadCount) },
                    navigateUp = navigator::pop,
                    actions = { if (downloadList.isNotEmpty()) DownloadQueueActions(screenModel) },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                PauseResumeFab(screenModel, expanded = fabExpanded, visible = downloadList.isNotEmpty())
            },
        ) { contentPadding ->
            if (downloadList.isEmpty()) {
                EmptyScreen(
                    stringRes = MR.strings.information_no_downloads,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
                    DownloadQueueList(screenModel, downloadList, contentPadding)
                }
            }
        }
    }
}
