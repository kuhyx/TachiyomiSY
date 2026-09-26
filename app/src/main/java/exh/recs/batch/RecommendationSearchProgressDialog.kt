package exh.recs.batch

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

@Composable
internal fun RecSearchProgressDialog(
    status: SearchStatus,
    setStatusIdle: () -> Unit,
    setStatusCancelling: () -> Unit,
) {
    val context = LocalContext.current
    val currentView = LocalView.current

    DisposableEffect(status) {
        if (status != SearchStatus.Idle) {
            currentView.keepScreenOn = true
        }
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    val properties by produceState<RecommendationSearchProgressProperties?>(initialValue = null, status) {
        value = progressProperties(context, status, setStatusIdle, setStatusCancelling)
    }
    val dialog = properties
    if (dialog != null) {
        ProgressDialog(dialog, status)
    }
}

private fun progressProperties(
    context: Context,
    status: SearchStatus,
    setStatusIdle: () -> Unit,
    setStatusCancelling: () -> Unit,
): RecommendationSearchProgressProperties? = when (status) {
    is SearchStatus.Initializing -> RecommendationSearchProgressProperties(
        title = context.stringResource(SYMR.strings.rec_collecting),
        text = context.stringResource(SYMR.strings.rec_initializing),
        negativeButton = ProgressDialogButton(context.stringResource(MR.strings.action_cancel), setStatusCancelling),
    )
    is SearchStatus.Error -> RecommendationSearchProgressProperties(
        title = context.stringResource(SYMR.strings.rec_error_title),
        text = context.stringResource(SYMR.strings.rec_error_string, status.message),
        positiveButton = ProgressDialogButton(context.stringResource(MR.strings.action_ok), setStatusIdle),
    )
    is SearchStatus.Processing -> RecommendationSearchProgressProperties(
        title = context.stringResource(SYMR.strings.rec_collecting),
        text = context.stringResource(SYMR.strings.rec_processing_state, status.current, status.total) +
            "\n\n" + status.manga.title,
        negativeButton = ProgressDialogButton(context.stringResource(MR.strings.action_cancel), setStatusCancelling),
    )
    else -> null
}

@Composable
private fun ProgressDialog(dialog: RecommendationSearchProgressProperties, status: SearchStatus) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            dialog.positiveButton?.let { button ->
                TextButton(onClick = button.onClick) {
                    Text(text = button.text)
                }
            }
        },
        dismissButton = {
            dialog.negativeButton?.let { button ->
                TextButton(onClick = button.onClick) {
                    Text(text = button.text)
                }
            }
        },
        title = {
            Text(text = dialog.title)
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(text = dialog.text)
                if (status is SearchStatus.Processing) {
                    LinearProgressIndicator(
                        progress = { status.current.toFloat() / status.total },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    )
                }
            }
        },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
        ),
    )
}
