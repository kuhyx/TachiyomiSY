package eu.kanade.presentation.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun AppBar(
    title: String?,

    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    // Text
    subtitle: String? = null,
    // Up button
    navigateUp: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    // Menu
    actions: @Composable RowScope.() -> Unit = {},
    // Action mode
    actionModeCounter: Int = 0,
    onCancelActionMode: () -> Unit = {},
    actionModeActions: @Composable RowScope.() -> Unit = {},

    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val isActionMode by remember(actionModeCounter) {
        derivedStateOf { actionModeCounter > 0 }
    }

    AppBar(
        modifier = modifier,
        backgroundColor = backgroundColor,
        titleContent = {
            if (isActionMode) {
                AppBarTitle(actionModeCounter.toString())
            } else {
                AppBarTitle(title, subtitle = subtitle)
            }
        },
        navigateUp = navigateUp,
        navigationIcon = navigationIcon,
        actions = {
            if (isActionMode) {
                actionModeActions()
            } else {
                actions()
            }
        },
        isActionMode = isActionMode,
        onCancelActionMode = onCancelActionMode,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
internal fun AppBar(
    // Title
    titleContent: @Composable () -> Unit,

    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    // Up button
    navigateUp: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    // Menu
    actions: @Composable RowScope.() -> Unit = {},
    // Action mode
    isActionMode: Boolean = false,
    onCancelActionMode: () -> Unit = {},

    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    Column(
        modifier = modifier,
    ) {
        TopAppBar(
            navigationIcon = {
                if (isActionMode) {
                    IconButton(onClick = onCancelActionMode) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(MR.strings.action_cancel),
                        )
                    }
                } else {
                    navigateUp?.let {
                        IconButton(onClick = it) {
                            UpIcon(navigationIcon = navigationIcon)
                        }
                    }
                }
            },
            title = titleContent,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = backgroundColor ?: MaterialTheme.colorScheme.surfaceColorAtElevation(
                    elevation = if (isActionMode) 3.dp else 0.dp,
                ),
            ),
            scrollBehavior = scrollBehavior,
        )
    }
}

@Composable
internal fun AppBarTitle(
    title: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier) {
        title?.let {
            Text(
                text = it,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(
                    repeatDelayMillis = 2_000,
                ),
            )
        }
    }
}

@Composable
internal fun UpIcon(
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
) {
    val icon = navigationIcon
        ?: Icons.AutoMirrored.Outlined.ArrowBack
    Icon(
        imageVector = icon,
        contentDescription = stringResource(MR.strings.action_bar_up_description),
        modifier = modifier,
    )
}

internal sealed interface AppBar {
    sealed interface AppBarAction

    data class Action(
        val title: String,
        val icon: ImageVector,
        val iconTint: Color? = null,
        val onClick: () -> Unit,
        val enabled: Boolean = true,
    ) : AppBarAction

    data class OverflowAction(
        val title: String,
        val onClick: () -> Unit,
    ) : AppBarAction
}
