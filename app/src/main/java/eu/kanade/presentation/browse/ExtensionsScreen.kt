package eu.kanade.presentation.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.presentation.util.rememberInstallPermissionState
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsScreenModel
import eu.kanade.tachiyomi.util.system.launchInstallPermissionRequest
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus

@Composable
internal fun ExtensionScreen(
    state: ExtensionsScreenModel.State,
    contentPadding: PaddingValues,
    searchQuery: String?,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onInstallExtension: (Extension.Available) -> Unit,
    onUninstallExtension: (Extension) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onTrustExtension: (Extension.Untrusted) -> Unit,
    onOpenExtension: (Extension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
    onRefresh: () -> Unit,
) {
    val navigator = LocalNavigator.currentOrThrow

    PullRefresh(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh,
        enabled = !state.isLoading,
    ) {
        when {
            state.isLoading -> {
                LoadingScreen(Modifier.padding(contentPadding))
            }
            state.isEmpty -> {
                val msg = if (!searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.empty_screen
                }
                EmptyScreen(
                    msg,
                    modifier = Modifier.padding(contentPadding),
                    actions = listOf(
                        EmptyScreenAction(
                            stringRes = MR.strings.extensionStores,
                            icon = Icons.Outlined.Settings,
                            onClick = { navigator.push(ExtensionStoresScreen()) },
                        ),
                    ),
                )
            }
            else -> {
                ExtensionContent(
                    state = state,
                    contentPadding = contentPadding,
                    onLongClickItem = onLongClickItem,
                    onClickItemCancel = onClickItemCancel,
                    onOpenWebView = onOpenWebView,
                    onInstallExtension = onInstallExtension,
                    onUninstallExtension = onUninstallExtension,
                    onUpdateExtension = onUpdateExtension,
                    onTrustExtension = onTrustExtension,
                    onOpenExtension = onOpenExtension,
                    onClickUpdateAll = onClickUpdateAll,
                )
            }
        }
    }
}

@Composable
private fun ExtensionContent(
    state: ExtensionsScreenModel.State,
    contentPadding: PaddingValues,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onInstallExtension: (Extension.Available) -> Unit,
    onUninstallExtension: (Extension) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onTrustExtension: (Extension.Untrusted) -> Unit,
    onOpenExtension: (Extension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
) {
    val context = LocalContext.current
    var trustState by remember { mutableStateOf<Extension.Untrusted?>(null) }
    val installGranted = rememberInstallPermissionState(initialValue = true)

    FastScrollLazyColumn(
        contentPadding = contentPadding + topSmallPaddingValues,
    ) {
        if (!installGranted && state.installer?.requiresSystemPermission == true) {
            item(key = "extension-permissions-warning") {
                WarningBanner(
                    textRes = MR.strings.ext_permission_install_apps_warning,
                    modifier = Modifier.clickable {
                        context.launchInstallPermissionRequest()
                    },
                )
            }
        }

        state.items.forEach { (header, items) ->
            extensionHeaderItem(header, onClickUpdateAll)
            extensionItems(
                items = items,
                onLongClickItem = onLongClickItem,
                onClickItemCancel = onClickItemCancel,
                onOpenWebView = onOpenWebView,
                onInstallExtension = onInstallExtension,
                onUpdateExtension = onUpdateExtension,
                onOpenExtension = onOpenExtension,
                onTrust = { trustState = it },
            )
        }
    }
    if (trustState != null) {
        ExtensionTrustDialog(
            onClickConfirm = {
                onTrustExtension(trustState!!)
                trustState = null
            },
            onClickDismiss = {
                onUninstallExtension(trustState!!)
                trustState = null
            },
            onDismissRequest = {
                trustState = null
            },
        )
    }
}

// One row per extension; tapping an untrusted one opens the trust dialog instead of acting on it.
private fun LazyListScope.extensionItems(
    items: List<ExtensionUiModel.Item>,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onInstallExtension: (Extension.Available) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onOpenExtension: (Extension.Installed) -> Unit,
    onTrust: (Extension.Untrusted) -> Unit,
) {
    items(
        items = items,
        contentType = { "item" },
        key = { item ->
            when (item.extension) {
                is Extension.Untrusted -> "extension-untrusted-${item.hashCode()}"
                is Extension.Installed -> "extension-installed-${item.hashCode()}"
                is Extension.Available -> "extension-available-${item.hashCode()}"
            }
        },
    ) { item ->
        ExtensionItem(
            modifier = Modifier.animateItemFastScroll(),
            item = item,
            onClickItem = { dispatch(it, onInstallExtension, onOpenExtension, onTrust) },
            onLongClickItem = onLongClickItem,
            onClickItemSecondaryAction = { dispatch(it, onOpenWebView, onOpenExtension) },
            onClickItemCancel = onClickItemCancel,
            // The primary action on an installed extension is "update" when one is pending, "open" otherwise.
            onClickItemAction = {
                dispatch(
                    it,
                    onInstallExtension,
                    { installed ->
                        if (installed.hasUpdate) onUpdateExtension(installed) else onOpenExtension(installed)
                    },
                    onTrust,
                )
            },
        )
    }
}

private fun dispatch(
    extension: Extension,
    onAvailable: (Extension.Available) -> Unit,
    onInstalled: (Extension.Installed) -> Unit,
    onUntrusted: (Extension.Untrusted) -> Unit = {},
) {
    when (extension) {
        is Extension.Available -> onAvailable(extension)
        is Extension.Installed -> onInstalled(extension)
        is Extension.Untrusted -> onUntrusted(extension)
    }
}

// A section header; the "updates pending" one carries the update-all button.
private fun LazyListScope.extensionHeaderItem(header: ExtensionUiModel.Header, onClickUpdateAll: () -> Unit) {
    item(
        contentType = "header",
        key = "extensionHeader-${header.hashCode()}",
    ) {
        when (header) {
            is ExtensionUiModel.Header.Resource -> {
                val action: @Composable RowScope.() -> Unit =
                    if (header.textRes == MR.strings.ext_updates_pending) {
                        {
                            Button(onClick = { onClickUpdateAll() }) {
                                Text(
                                    text = stringResource(MR.strings.ext_update_all),
                                    style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onPrimary),
                                )
                            }
                        }
                    } else {
                        {}
                    }
                ExtensionHeader(
                    textRes = header.textRes,
                    modifier = Modifier.animateItemFastScroll(),
                    content = action,
                )
            }
            is ExtensionUiModel.Header.Text -> {
                ExtensionHeader(
                    text = header.text,
                    modifier = Modifier.animateItemFastScroll(),
                )
            }
        }
    }
}

@Composable
private fun ExtensionHeader(
    textRes: StringResource,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit = {},
) {
    ExtensionHeader(
        text = stringResource(textRes),
        modifier = modifier,
        content = content,
    )
}

@Composable
private fun ExtensionHeader(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .weight(1f),
            style = MaterialTheme.typography.header,
        )
        content()
    }
}

@Composable
private fun ExtensionTrustDialog(
    onClickConfirm: () -> Unit,
    onClickDismiss: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = stringResource(MR.strings.untrusted_extension))
        },
        text = {
            Text(text = stringResource(MR.strings.untrusted_extension_message))
        },
        confirmButton = {
            TextButton(onClick = onClickConfirm) {
                Text(text = stringResource(MR.strings.ext_trust))
            }
        },
        dismissButton = {
            TextButton(onClick = onClickDismiss) {
                Text(text = stringResource(MR.strings.ext_uninstall))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}
