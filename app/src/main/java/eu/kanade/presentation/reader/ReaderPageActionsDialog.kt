package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AdaptiveSheet
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.ActionButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

private data class PageAction(val title: StringResource, val icon: ImageVector, val onClick: () -> Unit)

@Composable
internal fun ReaderPageActionsDialog(
    onDismissRequest: () -> Unit,
    // SY -->
    onSetAsCover: (useExtraPage: Boolean) -> Unit,
    onShare: (copy: Boolean, useExtraPage: Boolean) -> Unit,
    onSave: (useExtraPage: Boolean) -> Unit,
    onShareCombined: (copy: Boolean) -> Unit,
    onSaveCombined: () -> Unit,
    hasExtraPage: Boolean,
    // SY <--
) {
    var showSetCoverDialog by remember { mutableStateOf(false) }
    // SY -->
    var useExtraPage by remember { mutableStateOf(false) }
    // SY <--

    // Every action closes the sheet except "set as cover", which asks for confirmation first.
    val callbacks = PageCallbacks(
        onSetCover = { extraPage ->
            useExtraPage = extraPage
            showSetCoverDialog = true
        },
        onShare = { copy, extraPage ->
            onShare(copy, extraPage)
            onDismissRequest()
        },
        onSave = { extraPage ->
            onSave(extraPage)
            onDismissRequest()
        },
    )

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            // SY -->
            ActionRow(callbacks.pageActions(false, if (hasExtraPage) FIRST_PAGE_TITLES else SINGLE_PAGE_TITLES))
            if (hasExtraPage) {
                ActionRow(callbacks.pageActions(true, SECOND_PAGE_TITLES))
                ActionRow(
                    listOf(
                        PageAction(SYMR.strings.action_copy_combined_page, Icons.Outlined.ContentCopy) {
                            onShareCombined(true)
                            onDismissRequest()
                        },
                        PageAction(SYMR.strings.action_share_combined_page, Icons.Outlined.Share) {
                            onShareCombined(false)
                            onDismissRequest()
                        },
                        PageAction(SYMR.strings.action_save_combined_page, Icons.Outlined.Save) {
                            onSaveCombined()
                            onDismissRequest()
                        },
                    ),
                )
            }
            // SY <--
        }
    }

    if (showSetCoverDialog) {
        SetCoverDialog(
            onConfirm = {
                // SY -->
                onSetAsCover(useExtraPage)
                showSetCoverDialog = false
                useExtraPage = false
                // SY <--
            },
            onDismiss = { showSetCoverDialog = false },
        )
    }
}

// Set-cover / copy / share / save titles for one page of the spread.
private data class PageTitles(
    val cover: StringResource,
    val copy: StringResource,
    val share: StringResource,
    val save: StringResource,
)

private val SINGLE_PAGE_TITLES = PageTitles(
    MR.strings.set_as_cover,
    MR.strings.action_copy_to_clipboard,
    MR.strings.action_share,
    MR.strings.action_save,
)
private val FIRST_PAGE_TITLES = PageTitles(
    SYMR.strings.action_set_first_page_cover,
    SYMR.strings.action_copy_first_page,
    SYMR.strings.action_share_first_page,
    SYMR.strings.action_save_first_page,
)
private val SECOND_PAGE_TITLES = PageTitles(
    SYMR.strings.action_set_second_page_cover,
    SYMR.strings.action_copy_second_page,
    SYMR.strings.action_share_second_page,
    SYMR.strings.action_save_second_page,
)

private class PageCallbacks(
    val onSetCover: (extraPage: Boolean) -> Unit,
    val onShare: (copy: Boolean, extraPage: Boolean) -> Unit,
    val onSave: (extraPage: Boolean) -> Unit,
) {
    fun pageActions(extraPage: Boolean, titles: PageTitles) = listOf(
        PageAction(titles.cover, Icons.Outlined.Photo) { onSetCover(extraPage) },
        PageAction(titles.copy, Icons.Outlined.ContentCopy) { onShare(true, extraPage) },
        PageAction(titles.share, Icons.Outlined.Share) { onShare(false, extraPage) },
        PageAction(titles.save, Icons.Outlined.Save) { onSave(extraPage) },
    )
}

@Composable
private fun ActionRow(actions: List<PageAction>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        actions.forEach { action ->
            ActionButton(
                modifier = Modifier.weight(1f),
                title = stringResource(action.title),
                icon = action.icon,
                onClick = action.onClick,
            )
        }
    }
}

@Composable
private fun SetCoverDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        text = {
            Text(stringResource(MR.strings.confirm_set_image_as_cover))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        onDismissRequest = onDismiss,
    )
}
