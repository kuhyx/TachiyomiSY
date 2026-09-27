package eu.kanade.tachiyomi.ui.browse.extension.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.ExtensionDetailsScreen
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tachiyomi.presentation.core.screens.LoadingScreen

internal data class ExtensionDetailsScreen(
    private val pkgName: String,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val screenModel = rememberScreenModel { ExtensionDetailsScreenModel(pkgName = pkgName, context = context) }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        // Collected before the loading check: "uninstalled" can arrive while the screen still loads.
        LaunchedEffect(Unit) {
            screenModel.events
                .onEach { event ->
                    // Exhaustive, so a new event type is a compile error rather than a silent pop.
                    when (event) {
                        ExtensionDetailsEvent.Uninstalled -> navigator.pop()
                    }
                }
                .launchIn(this)
        }

        if (state.isLoading) {
            LoadingScreen()
            return
        }

        ExtensionDetailsScreen(
            navigateUp = navigator::pop,
            state = state,
            onClickSourcePreferences = { navigator.push(SourcePreferencesScreen(it)) },
            onClickEnableAll = { screenModel.toggleSources(true) },
            onClickDisableAll = { screenModel.toggleSources(false) },
            onClickClearCookies = screenModel::clearCookies,
            onClickUninstall = screenModel::uninstallExtension,
            onClickSource = screenModel::toggleSource,
            onClickIncognito = screenModel::toggleIncognito,
        )
    }
}
