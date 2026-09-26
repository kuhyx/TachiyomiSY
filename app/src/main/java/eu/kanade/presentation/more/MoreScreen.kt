package eu.kanade.presentation.more

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import tachiyomi.core.common.Constants
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    // SY -->
    showNavUpdates: Boolean,
    showNavHistory: Boolean,
    // SY <--
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
    onClickBatchAdd: () -> Unit,
    onClickUpdates: () -> Unit,
    onClickHistory: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold { contentPadding ->
        ScrollbarLazyColumn(
            modifier = Modifier.padding(contentPadding),
        ) {
            item {
                LogoHeader()
            }
            modeSwitches(
                downloadedOnly = downloadedOnly,
                onDownloadedOnlyChange = onDownloadedOnlyChange,
                incognitoMode = incognitoMode,
                onIncognitoModeChange = onIncognitoModeChange,
            )

            item { HorizontalDivider() }

            // SY --> Updates and history live here only when they are not bottom-nav tabs.
            linkItems(
                listOfNotNull(
                    Triple(MR.strings.label_recent_updates, Icons.Outlined.NewReleases, onClickUpdates)
                        .takeIf { !showNavUpdates },
                    Triple(MR.strings.label_recent_manga, Icons.Outlined.History, onClickHistory)
                        .takeIf { !showNavHistory },
                ),
            )
            // SY <--

            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_download_queue),
                    subtitle = downloadQueueSubtitle(downloadQueueStateProvider()),
                    icon = Icons.Outlined.GetApp,
                    onPreferenceClick = onClickDownloadQueue,
                )
            }
            linkItems(
                listOf(
                    Triple(MR.strings.categories, Icons.AutoMirrored.Outlined.Label, onClickCategories),
                    Triple(MR.strings.label_stats, Icons.Outlined.QueryStats, onClickStats),
                    Triple(MR.strings.label_data_storage, Icons.Outlined.Storage, onClickDataAndStorage),
                    // SY -->
                    Triple(SYMR.strings.eh_batch_add, Icons.AutoMirrored.Outlined.PlaylistAdd, onClickBatchAdd),
                    // SY <--
                ),
            )

            item { HorizontalDivider() }

            linkItems(
                listOf(
                    Triple(MR.strings.label_settings, Icons.Outlined.Settings, onClickSettings),
                    Triple(MR.strings.pref_category_about, Icons.Outlined.Info, onClickAbout),
                    Triple(MR.strings.label_help, Icons.AutoMirrored.Outlined.HelpOutline) {
                        uriHandler.openUri(Constants.URL_HELP)
                    },
                ),
            )
        }
    }
}

private fun LazyListScope.modeSwitches(
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
) {
    item {
        SwitchPreferenceWidget(
            title = stringResource(MR.strings.label_downloaded_only),
            subtitle = stringResource(MR.strings.downloaded_only_summary),
            icon = Icons.Outlined.CloudOff,
            checked = downloadedOnly,
            onCheckedChanged = onDownloadedOnlyChange,
        )
    }
    item {
        SwitchPreferenceWidget(
            title = stringResource(MR.strings.pref_incognito_mode),
            subtitle = stringResource(MR.strings.pref_incognito_mode_summary),
            icon = ImageVector.vectorResource(R.drawable.ic_glasses_24dp),
            checked = incognitoMode,
            onCheckedChanged = onIncognitoModeChange,
        )
    }
}

private fun LazyListScope.linkItems(links: List<Triple<StringResource, ImageVector, () -> Unit>>) {
    links.forEach { (title, icon, onClick) ->
        item {
            TextPreferenceWidget(
                title = stringResource(title),
                icon = icon,
                onPreferenceClick = onClick,
            )
        }
    }
}

@Composable
// Stopped is the `else`: an exhaustive sealed `when` keeps a dead "no match" arm.
private fun downloadQueueSubtitle(state: DownloadQueueState): String? = when (state) {
    is DownloadQueueState.Paused -> {
        val pending = state.pending
        if (pending == 0) {
            stringResource(MR.strings.paused)
        } else {
            "${stringResource(MR.strings.paused)} • ${
                pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)
            }"
        }
    }
    is DownloadQueueState.Downloading -> {
        val pending = state.pending
        pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)
    }
    else -> {
        null
    }
}
