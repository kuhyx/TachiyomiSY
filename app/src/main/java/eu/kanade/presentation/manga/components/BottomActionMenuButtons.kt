package eu.kanade.presentation.manga.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.seconds

// A menu button wired to its long-press confirmation slot.
@Composable
internal fun RowScope.SlotButton(
    slots: ConfirmSlots,
    slot: Enum<*>,
    label: StringResource,
    icon: ImageVector,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null,
) {
    Button(
        title = stringResource(label),
        icon = icon,
        toConfirm = slots.confirm[slot.ordinal],
        onLongClick = { slots.onLongClickItem(slot.ordinal) },
        onClick = onClick,
        content = content,
    )
}

@Composable
internal fun rememberConfirmSlots(size: Int): ConfirmSlots {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val confirm = remember { List(size) { false }.toMutableStateList() }
    var resetJob by remember { mutableStateOf<Job?>(null) }
    return remember(confirm) {
        ConfirmSlots(confirm) { toConfirmIndex ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            confirm.indices.forEach { i -> confirm[i] = i == toConfirmIndex }
            resetJob?.cancel()
            resetJob = scope.launch {
                delay(1.seconds)
                // A cancelled delay throws, so reaching here means this job was not replaced.
                confirm[toConfirmIndex] = false
            }
        }
    }
}

@Composable
internal fun RowScope.Button(
    title: String,
    icon: ImageVector,
    toConfirm: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null,
) {
    val animatedWeight by animateFloatAsState(
        targetValue = if (toConfirm) 2f else 1f,
        label = "weight",
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .weight(animatedWeight)
            .combinedClickable(
                interactionSource = null,
                indication = ripple(bounded = false),
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
            )
            AnimatedVisibility(
                visible = toConfirm,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                Text(
                    text = title,
                    overflow = TextOverflow.Visible,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        content?.invoke()
    }
}

@Composable
internal fun Modifier.bottomMenuPadding(): Modifier = this
    .windowInsetsPadding(
        WindowInsets.navigationBars
            .only(WindowInsetsSides.Bottom),
    )
    .padding(horizontal = 8.dp, vertical = 12.dp)

// Long-pressing a button widens it for a second as a "tap again to confirm" affordance; one slot per action.
internal data class ConfirmSlots(val confirm: SnapshotStateList<Boolean>, val onLongClickItem: (Int) -> Unit)
