package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsScreenModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

// The NSFW badge shares its row with the version; the badge gets the wider column.

@Composable
internal fun ExtensionDetailsScreen(
    navigateUp: () -> Unit,
    state: ExtensionDetailsScreenModel.State,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickEnableAll: () -> Unit,
    onClickDisableAll: () -> Unit,
    onClickClearCookies: () -> Unit,
    onClickUninstall: () -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    onClickIncognito: (Boolean) -> Unit,
) {
    val repoUrl = remember(state.extension) { repoUrl(state.extension?.store?.indexUrl.orEmpty()) }

    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.label_extension_info),
                navigateUp = navigateUp,
                actions = {
                    AppBarActions(
                        actions = detailsActions(
                            repoUrl = repoUrl,
                            onClickEnableAll = onClickEnableAll,
                            onClickDisableAll = onClickDisableAll,
                            onClickClearCookies = onClickClearCookies,
                        ),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        if (state.extension == null) {
            EmptyScreen(
                MR.strings.empty_screen,
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            ExtensionDetails(
                contentPadding = paddingValues,
                extension = state.extension,
                sources = state.sources,
                incognitoMode = state.isIncognito,
                onClickSourcePreferences = onClickSourcePreferences,
                onClickUninstall = onClickUninstall,
                onClickSource = onClickSource,
                onClickIncognito = onClickIncognito,
            )
        }
    }
}

// GitHub-hosted repos link to the repo page; anything else links to the raw index URL.
private fun repoUrl(indexUrl: String): String? {
    val regex = """https://raw.githubusercontent.com/(.+?)/(.+?)/.+""".toRegex()
    return regex.find(indexUrl)
        ?.let {
            val (user, repo) = it.destructured
            "https://github.com/$user/$repo"
        }
        ?: indexUrl.ifEmpty { null }
}

@Composable
private fun detailsActions(
    repoUrl: String?,
    onClickEnableAll: () -> Unit,
    onClickDisableAll: () -> Unit,
    onClickClearCookies: () -> Unit,
): List<AppBar.AppBarAction> {
    val uriHandler = LocalUriHandler.current
    return listOfNotNull(
        repoUrl?.let {
            AppBar.Action(
                title = stringResource(MR.strings.action_open_repo),
                icon = Icons.AutoMirrored.Outlined.Launch,
                onClick = { uriHandler.openUri(it) },
            )
        },
        AppBar.OverflowAction(
            title = stringResource(MR.strings.action_enable_all),
            onClick = onClickEnableAll,
        ),
        AppBar.OverflowAction(
            title = stringResource(MR.strings.action_disable_all),
            onClick = onClickDisableAll,
        ),
        AppBar.OverflowAction(
            title = stringResource(MR.strings.pref_clear_cookies),
            onClick = onClickClearCookies,
        ),
    )
}
