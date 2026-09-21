package eu.kanade.presentation.browse

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.browse.components.BaseBrowseItem
import eu.kanade.presentation.browse.components.ExtensionIcon
import eu.kanade.presentation.manga.components.DotSeparatorNoSpaceText
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
internal fun ExtensionItem(
    item: ExtensionUiModel.Item,
    onClickItem: (Extension) -> Unit,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onClickItemAction: (Extension) -> Unit,
    onClickItemSecondaryAction: (Extension) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (extension, installStep) = item
    BaseBrowseItem(
        modifier = modifier
            .combinedClickable(
                onClick = { onClickItem(extension) },
                onLongClick = { onLongClickItem(extension) },
            ),
        onClickItem = { onClickItem(extension) },
        onLongClickItem = { onLongClickItem(extension) },
        icon = {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                val idle = installStep.isCompleted()
                if (!idle) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 2.dp,
                    )
                }

                val padding by animateDpAsState(targetValue = if (idle) 0.dp else 8.dp)
                ExtensionIcon(
                    extension = extension,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(padding),
                )
            }
        },
        action = {
            ExtensionItemActions(
                extension = extension,
                installStep = installStep,
                onClickItemCancel = onClickItemCancel,
                onClickItemAction = onClickItemAction,
                onClickItemSecondaryAction = onClickItemSecondaryAction,
            )
        },
    ) {
        ExtensionItemContent(
            extension = extension,
            installStep = installStep,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExtensionItemContent(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = MaterialTheme.padding.medium),
    ) {
        Text(
            text = extension.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        // Won't look good but it's not like we can ellipsize overflowing content
        FlowRow(
            modifier = Modifier.secondaryItemAlpha(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                val parts = subtitleParts(extension, installStep)
                parts.forEachIndexed { index, (text, isWarning) ->
                    if (index > 0) DotSeparatorNoSpaceText()
                    if (isWarning) {
                        Text(
                            text = text,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Text(text = text)
                    }
                }
            }
        }
    }
}

// Language, version, a warning (untrusted / obsolete / redundant / NSFW), "private", and the install step.
@Composable
private fun subtitleParts(extension: Extension, installStep: InstallStep): List<Pair<String, Boolean>> {
    val installed = extension as? Extension.Installed
    return listOfNotNull(
        installed?.lang?.takeIf { it.isNotEmpty() }
            ?.let { LocaleHelper.getSourceDisplayName(it, LocalContext.current) to false },
        extension.versionName.takeIf { it.isNotEmpty() }?.let { it to false },
        extension.warning()?.let { stringResource(it).uppercase() to true },
        installed?.takeIf { !it.isShared }
            ?.let { stringResource(MR.strings.ext_installer_private) to false },
        installStepLabels[installStep]?.let { stringResource(it) to false },
    )
}

private fun Extension.warning(): StringResource? = when {
    this is Extension.Untrusted -> MR.strings.ext_untrusted
    this is Extension.Installed && isObsolete -> MR.strings.ext_obsolete
    // SY -->
    this is Extension.Installed && isRedundant -> SYMR.strings.ext_redundant
    // SY <--
    isNsfw -> MR.strings.ext_nsfw_short
    else -> null
}

// Only the in-progress steps get a label; completed steps show nothing.
private val installStepLabels = mapOf(
    InstallStep.Pending to MR.strings.ext_pending,
    InstallStep.Downloading to MR.strings.ext_downloading,
    InstallStep.Installing to MR.strings.ext_installing,
)

@Composable
private fun ExtensionItemActions(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
    onClickItemCancel: (Extension) -> Unit = {},
    onClickItemAction: (Extension) -> Unit = {},
    onClickItemSecondaryAction: (Extension) -> Unit = {},
) {
    val isIdle = installStep.isCompleted()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        when {
            !isIdle -> {
                ActionIcon(Icons.Outlined.Close, MR.strings.action_cancel) { onClickItemCancel(extension) }
            }
            installStep == InstallStep.Error -> {
                ActionIcon(Icons.Outlined.Refresh, MR.strings.action_retry) { onClickItemAction(extension) }
            }
            installStep == InstallStep.Idle -> {
                IdleActions(extension, onClickItemAction, onClickItemSecondaryAction)
            }
        }
    }
}

@Composable
private fun IdleActions(
    extension: Extension,
    onClickItemAction: (Extension) -> Unit,
    onClickItemSecondaryAction: (Extension) -> Unit,
) {
    when (extension) {
        is Extension.Installed -> {
            ActionIcon(Icons.Outlined.Settings, MR.strings.action_settings) { onClickItemSecondaryAction(extension) }
            if (extension.hasUpdate) {
                ActionIcon(Icons.Outlined.GetApp, MR.strings.ext_update) { onClickItemAction(extension) }
            }
        }
        is Extension.Untrusted -> {
            ActionIcon(Icons.Outlined.VerifiedUser, MR.strings.ext_trust) { onClickItemAction(extension) }
        }
        is Extension.Available -> {
            if (extension.sources.isNotEmpty()) {
                ActionIcon(Icons.Outlined.Public, MR.strings.action_open_in_web_view) {
                    onClickItemSecondaryAction(extension)
                }
            }
            ActionIcon(Icons.Outlined.GetApp, MR.strings.ext_install) { onClickItemAction(extension) }
        }
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, description: StringResource, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(description),
        )
    }
}
