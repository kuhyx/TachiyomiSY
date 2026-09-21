package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus

// One row per extension; tapping an untrusted one opens the trust dialog instead of acting on it.
internal fun LazyListScope.extensionItems(
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

internal fun dispatch(
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
internal fun LazyListScope.extensionHeaderItem(header: ExtensionUiModel.Header, onClickUpdateAll: () -> Unit) {
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
internal fun ExtensionHeader(
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
internal fun ExtensionHeader(
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
internal fun ExtensionTrustDialog(
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
